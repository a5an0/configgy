/*
 * Copyright 2009 Robey Pointer <robeypointer@gmail.com>
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may
 * not use this file except in compliance with the License. You may obtain
 * a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.lag.configgy

import java.net.InetAddress

import scala.collection.jcl
import scala.collection.mutable

// grr, scala can wrap any specific java Map type, but not the generic Map. why not?
private class JavaMap[K, E](override val underlying: java.util.Map[K, E]) extends jcl.MapWrapper[K, E]


/**
 * A ConfigMap that wraps the system environment. This is used as a
 * fallback when looking up "$(...)" substitutions in config files.
 */
private[configgy] object EnvironmentAttributes extends ConfigMap {
  // FIXME use immutable HashMap.Empty
  private val env = new mutable.HashMap[String, String]
  env ++= new JavaMap(System.getenv()).elements

  def getString(key: String): Option[String] = env.get(key)

  def getConfigMap(key: String): Option[ConfigMap] = None
  def configMap(key: String): ConfigMap = error("not implemented")

  def getList(key: String): Seq[String] = getString(key) match {
    case None => Array[String]()
    case Some(x) => Array[String](x)
  }

  def setString(key: String, value: String): Unit = error("read-only attributes")
  def setList(key: String, value: Seq[String]): Unit = error("read-only attributes")
  def setConfigMap(key: String, value: ConfigMap): Unit = error("read-only attributes")
  def contains(key: String): Boolean = env.contains(key)
  def remove(key: String): Boolean = error("read-only attributes")
  def keys: Iterator[String] = env.keys
  def asMap(): Map[String, String] = error("not implemented")
  def subscribe(subscriber: Subscriber): SubscriptionKey = error("not implemented")
  def copy(): ConfigMap = this


  try {
    val addr = InetAddress.getLocalHost
    val ip = addr.getHostAddress
    val dns = addr.getHostName

    if (ip != null) {
      env("HOSTIP") = ip
    }
    if (dns != null) {
      env("HOSTNAME") = dns
    }
  } catch {
    case _ => // pass
  }
}
