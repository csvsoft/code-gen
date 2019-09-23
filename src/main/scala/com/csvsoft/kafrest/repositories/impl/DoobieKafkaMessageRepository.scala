package com.csvsoft.kafrest.repositories.impl

import java.sql.Timestamp
import java.time.LocalDateTime

import com.csvsoft.kafrest.AppTask
import com.csvsoft.kafrest.entities.{KafkaMessage, KafkaMessageId}
import com.csvsoft.kafrest.repositories.KafkaMsgRepository
import com.csvsoft.kafrest.repositories.impl.DoobiekafkaMsgRepository.SQL
import doobie.free.connection
import doobie._
import doobie.implicits._
import cats.implicits._
import scalaz.zio.Task
import scalaz.zio.interop.catz._

case class DoobiekafkaMsgRepository(xa: Transactor[Task]) extends KafkaMsgRepository {

 // protected def xa: Transactor[AppTask]

      override def getAll(): AppTask[List[KafkaMessage]] =
        SQL
          .getAll
          .to[List]
          .transact(xa)
          .orDie
  def getLatest(topic:String,n:Int): AppTask[List[KafkaMessage]] = {
    SQL
      .getLatest(n)
      .to[List]
      .transact(xa)
      .orDie
  }
      override def getById(id: KafkaMessageId): AppTask[Option[KafkaMessage]] =
        SQL
          .get(id)
          .option
          .transact(xa)
          .orDie

      override def delete(id: KafkaMessageId): AppTask[Unit] =
        SQL
          .delete(id)
          .run
          .transact(xa)
          .unit
          .orDie

      override def deleteAll: AppTask[Unit] =
        SQL
          .deleteAll
          .run
          .transact(xa)
          .unit
          .orDie

      override def create(msg: KafkaMessage): AppTask[KafkaMessage] =
        SQL
          .create(msg)
          .withUniqueGeneratedKeys[Long]("ID")
          .map(id =>msg.copy(id=KafkaMessageId(id)))
          .transact(xa)
          .orDie

      override def update(id: KafkaMessageId, msg: KafkaMessage):AppTask[Option[KafkaMessage]] =
        (for {
          oldItem    <- SQL.get(id).option
          newItem     = oldItem.map(_.update(msg))
          _          <- newItem.fold(connection.unit)(item => SQL.update(item).run.void)
        } yield newItem)
        .transact(xa)
        .orDie


}

object DoobiekafkaMsgRepository{

  implicit val getLocalDateTime:Get[LocalDateTime] = Get[Timestamp].tmap(ts=>ts.toLocalDateTime)
  implicit val putLocalDateTime:Put[LocalDateTime] = Put[Timestamp].tcontramap( ldt =>  Timestamp.valueOf(ldt))
  object SQL {

    def create(msg: KafkaMessage): Update0 = sql""" INSERT INTO KAFKA_MESSAGE(
                                                 |  TOPIC,
                                                 |  BIZ_KEY,
                                                 |  MSG_HEADERS,
                                                 |  MSG_KEY,
                                                 |  MSG,
                                                 |   CREATE_TIME)
                                                 |  VALUES(${msg.topic},${msg.bizKey},${msg.msgHeaders},${msg.msgKey},${msg.msg},${msg.createTime})
      """.stripMargin.update

    def get(id: KafkaMessageId): Query0[KafkaMessage] = sql"""
      SELECT * FROM KAFKA_MESSAGE WHERE ID = ${id.id}
      """.query[KafkaMessage]

    val getAll: Query0[KafkaMessage] = sql"""
      SELECT * FROM KAFKA_MESSAGE
      """.query[KafkaMessage]

    def getLatest(n:Int): Query0[KafkaMessage] = sql"""
      SELECT * FROM KAFKA_MESSAGE ORDER BY CREATE_TIME DESC LIMIT 1,$n
      """.query[KafkaMessage]

    def delete(id: KafkaMessageId): Update0 =sql"""
      DELETE from KAFKA_MESSAGE WHERE ID = ${id.id}
      """.update

    val deleteAll: Update0 =sql"""
      DELETE from KAFKA_MESSAGE
      """.update

    def update(msg: KafkaMessage): Update0 = sql"""
        UPDATE KAFKA_MESSAGE SET
        TOPIC = ${msg.topic},
        BIZ_KEY = ${msg.bizKey},
        MSG_HEADERS = ${msg.msgHeaders},
        MSG_KEY = ${msg.msgKey},
        MSG = ${msg.msg}
        WHERE ID = ${msg.id.id}
      """.update
  }

}


