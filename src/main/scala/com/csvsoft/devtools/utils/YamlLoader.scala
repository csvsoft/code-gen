package com.csvsoft.devtools.utils

import java.io.{InputStreamReader, Reader}
import java.nio.charset.StandardCharsets
import java.util

import scala.collection.JavaConverters._
import org.yaml.snakeyaml.Yaml

trait YamlLoader {
  def loadYaml(name: String): java.util.Map[String, Any]

}

abstract class YamlLoaderBase() extends YamlLoader {
  val csvLoader:CSVLoader
  val varPattern = """\$\{([a-zA-Z0-9]+)\}""".r

 protected def loadYaml(reader: Reader): java.util.Map[String, Any] = {
    val yaml = new Yaml()
    val map = yaml.load(reader).asInstanceOf[java.util.Map[String, Any]]
    resolveMap(map)
  }

  private def resolveMap(map: java.util.Map[String, Any]): java.util.Map[String, Any] = {
    val m = map.asScala
    m.map(kv => {
      val key = kv._1
      val value = kv._2
      val newValue = value match {
        case s: String => resolve(s, map)
        case a => a
      }
      if (key.endsWith("_csv")&& Option(newValue).isDefined) {
        (key,csvLoader.loadCSV(newValue.toString))
      }else{
        (key,newValue)
      }

    }).toMap.asJava
  }

  private def resolve(value: String, map: java.util.Map[String, Any]): String = {
    varPattern.replaceSomeIn(value, rm => Option(map.getOrDefault(rm group 1, rm group 1).toString))
  }

}

class ClassPathYamlLoader(classPathBase: String) extends YamlLoaderBase {
  val csvLoader = CSVLoader.getClassPathCSVLoader(classPathBase)
  override def loadYaml(name: String): util.Map[String, Any] = {
    val classPath = s"$classPathBase/$name"
    loadYamlFromClassPath(classPath)
  }
  private def loadYamlFromClassPath(classPath: String): java.util.Map[String, Any] = {
    val inputStream = this.getClass.getClassLoader.getResourceAsStream(classPath)
    if (Option(inputStream).isEmpty) {
      throw new RuntimeException(s"No resource found in classpath:$classPath")
    }
    val reader = new InputStreamReader(inputStream, StandardCharsets.UTF_8)
    ResourceUtils.use(reader)(loadYaml)
  }
}

object YamlLoader {
def getClassPathYamlLoader(classPathBase: String):YamlLoader = new ClassPathYamlLoader(classPathBase)



}
