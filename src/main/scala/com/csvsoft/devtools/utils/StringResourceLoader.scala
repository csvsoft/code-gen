package com.csvsoft.devtools.utils

import java.io.ByteArrayInputStream
import java.nio.charset.StandardCharsets

import org.apache.commons.collections.ExtendedProperties
import org.apache.velocity.exception.ResourceNotFoundException
import org.apache.velocity.runtime.resource.Resource

class StringResourceLoader  extends org.apache.velocity.runtime.resource.loader.ResourceLoader {
  @Override
  def init( extendedProperties:ExtendedProperties)= {

  }



  @throws[ResourceNotFoundException]
  def getResourceStream(s: String) = new ByteArrayInputStream(s.getBytes(StandardCharsets.UTF_8))

  override def isSourceModified(resource: Resource ) = false

  override def getLastModified(resource: Resource) = 0
}
