package com.csvsoft.kafrest.entities

import java.time.{LocalDate, LocalDateTime}
import java.time.format.DateTimeFormatter

import io.circe.{Decoder, Encoder}
import io.circe.java8.time._

final case class KafkaMessageId(id: Long) extends AnyVal

 case class KafkaMessage(id: KafkaMessageId, topic: String, bizKey: Option[String], msgHeaders: Option[String], msgKey: Option[String], msg: String,createTime:LocalDateTime = LocalDateTime.now())
{
  def update(msg: KafkaMessage): KafkaMessage = {
    this.copy(id = this.id
      , topic = msg.topic
      , bizKey = msg.bizKey
      , msgHeaders = msg.msgHeaders
      , msg = msg.msg
      ,createTime =msg.createTime)
  }
}

object KafkaMessage{
   val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSSSSS")
  implicit val encodeDate: Encoder[LocalDate] = encodeLocalDateWithFormatter(formatter)
  implicit val decodeDate: Decoder[LocalDate] = decodeLocalDateWithFormatter(formatter)
  implicit val encodeDateTime: Encoder[LocalDateTime] = encodeLocalDateTimeWithFormatter(formatter)
  implicit val decodeDateTime: Decoder[LocalDateTime] = decodeLocalDateTimeWithFormatter(formatter)

}
