package com.csvsoft.kafrest.service

import java.nio.charset.StandardCharsets

import com.csvsoft.kafrest
import com.csvsoft.kafrest.AppTask
import com.csvsoft.kafrest.config.AppConfig
import com.csvsoft.kafrest.entities.{KafkaMessage, KafkaMessageId}
import com.csvsoft.kafrest.repositories.KafkaMsgRepository
import org.apache.kafka.clients.consumer.{ConsumerRecord, OffsetAndMetadata}
import org.apache.kafka.clients.producer.{ProducerRecord, RecordMetadata}
import org.apache.kafka.common.TopicPartition
import org.apache.kafka.common.serialization.{Serde, Serdes}
import scalaz.zio.blocking.Blocking
import scalaz.zio.{Chunk, Queue, Semaphore, ZIO}
import zio.kafka.client.{Consumer, ConsumerSettings, Producer, ProducerSettings, Subscription}
import scalaz.zio.duration._

abstract class KafkaService(appConfig: AppConfig) {

  implicit val stringSerde: Serde[String] = Serdes.String()
  implicit val byteArraySerde: Serde[Array[Byte]] = Serdes.ByteArray()

  def consumerSettings(): ConsumerSettings = {
    val kafkaConfig = appConfig.kafkaConsumerConfig;
    ConsumerSettings(
      List(appConfig.kafkaConsumerConfig.bootstrapServer),
      kafkaConfig.groupId,
      kafkaConfig.clientId,
      5.seconds,
      kafkaConfig.extraSettings
    )
  }

  def producerSettings():ProducerSettings ={
    val kafkaConfig = appConfig.kafkaConsumerConfig;
    ProducerSettings(
      List(appConfig.kafkaConsumerConfig.bootstrapServer),
      5.seconds,
      kafkaConfig.extraSettings
    )
  }

  def makeProducer()={
    Producer.make[String,String](producerSettings)

  }

  def produceMsg(topic:String,msg:String):AppTask[RecordMetadata]={
    makeProducer().use{ p=>
      p.produce(new ProducerRecord[String,String](topic,"",msg))

    }
  }

  def makeConsumer() = Consumer.make[String, Array[Byte]](consumerSettings)

  def log(msg: String) = kafrest.logInfo(msg)


  def listenKafka(subscription: Subscription, consumer: Consumer[String, Array[Byte]], msgQueue: Queue[ConsumerRecord[String, Array[Byte]]], semaphore: Semaphore): AppTask[Unit] = {
    val prog = for {
      _ <- semaphore.acquire
      _ <- consumer.subscribe(subscription)
      records <- pollNtimes(1, consumer)
      _ <- semaphore.release
      _ <- log(s"Enqueuing messages:${records.length}")
      _ <- msgQueue.offerAll(records.toSeq.toIterable)
      _ <- listenKafka(subscription, consumer, msgQueue, semaphore)
    } yield ()
    prog
  }

 /* def consume(consumer: Consumer[String, Array[Byte]], msgQueue: Queue[ConsumerRecord[String, Array[Byte]]], semaphore: Semaphore): AppTask[Unit] = {
    val prog = for {
      records <- msgQueue.takeUpTo(20)
      _ <- log(s"Consume messages:${records.length}")
      result <- ZIO.foreachPar(records.toSeq.toIterable)(processRecord)
      _ <- semaphore.acquire
      _ <- consumer.commit(reduce(result))
      _ <- semaphore.release
      _ <- consume(consumer, msgQueue, semaphore)
    } yield ()
    prog
  }*/

  def startListening(): AppTask[Unit] = {
    val subscription = Subscription.Topics(appConfig.kafkaConsumerConfig.topics.toSet)

    makeConsumer().use( consumer => for {
      _ <- consumer.subscribe(subscription)
      _ <- listen(consumer)
    } yield ()
   )
  }

  def listen(consumer: Consumer[String, Array[Byte]]): AppTask[Unit] = {
    val prog = for {
      records <- pollNtimes(1, consumer)
      result <- processRecords(records)
      _ <- consumer.commit(reduce(result))
      _ <- listen(consumer)
    } yield ()
    prog.catchAll(t=>{
      t.printStackTrace()
      listen(consumer)
    })
  }

