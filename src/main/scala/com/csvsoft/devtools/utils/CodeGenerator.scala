package com.csvsoft.devtools.utils

import java.io.{File, FileOutputStream, OutputStreamWriter, Writer}

import com.csvsoft.devtools.model.{CodeGenItem, Project}

class CodeGenerator(resourceLoader: ResourceLoader) {


  def genCode(project: Project): Unit = {
    project.codeGenItems.foreach(genItemCodeWithVelocity(_, project.outputDir))
  }

  private def genItemCodeWithVelocity(item: CodeGenItem, baseDir: String): Unit = {
    val model = item.model
    val map = resourceLoader.loadModel(model)
    val template = resourceLoader.getVelocityTemplate(item.template)
    val outPath = s"$baseDir/${item.out}"
    val outWriter = getOutWriter(outPath)

    println(s"Generating file: ${outPath}")
    ResourceUtils.use(outWriter) { writer =>
      template.merge(VelocityTemplateUtils.getVelocityContext(map), writer)
    }

  }
  /*
  private def genItemCodeWithFreeMarker(item: CodeGenItem, baseDir: String): Unit = {
    val model = item.model
    val map = resourceLoader.loadModel(model)
    val template = resourceLoader.getFreeMarkerTemplate(item.template)

    val outPath = s"$baseDir/${item.out}"
    val outWriter = getOutWriter(outPath)

    println(s"Generating file: ${outPath}")
    ResourceUtils.use(outWriter) { writer =>
      template.process(map, writer)
    }
  }
 */
  private def getOutWriter(outPath:String):Writer ={
    val outFile = new File(outPath)
    val outDir = outFile.getParentFile
    if (!outDir.exists()) {
      val created = outDir.mkdirs()
      if (!created) {
        throw new RuntimeException(s"Unable to create directory:${outDir.getAbsolutePath}")
      }
    }
    val outStream = new FileOutputStream(outFile)
    val outWriter = new OutputStreamWriter(outStream)
    outWriter
  }
}
