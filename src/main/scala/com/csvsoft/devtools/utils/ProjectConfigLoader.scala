package com.csvsoft.devtools.utils


import com.csvsoft.devtools.model.{CodeGenItem, Project}
import scala.collection.JavaConverters._

trait ProjectConfigLoader {
  def loadProjectConfig(name: String): Project
}

class ClassPathProjectConfigLoader(classPathBase: String) extends ProjectConfigLoader {
val yamlLoader:YamlLoader = YamlLoader.getClassPathYamlLoader(classPathBase)


  override def loadProjectConfig(name: String): Project = {
    val projectMap = yamlLoader.loadYaml(s"$name/project.yaml")
    val codGenItems: List[java.util.Map[String, String]] = projectMap.get("codeGenItems").asInstanceOf[java.util.List[java.util.Map[String, String]]].asScala.toList

    val project = Project(projectName = projectMap.get("projectName").asInstanceOf[String],
      outputDir = projectMap.get("outputDir").asInstanceOf[String]
      , codeGenItems = codGenItems.map(m => CodeGenItem(m.get("model"), m.get("template"), m.get("out"))))
    project  }
}

object ProjectConfigLoader {
  def getClassPathLoader(classPathBase: String) = new ClassPathProjectConfigLoader(classPathBase)
}
