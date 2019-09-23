package com.csvsoft.kafrest

import org.scalatest.{FunSpec, Matchers}
import com.softwaremill.sttp._
class SimpleHttpTest extends FunSpec with Matchers{

  it("test put and get"){

    implicit val sttpBackend = HttpURLConnectionBackend()
    //implicit val sttpBackend = AsyncHttpClientZioBackend()
    val response = sttp.put(uri"http://localhost:8090/kafka_message/topic1")
      .header("Content-type","Application/json")
      .body("""{"msg":"test message1"}""")
      .send()
    println(response.code)
    response.body match{
      case Left(err) => println(s"Error:$err")
      case Right(msg) => println(s"OK:$msg")
    }

    val responseLatest = sttp.get(uri"http://localhost:8090/kafka_message/topic1/latest/10")
      .header("Content-type","Application/json")
      .send()
    printResponse(responseLatest)
  }

  def printResponse(response:Response[String]): Unit ={
    println(response.code)
    response.body match{
      case Left(err) => println(s"Error:$err")
      case Right(msg) => println(s"OK:$msg")
    }
  }
}
