package com.csvsoft.devtools.utils

import java.io.{File}
import com.github.tototoshi.csv._
trait CSVLoader{
  def loadCSV(csvName:String):List[Map[String,String]]
}
abstract class CSVLoaderBase extends CSVLoader {
   def loadCSV(file:File):List[Map[String,String]]={
     val reader = CSVReader.open(file)
     ResourceUtils.use(reader){ reader =>reader.allWithHeaders()}
   }
}

class ClassPathCSVLoader(classPathBase:String) extends CSVLoaderBase{
  override def loadCSV(csvName: String): List[Map[String, String]] = {
    val csvFileName = this.getClass.getClassLoader.getResource(s"$classPathBase/$csvName").getFile
    val csvFile = new File(csvFileName)
    if(!csvFile.exists()){
      throw new RuntimeException(s"File not found:$csvFile")
    }
    loadCSV(csvFile)
  }
}

object CSVLoader{
  def getClassPathCSVLoader(classPathBase:String):CSVLoader = new ClassPathCSVLoader(classPathBase)
}
