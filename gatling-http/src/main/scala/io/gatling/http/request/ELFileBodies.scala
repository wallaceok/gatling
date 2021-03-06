/**
 * Copyright 2011-2014 eBusiness Information, Groupe Excilys (www.ebusinessinformation.fr)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * 		http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.gatling.http.request

import scala.collection.JavaConversions.mapAsScalaConcurrentMap
import scala.collection.concurrent

import org.apache.commons.io.IOUtils
import jsr166e.ConcurrentHashMapV8

import io.gatling.core.config.GatlingConfiguration.configuration
import io.gatling.core.config.Resource
import io.gatling.core.session.Expression
import io.gatling.core.session.el.EL
import io.gatling.core.util.IO
import io.gatling.core.validation.Validation

object ELFileBodies extends IO {

  val cache: concurrent.Map[String, Validation[Expression[String]]] = new ConcurrentHashMapV8[String, Validation[Expression[String]]]
  private val cacheELFileBodies = configuration.http.cacheELFileBodies
  def cached(path: String) =
    if (cacheELFileBodies)
      cache.getOrElseUpdate(path, compileFile(path))
    else
      compileFile(path)

  def compileFile(path: String): Validation[Expression[String]] =
    Resource.requestBody(path)
      .map(resource => withCloseable(resource.inputStream)(IOUtils.toString(_, configuration.core.charset)))
      .map(_.el[String])

  def asString(filePath: Expression[String]): Expression[String] = session =>
    for {
      path <- filePath(session)
      expression <- cached(path)
      body <- expression(session)
    } yield body
}
