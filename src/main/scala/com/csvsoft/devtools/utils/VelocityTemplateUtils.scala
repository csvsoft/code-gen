package com.csvsoft.devtools.utils

import org.apache.commons.lang.StringUtils
import org.apache.commons.lang.time.DateUtils
import org.apache.velocity.{Template, VelocityContext}
import org.apache.velocity.app.VelocityEngine
import org.apache.velocity.runtime.RuntimeConstants
import org.apache.velocity.runtime.resource.loader.{ClasspathResourceLoader}
import java.io.{BufferedWriter, File, FileWriter, IOException, StringWriter}
import java.util
import java.util.Properties


object VelocityTemplateUtils {

  private var veClasspath: VelocityEngine = null
  private var veString: VelocityEngine = null
  init()
  initClassPathVelocity()

  def init(): Unit = {
    veString = new VelocityEngine
    veString.setProperty("resource.loader", "string")
    veString.setProperty("string.resource.loader.class", classOf[StringResourceLoader].getName)
    veString.init()
  }

  def initClassPathVelocity(): Unit = {
    veClasspath = new VelocityEngine()
    veClasspath.setProperty(RuntimeConstants.RESOURCE_LOADER, "classpath")
    veClasspath.setProperty("classpath.resource.loader.class", classOf[ClasspathResourceLoader].getName)
    veClasspath.init()
  }

  def genFile(templateName: String, outFileName: String, map: Map[String, _]): Unit = {
    import scala.collection.JavaConverters._
    genFile(templateName, outFileName, map.asJava)
  }

  @throws[Exception]
  def genFile(templateName: String, outFileName: String, map: util.Map[String, _]): Unit = {
    genFile(templateName, outFileName, map, true)
  }

  def getClassPathTemplate(templateName:String):Template = {
    val template = veClasspath.getTemplate(templateName, "UTF-8")
    if (template == null) throw new RuntimeException("Unable to find file in class path:" + templateName)
    template
  }
  @throws[Exception]
  def genFile(templateName: String, outFileName: String, map: util.Map[String, _], `override`: Boolean): Unit = {
    val template = veClasspath.getTemplate(templateName, "UTF-8")
    if (template == null) throw new RuntimeException("Unable to find file in class path:" + templateName)
    val context = getVelocityContext(map)
    context.put("ctx", map)
    context.put("dateutil", new DateUtils)
    context.put("strutil", new StringUtils)

    val outFile: File = new File(outFileName)
    val parentDir = outFile.getParentFile
    if (!parentDir.exists) parentDir.mkdirs()
    if (outFile.exists && `override` == false) return
    val writer = new BufferedWriter(new FileWriter(outFile))
    ResourceUtils.use(writer){ wr=>template.merge(context, wr)}
  }

   def getVelocityContext(map: util.Map[String, _]) = {
    val context = new VelocityContext
    import scala.collection.JavaConverters._
    map.asScala.map(kv => {
      context.put(kv._1, kv._2)
    })
    context
  }

  def merge(templateStr: String, map: Map[String, _]): String = {
    import scala.collection.JavaConverters._
    merge(templateStr, map.asJava)
  }

  def merge(templateStr: String, map: util.Map[String, _]): String = {
    val template = veString.getTemplate(templateStr)
    val context = getVelocityContext(map)
    val sw = new StringWriter(60 * 1024)
    template.merge(context, sw)
    sw.flush()
    val outputText = sw.toString
    try sw.close()
    catch {
      case e: IOException =>

      // this would not happen
    }
    outputText
  }

  @throws[IOException]
  def genText(templateName: String, outFile: String, map: util.Map[String, Any]): Unit = {
    val templateFile = new File(templateName)
    val ve = new VelocityEngine
    val props = new Properties()
    props.setProperty(" resource.loader", "file")
    // Velocity.setProperty("classpath.resource.loader.description", "Velocity Classpath Resource Loader");
    // Velocity.setProperty("classpath.resource.loader.class", "org.apache.velocity.runtime.resource.loader.ClasspathResourceLoader");
    props.setProperty("file.resource.loader.class", "org.apache.velocity.runtime.resource.loader.FileResourceLoader")
    if (templateFile.getParent == null) props.setProperty("file.resource.loader.path", ".")
    else props.setProperty("file.resource.loader.path", templateFile.getParent)
    ve.init(props)
    var template: Template = null
    template = ve.getTemplate(templateFile.getName)
    val context = new VelocityContext

    import scala.collection.JavaConverters._
    map.asScala.map(kv => {
      context.put(kv._1, kv._2)
    })
    context.put("ctx", map)
    context.put("dateutil", new DateUtils)
    context.put("strutil", new StringUtils)

    val outf = new File(outFile)
    val dir = outf.getParentFile
    if (!dir.exists) dir.mkdirs
    val sw = new StringWriter(60 * 1024)
    template.merge(context, sw)
    sw.flush()
    val outputText = sw.toString
    sw.close()
    // if (outFile.toLowerCase().endsWith(".xml")) {
    // outputText = formatXML(outputText);
    // }
    val fw = new FileWriter(outf)
    val lines = StringUtils.split(outputText, "\n")
    var findFirstNonBlankLine = false
    for (line <- lines) {
      if (!findFirstNonBlankLine && StringUtils.isBlank(line)) {

      }
      else if (findFirstNonBlankLine) {
        fw.write(line)
        fw.write("\n")
      }
      else {
        findFirstNonBlankLine = true
        fw.write(line)
        fw.write("\n")
      }
    }
    // FileWriter fw = new FileWriter(outf);
    // template.merge(context, fw);
    fw.close()
  }
}
