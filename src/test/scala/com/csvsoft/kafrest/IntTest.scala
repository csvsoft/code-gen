package com.csvsoft.kafrest

import com.csvsoft.kafrest.utils.{ProgramBuilder, ProgramRunner}
import net.manub.embeddedkafka.EmbeddedKafka
import org.apache.kafka.common.serialization.{Serde, Serdes}
import org.scalatest.{FunSpec, Matchers}
import scalaz.zio.{UIO, ZIO}

class IntTest extends FunSpec with Matchers {

  val embeddedKafka = EmbeddedKafka.start()
  val bootstrapServer = s"localhost:${embeddedKafka.config.kafkaPort}"
  val x = System.setProperty("KAFKA_BOOTS_TRAP_SERVER", bootstrapServer)
  implicit val stringSerde: Serde[String] = Serdes.String()

  // val program = {

  //  ProgramRunner.run(Array("arg1"),program)

  //}

  def produceOne(t: String, k: String, m: String): UIO[Unit] = ZIO.effectTotal {
    import net.manub.embeddedkafka.Codecs._
    EmbeddedKafka.publishToKafka(t, k, m)
  }

  it("sss") {
    val program = ProgramBuilder.build()
    val test1 = for {
      p <- program.fork
      _ <- produceOne("topic1", "key", "testMessag1")
      _ <- p.interrupt
      _ <- p.join

    } yield List(())
    ProgramRunner.run(Array("arg1"), test1)
  }
}
