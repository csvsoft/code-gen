package com.csvsoft.devtools.utils

import scala.language.reflectiveCalls
object ResourceUtils {
  def use[A <: {def close()},B](a:A)( f: A=>B):B ={
    try{
      f(a)
    }finally{
      a.close()
    }
  }
}
