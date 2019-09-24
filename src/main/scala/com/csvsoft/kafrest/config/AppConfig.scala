package com.csvsoft.kafrest.config

import com.csvsoft.kafrest.{AppEnvironment, AppTask}
import pureconfig.generic.auto._
import scalaz.zio.{ZIO, system}
import pureconfig._

final case class DBConfig(url: String, driver: String, user: String, password: String, repoImpl: String)

final case class HttpConfig(port: Int, baseUrl: String)

final case class ThreadPoolConfig(dbBlockingFixPoolSize: Int, nonBlockingFixPoolSize: Int)

final case class KafkaConsumerConfig(bootstrapServer: String, groupId: String, clientId: String, topics: List[String], extraSettings: Map[String, String])

final case class KafkaProducerConfig(bootstrapServer: String, extraSettings: Map[String, String])

final case class AppConfig(DBConfig: DBConfig, httpConfig: HttpConfig, threadPoolConfig: ThreadPoolConfig, kafkaConsumerConfig: KafkaConsumerConfig, kafkaProducerConfig: KafkaProducerConfig)

trait ConfigService {
  val args: Array[String]

  def loadConfig(): AppTask[AppConfig]
}

object ConfigService {

  trait DEFAULT extends ConfigService {
    def loadConfig(): AppTask[AppConfig] = {
      system.property("ACTIVE_PROFILE")
        .map(profileOpt => profileOpt.fold("")(p => s"-${p}"))
        .map(profile => s"application${profile}.conf")
        .map(configFile => ConfigSource.resources(configFile).withFallback(ConfigSource.default))
        .flatMap(configSource =>
          ZIO.fromFunction((env: AppEnvironment) => configSource.load[AppConfig])
            .flatMap(r => ZIO.fromEither(r))
            .mapError(f => new RuntimeException(f.toList.mkString(",")))
        ).memoize
        .flatMap(mix => mix.flatMap(config => ZIO.fromFunction((env: AppEnvironment) => config)))

    }
  }

  def loadConfig(): AppTask[AppConfig] = ZIO.accessM[AppEnvironment](_.loadConfig())

  implicit def make(): AppTask[AppConfig] = loadConfig()
}
