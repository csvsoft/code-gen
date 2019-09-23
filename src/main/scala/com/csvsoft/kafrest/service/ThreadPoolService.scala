package com.csvsoft.kafrest.service

import java.util.concurrent.Executors

import com.csvsoft.kafrest.AppTask
import com.csvsoft.kafrest.config.{AppConfig, ConfigService}
import scalaz.zio.ZIO

import scala.concurrent.ExecutionContext

trait ThreadPoolService {

  def initDBBlockingThreadExecutionContext: AppTask[ExecutionContext]

  def initNonBlockingThreadExecutionContext: AppTask[ExecutionContext]

}

class DefaultThreadPoolService(appConfig: AppConfig) extends ThreadPoolService {

  def initDBBlockingThreadExecutionContext: AppTask[ExecutionContext] = ZIO.succeedLazy {
    ExecutionContext.fromExecutor(Executors.newFixedThreadPool(appConfig.threadPoolConfig.dbBlockingFixPoolSize))
  }

  def initNonBlockingThreadExecutionContext: AppTask[ExecutionContext] = ZIO.succeedLazy {
    ExecutionContext.fromExecutor(
      new java.util.concurrent.ForkJoinPool(appConfig.threadPoolConfig.nonBlockingFixPoolSize)
    )
  }

}

object ThreadPoolService {
  def apply(config: AppConfig): ThreadPoolService = new DefaultThreadPoolService(config)

  implicit val make: AppTask[ThreadPoolService] = ConfigService.loadConfig().map { appConfig => new DefaultThreadPoolService(appConfig) }
}
