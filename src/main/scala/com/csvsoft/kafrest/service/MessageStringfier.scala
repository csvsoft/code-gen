package com.csvsoft.kafrest.service

import java.nio.charset.StandardCharsets

import com.csvsoft.kafrest.AppTask
import com.csvsoft.kafrest.utils.Log
import org.apache.kafka.clients.consumer.ConsumerRecord
import scalaz.zio.ZIO

trait MessageStringfier {
  def toMessageString(rec: ConsumerRecord[_, Array[Byte]]): AppTask[String]
}

class SimpleMessageStringfier extends MessageStringfier with Log{
  override def toMessageString(rec: ConsumerRecord[_, Array[Byte]]): AppTask[String] = ZIO.succeedLazy(new String(rec.value(), StandardCharsets.UTF_8))
}
