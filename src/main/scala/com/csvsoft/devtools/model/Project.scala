package com.csvsoft.devtools.model
case class CodeGenItem(model:String,template:String,out:String)
case class Project(projectName:String,outputDir:String,codeGenItems:List[CodeGenItem]){

}