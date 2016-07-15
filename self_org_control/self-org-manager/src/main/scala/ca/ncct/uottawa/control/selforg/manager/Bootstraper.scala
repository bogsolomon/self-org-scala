package ca.ncct.uottawa.control.selforg.manager

import java.net.{NetworkInterface, InetAddress}
import java.util

import scala.xml.XML
import akka.actor.{Props, ActorSystem}
import ca.ncct.uottawa.control.selforg.manager.config.Config

/**
  * Created by Bogdan on 2/11/2016.
  */
object Bootstraper {

  val system = ActorSystem("controlSystem")
  var startCount = 0

  def startManager(config: Config): Unit = {
    val interfaces = NetworkInterface.getNetworkInterfaces
    def findEth0Address: String = {
      while (interfaces.hasMoreElements) {
        val element = interfaces.nextElement
        if (element.getDisplayName.equalsIgnoreCase("eth0")) {
          val addresses: util.Enumeration[InetAddress] = element.getInetAddresses
          while (addresses.hasMoreElements) {
            val address = addresses.nextElement
            if (!address.getHostAddress.contains(":")) {
              return address.getHostAddress
            }
          }
        }
      }
      ""
    }

    val localIpAddress: String = findEth0Address
    val port = config.startPort + startCount
    val command = "docker run -d --net=" + config.networkName + " --name=red5-" + startCount + " -e \"red5_port=" + port +
      "\" -e \"red5_ip="+localIpAddress+"\" -p " + port + ":" + config.startPort + " bsolomon/red5-media:v1"
    println("Base command is: " + command)
  }

  def main(args: Array[String]): Unit = {
    // read system config
    val configXml = XML.loadFile(args(0))
    val config = Config.fromXML(configXml)

    startManager(config)
  }
}
