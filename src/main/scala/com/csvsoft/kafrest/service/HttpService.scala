package com.csvsoft.kafrest.service

import cats.effect.ExitCode
import com.csvsoft.kafrest.{AppEnvironment, AppTask}
import com.csvsoft.kafrest.config.AppConfig
import com.csvsoft.kafrest.entities.KafkaMessageId
import com.csvsoft.kafrest.repositories.KafkaMsgRepository
import com.csvsoft.kafrest.utils.Log
import org.http4s.{EntityDecoder, EntityEncoder, HttpRoutes}
import org.http4s.implicits._
import org.http4s.circe.{jsonEncoderOf, jsonOf}
import io.circe.{Decoder, Encoder}
import org.http4s.dsl.Http4sDsl
import scalaz.zio.interop.catz._
import org.http4s.dsl.impl.QueryParamDecoderMatcher
import org.http4s.server.blaze.BlazeServerBuilder
import scalaz.zio.ZIO
import io.circe.generic.auto._
import scalaz.zio.interop.catz.taskConcurrentInstances

trait HtttRestService {
  def startServer(): AppTask[Unit]
}

case class MessagePost(msg: String)

object MessagePost {
  //implicit val decoder: Decoder[MessagePost] = deriveDecoder
}

case class KafkaMessageRestService[R](config: AppConfig, repo: KafkaMsgRepository, kafkaService: KafkaService) extends HtttRestService with Log{

  implicit def circeJsonDecoder[A](implicit decoder: Decoder[A]): EntityDecoder[AppTask, A] = jsonOf[AppTask, A]

  implicit def circeJsonEncoder[A](implicit encoder: Encoder[A]): EntityEncoder[AppTask, A] = jsonEncoderOf[AppTask, A]

  object MessageKeyQueryParamMatcher extends QueryParamDecoderMatcher[String]("msgkey")

  val dsl: Http4sDsl[AppTask] = Http4sDsl[AppTask]

  import dsl._

  def startServer() = {

    val httpApp = HttpRoutes.of[AppTask] {
      case GET -> Root => Ok("hello!")
      case req@PUT -> Root / "kafka_message" / topic => req.decode[MessagePost](msg => Ok(kafkaService.produceMsg(topic, msg.msg).map(r => r.offset())))
      case GET -> Root / "kafka_message" / topic / "latest" / IntVar(n) => Ok(repo.getLatest(topic, n).map(l => l.toSeq))
      case GET -> Root / "kafka_message" / topic / "bykey" :? MessageKeyQueryParamMatcher(key) => repo.getById(KafkaMessageId(1)).foldM(_ => NotFound(), Ok(_))
      //Ok(SimpleMessage("name",11))
      //
    }.orNotFound

    ZIO.runtime[AppEnvironment].flatMap { implicit rts =>
      BlazeServerBuilder[AppTask]
        .bindHttp(config.httpConfig.port, "0.0.0.0")
        .withHttpApp(httpApp)

        .withNio2(true)

        .serve
        .compile[AppTask, AppTask, ExitCode]
        .drain
    }
  }

}

object HtttRestService {
  implicit def make(implicit appConfig: AppTask[AppConfig],
                    kafkaMsgRepository: AppTask[KafkaMsgRepository],
                    kafkaService: AppTask[KafkaService]
                   ): AppTask[HtttRestService] = for {
    config <- appConfig
    repo <- kafkaMsgRepository
    kservice <- kafkaService
  } yield KafkaMessageRestService(config, repo, kservice)

}
