package com.csvsoft.kafrest.service

import cats.effect.{Async, ContextShift}
import com.csvsoft.kafrest.{AppEnvironment, AppTask}
import com.csvsoft.kafrest.config.{AppConfig, ConfigService}
import com.csvsoft.kafrest.utils.Log
import doobie.hikari.HikariTransactor
import doobie.util.transactor.Transactor
import org.flywaydb.core.Flyway
import scalaz.zio.{Managed, Reservation, Task, ZIO, ZManaged}

trait DBService {
  def initDb(): Task[Unit]

  def mkTransactor(implicit at: Async[Task], cs: ContextShift[Task]): ZManaged[AppEnvironment, Throwable, Transactor[Task]]
}

class DefaultDBService(appConfig: AppConfig, threadPoolService: ThreadPoolService) extends DBService with Log{

  def initDb(): Task[Unit] = ZIO.effect {
    val cfg = appConfig.DBConfig
    val fw = Flyway.configure().dataSource(cfg.url, cfg.user, cfg.password).load()
    fw.migrate()
  }.unit

  /*def purgeOldRecords() = {
    val spaced = Schedule.spaced(10.milliseconds)
    val repeatedDelet = repo.deleteAll.repeat(spaced)
    repeatedDelet
  }*/


  def mkTransactor(implicit at: Async[Task], cs: ContextShift[Task]): ZManaged[AppEnvironment, Throwable, Transactor[Task]] = {

    val cfg = appConfig.DBConfig
    val x = for {
      connectEC <- threadPoolService.initNonBlockingThreadExecutionContext
      transactEC <- threadPoolService.initNonBlockingThreadExecutionContext
      xa = HikariTransactor.newHikariTransactor[Task](
        cfg.driver,
        cfg.url,
        cfg.user,
        cfg.password,
        connectEC,
        transactEC)

      res <- xa.allocated
        .map { case (transactor, cleanupM) =>
          Reservation(ZIO.succeed(transactor), cleanupM.orDie)
        }.uninterruptible
    } yield res
    Managed(x)
  }

}

object DBService {
  def apply(appConfig: AppConfig, threadPoolService: ThreadPoolService): DBService = new DefaultDBService(appConfig, threadPoolService)

  implicit def make(implicit threadPoolService: AppTask[ThreadPoolService]): AppTask[DBService] = for {
    appConfig <- ConfigService.loadConfig()
    pool <- threadPoolService
  } yield DBService(appConfig, pool)
}
