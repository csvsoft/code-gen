package com.csvsoft.devtools.utils

import java.io.{InputStreamReader, Reader}
import java.nio.charset.StandardCharsets

import com.csvsoft.devtools.model.{CodeGenItem, Project}
import scala.collection.JavaConverters._
class ProjectConfigLoader {

  def loadProjectFromClassPathBase(classPathBase:String):Project={
    loadProjectFromClassPath(s"$classPathBase/project.yaml")
  }
  def loadProjectFromClassPath(classPath:String):Project ={
    val inputStream = this.getClass.getClassLoader.getResourceAsStream(classPath)
    val reader = new InputStreamReader(inputStream,StandardCharsets.UTF_8)
    ResourceUtils.use(reader)(loadProject)
    }
  def loadProject(reader:Reader):Project ={
    val projectMap = YamlLoader.loadYaml(reader)
    val codGenItems: List[java.util.Map[String, String]] = projectMap.get("codeGenItems").asInstanceOf[java.util.List[java.util.Map[String, String]]].asScala.toList

    val project = Project(projectName = projectMap.get("projectName").asInstanceOf[String],
      outputDir = projectMap.get("outputDir").asInstanceOf[String]
      , codeGenItems = codGenItems.map(m => CodeGenItem(m.get("model"), m.get("template"), m.get("out"))))
    project
  }
}
