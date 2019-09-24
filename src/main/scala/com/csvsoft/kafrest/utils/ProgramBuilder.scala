package com.csvsoft.kafrest.utils

import com.csvsoft.kafrest.config.{AppConfig, ConfigService}
import com.csvsoft.kafrest.service.{DBService, HtttRestService, KafkaService}
import com.csvsoft.kafrest.{AppTask}
import doobie.util.transactor.Transactor
import scalaz.zio.{Task, ZIO}
import scalaz.zio.interop.catz._

object ProgramBuilder extends Log {

  def useResource(xa: Transactor[Task], config: AppTask[AppConfig]): AppTask[List[Unit]] = {
    implicit val ixa = Option(xa)
    implicit val appConfig = config
    for {
      kafkaService <- KafkaService.make
      httpService <- HtttRestService.make

      _ <- log_INFO("Staring listening kafka...")
      kafkaListener = kafkaService.startListening2()

      _ <- log_INFO("Starting http server...")
      httpServer = httpService.startServer()

      program <- ZIO.foreachPar(List(httpServer, kafkaListener))(s => s)

    } yield program
  }

  def build(): AppTask[List[Unit]] = {
    implicit val appConfig = ConfigService.loadConfig
    for {
      dbService <- DBService.make
      _ <- dbService.initDb()
      program <- dbService.mkTransactor.use(xa => useResource(xa, appConfig))
    } yield program
  }
}
