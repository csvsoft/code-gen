package com.csvsoft.kafrest.service

import org.apache.kafka.clients.consumer.ConsumerRecord

trait KafkaMessageBizKeyExtractor {
  def extractKey(record:ConsumerRecord[String, Array[Byte]]):String
}


//TODO: