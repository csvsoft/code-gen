package com.csvsoft.kafrest

import com.csvsoft.kafrest.config.ConfigService
import com.csvsoft.kafrest.utils.Log
import scalaz.zio.{Runtime, ZIO}
import scalaz.zio.blocking.Blocking
import scalaz.zio.clock.Clock
import scalaz.zio.console.Console
import scalaz.zio.internal.{Platform, PlatformLive}
import scalaz.zio.random.Random
import scalaz.zio.system.System

class AppRunTime(cmdArgs: Array[String]) extends Runtime[AppEnvironment] {

  type Environment = AppEnvironment

  val Platform: Platform = PlatformLive.Default
  val Environment: Environment = new Clock.Live with Console.Live with System.Live with Random.Live with Blocking.Live with Log with ConfigService.DEFAULT  {
    override protected def log[W](w: => W): AppTask[Unit] = ZIO.effectTotal(println(w))

    override val args: Array[String] = cmdArgs
  }
}