  /*def startListening2(): AppTask[Unit] = {
    val subscription = Subscription.Topics(appConfig.kafkaConsumerConfig.topics.toSet)

    def listen(consumer: Consumer[String, Array[Byte]]) = for {
      semaphore <- Semaphore.make(1L)
      msgQueue <- Queue.bounded[ConsumerRecord[String, Array[Byte]]](1000)
      consumFiber <- consume(consumer, msgQueue, semaphore).fork
      lFiber <- listenKafka(subscription, consumer, msgQueue, semaphore).fork
      _ <- consumFiber.join
      _ <- lFiber.join
    } yield ()

    makeConsumer().use(listen)
  }*/

  def reduce(results: List[(TopicPartition, OffsetAndMetadata)]) = {
    results.groupBy(_._1)
      .map(pair => (pair._1, pair._2.map(_._2).sortWith((a, b) => a.offset() > b.offset())))
      .map(pairSorted => (pairSorted._1, pairSorted._2.head))
  }

  def buildCommitEntry(rec: ConsumerRecord[String, Array[Byte]]): (TopicPartition, OffsetAndMetadata) = {
    val topicPartition = new TopicPartition(rec.topic(), rec.partition())
    val offset = new OffsetAndMetadata(rec.offset())
    (topicPartition, offset)
  }


  def processRecords(recs: Chunk[ConsumerRecord[String, Array[Byte]]]): AppTask[List[(TopicPartition, OffsetAndMetadata)]]

  def recordsFromAllTopics[K, V](pollResult: Map[TopicPartition, Chunk[ConsumerRecord[K, V]]]): Chunk[ConsumerRecord[K, V]] = {
    Chunk.fromIterable(pollResult.values).flatMap(identity)
  }


  def getAllRecordsFromMultiplePolls[K, V](res: List[Map[TopicPartition, Chunk[ConsumerRecord[K, V]]]]): Chunk[ConsumerRecord[K, V]] =
    res.foldLeft[Chunk[ConsumerRecord[K, V]]](Chunk.empty)(
      (acc, pollResult) => acc ++ recordsFromAllTopics[K, V](pollResult)
    )

  def pollNtimes[K, V](n: Int, consumer: Consumer[K, V]): ZIO[Blocking, Throwable, Chunk[ConsumerRecord[K, V]]] =
    ZIO.foreach(List.fill(n)(()))(_ => consumer.poll(Long.MaxValue.hours)).map(getAllRecordsFromMultiplePolls)

  def tp(topic: String, partition: Int): TopicPartition = new TopicPartition(topic, partition)

}


class PersistKafkaMessageService(appConfig: AppConfig, messageStringfierRegistry: MessageStringfierRegistry, repo: KafkaMsgRepository) extends KafkaService(appConfig) {
  import io.circe.syntax._
  def convertConsumerRecord(rec: ConsumerRecord[String, Array[Byte]]): AppTask[KafkaMessage] = {
    for {
      s <- messageStringfierRegistry.getMessageStringfier(rec.topic)
      msg <- s.toMessageString(rec)
    } yield KafkaMessage(KafkaMessageId(-1)
      , rec.topic
      , None
      , Option(rec.headers().toArray.map(h=>(h.key(),new String(h.value(),StandardCharsets.UTF_8))).asJson.toString())
      , Option(rec.key())
      , msg
    )
  }


  override  def processRecords(recs: Chunk[ConsumerRecord[String, Array[Byte]]]): AppTask[List[(TopicPartition, OffsetAndMetadata)]]={
    ZIO.foreach(recs.toSeq)(processRecord)
  }
   def processRecord(rec: ConsumerRecord[String, Array[Byte]]): AppTask[(TopicPartition, OffsetAndMetadata)] = for {

    msg <- convertConsumerRecord(rec)
    _ <- log("Saving kafka message to data store")
    _ <- repo.create(msg)

  } yield buildCommitEntry(rec)
}


object KafkaService {
  def makePersistService(appConfig: AppConfig, messageStringfierRegistry: MessageStringfierRegistry, repo: KafkaMsgRepository): KafkaService = {
    new PersistKafkaMessageService(appConfig, messageStringfierRegistry, repo)
  }

  implicit def make(implicit appConfig: AppTask[AppConfig], messageStringfierRegistry: AppTask[MessageStringfierRegistry], repo: AppTask[KafkaMsgRepository]): AppTask[KafkaService] = {
    for {
      config <- appConfig
      r <- messageStringfierRegistry
      rep <- repo
    } yield new PersistKafkaMessageService(config, r, rep)
  }

}