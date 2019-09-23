package com.csvsoft.kafrest

import com.csvsoft.kafrest.utils.ProgramRunner

object Main {

  def main(args: Array[String]): Unit = {
    val program = ProgramBuilder.build()
    ProgramRunner.run(args, program)
  }
}