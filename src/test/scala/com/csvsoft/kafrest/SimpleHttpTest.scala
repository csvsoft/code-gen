package com.csvsoft.kafrest

import org.scalatest.{FunSpec, Matchers}
import com.softwaremill.sttp._

class SimpleHttpTest extends FunSpec with Matchers {
  implicit val sttpBackend = HttpURLConnectionBackend()

  def sendMessage(index: Int) = {

    //implicit val sttpBackend = AsyncHttpClientZioBackend()
    val response = sttp.put(uri"http://localhost:8090/kafka_message/topic1")
      .header("Content-type", "Application/json")
      .body(s"""{"msg":"test message$index"}""")
      .send()
    println(response.code)
    response.body match {
      case Left(err) => println(s"Error:$err")
      case Right(msg) => println(s"OK:$msg")
    }
  }

  it("test put and get") {

    (1 to 20).foreach(i => sendMessage(i))

    val responseLatest = sttp.get(uri"http://localhost:8090/kafka_message/topic1/latest/10")
      .header("Content-type", "Application/json")
      .send()
    printResponse(responseLatest)
  }

  def printResponse(response: Response[String]): Unit = {
    println(response.code)
    response.body match {
      case Left(err) => println(s"Error:$err")
      case Right(msg) => println(s"OK:$msg")
    }
  }
}
