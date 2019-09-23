package com.csvsoft.kafrest.utils

import com.csvsoft.kafrest.{AppRunTime, AppTask}
import scalaz.zio
import scalaz.zio.{IO, ZIO}
import scalaz.zio.console.{Console, putStrLn}

object ProgramRunner {

  def run1[R <: Console,E,A](runtime:zio.Runtime[R], program:ZIO[R,E,A]):Nothing ={

    val result = program.foldM(err => putStrLn(s"Execution failed with: $err") *> ZIO.succeed(1), _ => ZIO.succeed(0))

    sys.exit(
      runtime.unsafeRun(
        for {
          fiber <- result.fork
          _ <- IO.effectTotal(java.lang.Runtime.getRuntime.addShutdownHook(new Thread {
            override def run() = {
              val _ = runtime.unsafeRunSync(fiber.interrupt)
            }
          }))
          result <- fiber.join
        } yield result
      )
    )

  }

  def run(args: Array[String], program: AppTask[List[Unit]]) = {
    val runtime = new AppRunTime(args)
    val result = program.foldM(err => putStrLn(s"Execution failed with: $err") *> ZIO.succeed(1), _ => ZIO.succeed(0))

    sys.exit(
      runtime.unsafeRun(
        for {
          fiber <- result.fork
          _ <- IO.effectTotal(java.lang.Runtime.getRuntime.addShutdownHook(new Thread {
            override def run() = {
              val _ = runtime.unsafeRunSync(fiber.interrupt)
            }
          }))
          result <- fiber.join
        } yield result
      )
    )
  }

}
