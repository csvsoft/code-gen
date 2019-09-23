package com.csvsoft.kafrest.utils

import com.csvsoft.kafrest.AppTask

trait Log {
  protected def log[W](w: => W): AppTask[Unit]

  def logInfo[W](w: => W): AppTask[Unit] = log(w)

}
