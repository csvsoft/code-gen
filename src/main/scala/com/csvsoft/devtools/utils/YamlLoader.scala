package com.csvsoft.devtools.utils

import java.io.{InputStreamReader, Reader}
import java.nio.charset.StandardCharsets

import scala.collection.JavaConverters._
import org.yaml.snakeyaml.Yaml

object YamlLoader {
  val varPattern = """\$\{([a-zA-Z0-9]+)\}""".r

  def loadYamlFromClassPath(classPath:String) :java.util.Map[String,Any]={
    val inputStream = this.getClass.getClassLoader.getResourceAsStream(classPath)
    val reader = new InputStreamReader(inputStream,StandardCharsets.UTF_8)
    ResourceUtils.use(reader)(loadYaml)
  }

  def loadYaml(reader:Reader):java.util.Map[String,Any]={
    val yaml = new Yaml()
    val map = yaml.load(reader).asInstanceOf[java.util.Map[String, Any]]
     resolveMap(map)
  }

  private def resolveMap(map:java.util.Map[String,Any]):java.util.Map[String,Any]={
    val m = map.asScala
    m.map(kv=> {
      val value = kv._2
      val newValue = value match {
        case s:String => resolve(s,map)
        case a => a
      }
      (kv._1,newValue)
    }).toMap.asJava
  }

  private def resolve(value:String,map:java.util.Map[String,Any]):String ={
    varPattern.replaceSomeIn(value, rm => Option(map.getOrDefault(rm group 1,rm group 1).toString))
  }
}
