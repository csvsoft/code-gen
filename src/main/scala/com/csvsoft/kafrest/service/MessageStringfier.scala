package com.csvsoft.kafrest.service

import java.nio.charset.StandardCharsets

import com.csvsoft.kafrest.AppTask
import org.apache.kafka.clients.consumer.ConsumerRecord
import scalaz.zio.ZIO

trait MessageStringfier {
  def toMessageString(rec: ConsumerRecord[_, Array[Byte]]): AppTask[String]
}

class SimpleMessageStringfier extends MessageStringfier {
  override def toMessageString(rec: ConsumerRecord[_, Array[Byte]]): AppTask[String] = ZIO.succeedLazy(new String(rec.value(), StandardCharsets.UTF_8))
}
