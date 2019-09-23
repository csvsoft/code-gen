package com.csvsoft

import com.csvsoft.kafrest.config.ConfigService
import com.csvsoft.kafrest.utils.Log
import scalaz.zio.{TaskR, ZIO}
import scalaz.zio.blocking.Blocking
import scalaz.zio.clock.Clock
import scalaz.zio.console.Console
import scalaz.zio.random.Random
import scalaz.zio.system.System

package object kafrest {

  type BizEnvironment = ConfigService
  type SysEnvironment = Clock with Console with System with Random with Blocking with Log
  type AppEnvironment = BizEnvironment with SysEnvironment
  type AppTask[A] = TaskR[AppEnvironment, A]

  def logInfo[W](w: => W) = ZIO.accessM[AppEnvironment](l => l.logInfo(w))
}
