package com.csvsoft.kafrest.repositories.impl

import com.csvsoft.kafrest.AppTask
import com.csvsoft.kafrest.entities.{KafkaMessage, KafkaMessageId}
import com.csvsoft.kafrest.repositories.KafkaMsgRepository
import com.csvsoft.kafrest.utils.Log
import scalaz.zio.{Ref, UIO, ZIO}


final case class InMemKakfaMessageRepo(ref: Ref[Map[KafkaMessageId, KafkaMessage]]) extends KafkaMsgRepository with Log{

  override def getAll(): ZIO[Any, Nothing, List[KafkaMessage]] =
    ref.get.map(_.values.toList)

  def getLatest(topic: String, n: Int): AppTask[List[KafkaMessage]] = {
    getAll().map(list => list
      .filter(_.topic == topic)
      .sortWith((a, b) => a.createTime.isAfter(b.createTime))
      .take(n)
    )
  }

  override def getById(id: KafkaMessageId): ZIO[Any, Nothing, Option[KafkaMessage]] =
    ref.get.map(_.get(id))

  override def delete(id: KafkaMessageId): ZIO[Any, Nothing, Unit] =
    ref.update(store => store - id).unit

  override def deleteAll: ZIO[Any, Nothing, Unit] =
    ref.update(_.empty).unit

  override def create(msg: KafkaMessage): ZIO[Any, Nothing, KafkaMessage] =
    ref.update(s => s + (msg.id -> msg)) *> ZIO.succeed(msg)

  override def update(id: KafkaMessageId, msg: KafkaMessage): ZIO[Any, Nothing, Option[KafkaMessage]] =
    for {
      oldValue <- getById(id)
      result <- oldValue.fold[UIO[Option[KafkaMessage]]](ZIO.succeed(None)) { x =>
        val newValue = x.update(msg)
        ref.update(store => store + (newValue.id -> newValue)) *> ZIO.succeed(Some(newValue))
      }
    } yield result

}
