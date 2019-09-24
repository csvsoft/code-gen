package com.csvsoft.kafrest.utils

import com.csvsoft.kafrest.AppTask
import com.typesafe.scalalogging.LazyLogging
import scalaz.zio.ZIO

trait Log extends LazyLogging {

  def log_INFO[W](w: => String): AppTask[Unit] = ZIO.effect(this.logger.info(w))
  def log_DEBUG[W](w: => String): AppTask[Unit] = ZIO.effect(this.logger.debug(w))
  def log_WARN[W](w: => String): AppTask[Unit] = ZIO.effect(this.logger.warn(w))
  def log_ERROR[W](w: => String): AppTask[Unit] = ZIO.effect(this.logger.error(w))
  def log_ERROR[W](w: => String,t: => Throwable): AppTask[Unit] = ZIO.effect(this.logger.error(w,t))
}
