package com.csvsoft.devtools


import com.csvsoft.devtools.utils.{CodeGenerator, ProjectConfigLoader, ResourceLoader}


object Main {
  
  def classPathCodeGen(projectName:String):Unit = {
    val classPathBase = "code_gen"
    val projectConfigLoader = ProjectConfigLoader.getClassPathLoader(classPathBase)
    val project = projectConfigLoader.loadProjectConfig(projectName)
    val resourceLoader = ResourceLoader.getClassPathResourceLoader(project,classPathBase)
    val codeGenerator = new CodeGenerator(resourceLoader)
    codeGenerator.genCode(project)

  }
  def main(args: Array[String]): Unit = {
    classPathCodeGen("project_A")

  }
  
}