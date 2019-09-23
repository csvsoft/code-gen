package com.csvsoft.kafrest.service

import com.csvsoft.kafrest.AppTask
import com.csvsoft.kafrest.config.AppConfig
import scalaz.zio.ZIO

trait MessageStringfierRegistry {
  def getMessageStringfier(topic: String): AppTask[MessageStringfier]
}

class DefaultMessageStringfierRegistry(appConfig: AppConfig) extends MessageStringfierRegistry {
  val map = Map[String, MessageStringfier]("topic1" -> new SimpleMessageStringfier())

  override def getMessageStringfier(topic: String): AppTask[MessageStringfier] = map.get(topic) match {
    case Some(s) => ZIO.effect(s)
    case _ => ZIO.effect(new SimpleMessageStringfier())
  }

}

object MessageStringfierRegistry {
  def apply(appConfig: AppConfig): MessageStringfierRegistry = new DefaultMessageStringfierRegistry(appConfig)

  implicit def make(implicit appConfig: AppTask[AppConfig]): AppTask[MessageStringfierRegistry] = {
    appConfig.map(apply)
  }
}
