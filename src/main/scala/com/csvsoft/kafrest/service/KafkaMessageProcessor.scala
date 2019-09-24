package com.csvsoft.kafrest.service

import java.nio.charset.StandardCharsets

import com.csvsoft.kafrest
import com.csvsoft.kafrest.AppTask
import com.csvsoft.kafrest.config.AppConfig
import com.csvsoft.kafrest.entities.{KafkaMessage, KafkaMessageId}
import com.csvsoft.kafrest.repositories.KafkaMsgRepository
import com.csvsoft.kafrest.utils.Log
import org.apache.kafka.clients.consumer.{ConsumerRecord, OffsetAndMetadata}
import org.apache.kafka.common.TopicPartition
import scalaz.zio.{Chunk, ZIO}

trait KafkaMessageProcessor {
  def processRecords(recs: Chunk[ConsumerRecord[String, Array[Byte]]]): AppTask[List[(TopicPartition, OffsetAndMetadata)]] = processRecords(recs.toSeq)

  def processRecords(recs: Seq[ConsumerRecord[String, Array[Byte]]]): AppTask[List[(TopicPartition, OffsetAndMetadata)]]
}


class PersistKafkaMessageProcessor(appConfig: AppConfig, messageStringfierRegistry: MessageStringfierRegistry, repo: KafkaMsgRepository) extends KafkaMessageProcessor with Log{

  import io.circe.syntax._

  def convertConsumerRecord(rec: ConsumerRecord[String, Array[Byte]]): AppTask[KafkaMessage] = {
    for {
      s <- messageStringfierRegistry.getMessageStringfier(rec.topic)
      msg <- s.toMessageString(rec)
    } yield KafkaMessage(KafkaMessageId(-1)
      , rec.topic
      , None
      , Option(rec.headers().toArray.map(h => (h.key(), new String(h.value(), StandardCharsets.UTF_8))).asJson.toString())
      , Option(rec.key())
      , msg
    )
  }

  override def processRecords(recs: Seq[ConsumerRecord[String, Array[Byte]]]): AppTask[List[(TopicPartition, OffsetAndMetadata)]] = {
    ZIO.foreach(recs)(processRecord)
  }

  def processRecord(rec: ConsumerRecord[String, Array[Byte]]): AppTask[(TopicPartition, OffsetAndMetadata)] = for {

    msg <- convertConsumerRecord(rec)
    _ <- kafrest.logInfo("Saving kafka message to data store")
    _ <- repo.create(msg)

  } yield buildCommitEntry(rec)

  def buildCommitEntry(rec: ConsumerRecord[String, Array[Byte]]): (TopicPartition, OffsetAndMetadata) = {
    val topicPartition = new TopicPartition(rec.topic(), rec.partition())
    val offset = new OffsetAndMetadata(rec.offset())
    (topicPartition, offset)
  }
}

object KafkaMessageProcessor {
  implicit def make(implicit appConfig: AppTask[AppConfig], messageStringfierRegistry: AppTask[MessageStringfierRegistry], kafkaMsgRepository: AppTask[KafkaMsgRepository]): AppTask[KafkaMessageProcessor] = {
    for {
      config <- appConfig
      r <- messageStringfierRegistry
      repo <- kafkaMsgRepository
    } yield new PersistKafkaMessageProcessor(config, r, repo)
  }
}

