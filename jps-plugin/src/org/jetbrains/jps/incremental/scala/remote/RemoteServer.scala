package org.jetbrains.jps.incremental.scala
package remote

import java.net.{ConnectException, InetAddress, UnknownHostException}

import com.intellij.util.Base64Converter
import org.jetbrains.jps.incremental.ModuleLevelBuilder.ExitCode
import org.jetbrains.jps.incremental.scala.data.{CompilationData, CompilerData, SbtData}

/**
 * @author Pavel Fatin
 */
class RemoteServer(val address: InetAddress, val port: Int) extends Server with RemoteResourceOwner {
  def compile(sbtData: SbtData, compilerData: CompilerData, compilationData: CompilationData, client: Client): ExitCode = {
    val arguments = {
      val strings = Arguments(sbtData, compilerData, compilationData, Seq.empty).asStrings
      strings.map(s => Base64Converter.encode(s.getBytes("UTF-8")))
    }

    try {
      send(serverAlias, arguments, client)
      ExitCode.OK
    } catch {
      case e: ConnectException =>
        val message = "Cannot connect to compile server at %s:%s\nTrying to compile without it".format(address.toString, port)
        client.warning(message)
        ScalaBuilderDelegate.localServer.compile(sbtData, compilerData, compilationData, client)
      case e: UnknownHostException =>
        val message = "Unknown IP address of compile server host: " + address.toString
        client.error(message)
        ExitCode.ABORT
    }
  }
}