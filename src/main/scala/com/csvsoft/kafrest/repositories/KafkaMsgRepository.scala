package com.csvsoft.kafrest.repositories

import com.csvsoft.kafrest.{AppEnvironment, AppTask}
import com.csvsoft.kafrest.config.ConfigService
import com.csvsoft.kafrest.entities.{KafkaMessage, KafkaMessageId}
import com.csvsoft.kafrest.repositories.impl.{DoobiekafKaMsgRepository, InMemKakfaMessageRepo}
import doobie.util.transactor.Transactor
import scalaz.zio.{Ref, Task, ZIO}


trait KafkaMsgRepository extends Serializable {

  def getAll(): AppTask[List[KafkaMessage]]

  def getLatest(topic: String, n: Int): AppTask[List[KafkaMessage]]

  def getById(id: KafkaMessageId): AppTask[Option[KafkaMessage]]

  def delete(id: KafkaMessageId): AppTask[Unit]

  def deleteAll: AppTask[Unit]

  def create(msg: KafkaMessage): AppTask[KafkaMessage]

  def update(id: KafkaMessageId, KafkaMessageForm: KafkaMessage): AppTask[Option[KafkaMessage]]

}

object KafkaMsgRepository extends Serializable {

  def loadRepo(xaOpt: Option[Transactor[Task]]): AppTask[KafkaMsgRepository] = {
    ConfigService.loadConfig().flatMap { appConfig =>
      (appConfig.DBConfig.repoImpl, xaOpt) match {
        case ("in-mem", _) => Ref.make(Map[KafkaMessageId, KafkaMessage]()).map(new InMemKakfaMessageRepo(_))
        case ("doobie", Some(xa)) => ZIO.fromFunction((env: AppEnvironment) => DoobiekafKaMsgRepository(xa))
        case ("doobie", None) => ZIO.fail(new RuntimeException("Transactor is required to load doobie repository"))
        case (x, _) => ZIO.fail(new RuntimeException(s"Not supported repo impl:$x"))
      }
    }
  }

  implicit def make(implicit xa: Option[Transactor[Task]]): AppTask[KafkaMsgRepository] = loadRepo(xa)
}
