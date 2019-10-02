package com.csvsoft.devtools.utils

import java.util

import com.csvsoft.devtools.model.Project
import freemarker.template.{Configuration, Template, TemplateExceptionHandler}
import org.apache.velocity

trait ResourceLoader {
  def loadModel(modelName: String): java.util.Map[String, Any]

  def getFreeMarkerTemplate(templateName: String): Template

  def getVelocityTemplate(templateName: String): velocity.Template
}

class ClassPathResourceLoader(project: Project, classPathBase: String) extends ResourceLoader {
  val yamlLoader = YamlLoader.getClassPathYamlLoader(s"$classPathBase/${project.projectName}/model")
  val freeMarkerCfg = initFreeMarker

  private[this] def initFreeMarker(): Configuration = {
    val cfg = new Configuration(Configuration.VERSION_2_3_29)
    // cfg.setDirectoryForTemplateLoading(new File("/where/you/store/templates"))
    cfg.setClassLoaderForTemplateLoading(this.getClass.getClassLoader, s"$classPathBase/${project.projectName}/ftl")
    // Recommended settings for new projects:
    cfg.setDefaultEncoding("UTF-8")
    cfg.setTemplateExceptionHandler(TemplateExceptionHandler.RETHROW_HANDLER)
    cfg.setLogTemplateExceptions(false)
    cfg.setWrapUncheckedExceptions(true)
    cfg.setFallbackOnNullLoopVariable(false)
    cfg
  }

  override def loadModel(modelName: String): util.Map[String, Any] = {
    yamlLoader.loadYaml(modelName)
  }

  override def getFreeMarkerTemplate(templateName: String): Template = freeMarkerCfg.getTemplate(templateName)

  override def getVelocityTemplate(templateName: String): velocity.Template = {
    VelocityTemplateUtils.getClassPathTemplate(s"$classPathBase/${project.projectName}/vm/$templateName")
  }
}

object ResourceLoader {
  def getClassPathResourceLoader(project: Project, classPathBase: String): ResourceLoader = new ClassPathResourceLoader(project, classPathBase)
}



