package com.csvsoft.kafrest

import java.util

import com.csvsoft.devtools.model.{CodeGenItem, Project}
import com.csvsoft.devtools.utils.YamlLoader
import org.scalatest.{FunSuite, Matchers}
import org.yaml.snakeyaml.Yaml
import freemarker.template.Configuration
import freemarker.template.TemplateExceptionHandler

import scala.collection.JavaConverters._


class JsonTest extends FunSuite with Matchers {

  test("yaml loader"){
    val map = YamlLoader.loadYamlFromClassPath("code_gen/poject_A/project.yaml")
    val srcDir = map.get("srcDir")
    println(srcDir)
  }
  test("project") {
    //code_gen/poject_A/project.yaml
    val yaml = new Yaml()
    val inputStream = this.getClass.getClassLoader.getResourceAsStream("code_gen/poject_A/project.yaml")
    // val map = yaml.load(inputStream).asInstanceOf[java.util.Map[String,Any]]

    val projectMap = yaml.load(inputStream).asInstanceOf[java.util.Map[String, Any]]
    val codGenItems: List[java.util.Map[String, String]] = projectMap.get("codeGenItems").asInstanceOf[java.util.List[java.util.Map[String, String]]].asScala.toList

    val project = Project(projectName = projectMap.get("projectName").asInstanceOf[String],
      outputDir = projectMap.get("outputDir").asInstanceOf[String]
      , codeGenItems = codGenItems.map(m => CodeGenItem(m.get("model"), m.get("template"), m.get("out"))))
    println(project)
  }

  test("yaml") {
    val yaml = new Yaml()
    val inputStream = this.getClass.getClassLoader.getResourceAsStream("test.yaml")
    val map = yaml.load(inputStream).asInstanceOf[java.util.Map[String, Any]]
    println(map)

    /*
    val engine = new TemplateEngine
    val output = engine.layout("/test.ssp",Map("firstName" -> "John","model" -> map))
    println(output)
    */


    /* ------------------------------------------------------------------------ *//* ------------------------------------------------------------------------ */
    /* You should do this ONLY ONCE in the whole application life-cycle:        */

    /* Create and adjust the configuration singleton */
    val cfg = new Configuration(Configuration.VERSION_2_3_29)
    // cfg.setDirectoryForTemplateLoading(new File("/where/you/store/templates"))
    cfg.setClassLoaderForTemplateLoading(this.getClass.getClassLoader, "/")
    // Recommended settings for new projects:
    cfg.setDefaultEncoding("UTF-8")
    cfg.setTemplateExceptionHandler(TemplateExceptionHandler.RETHROW_HANDLER)
    cfg.setLogTemplateExceptions(false)
    cfg.setWrapUncheckedExceptions(true)
    cfg.setFallbackOnNullLoopVariable(false)

    import java.io.OutputStreamWriter
    /* Create a data-model *//* Create a data-model */
    val root = new util.HashMap[String, Any]()
    root.put("firstName", "Big Joe")


    /* Get the template (uses cache internally) */
    val temp = cfg.getTemplate("test.ftl")

    /* Merge data-model with template */
    val out = new OutputStreamWriter(System.out)
    temp.process(map, out)
    // Note: Depending on what `out` is, you may need to call `out.close()`.
    // This is usually the case for file output, but not for servlet output.

  }


}
