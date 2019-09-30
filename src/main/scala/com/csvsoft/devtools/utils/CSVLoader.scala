package com.csvsoft.devtools.utils

import java.io.{File}
import com.github.tototoshi.csv._

object CSVLoader {
   def loadCSV(file:File):List[Map[String,String]]={
     val reader = CSVReader.open(file)
     ResourceUtils.use(reader){ reader =>reader.allWithHeaders()}
   }
}
