package ca.ncct.uottawa.control.selforg.manager

import java.net.{NetworkInterface, InetAddress}

import scala.xml.XML
import akka.actor.{Props, ActorSystem}
import ca.ncct.uottawa.control.selforg.manager.config.Config

/**
  * Created by Bogdan on 2/11/2016.
  */
object Bootstraper {

  val system = ActorSystem("controlSystem")
  val startCount = 0

  def startManager(config: Config): Unit = {
    val interfaces = NetworkInterface.getNetworkInterfaces
    var localIpAddress: String = ""
    while (interfaces.hasMoreElements) {
      val element = interfaces.nextElement
      if (element.getDisplayName.equalsIgnoreCase("eth0")) {
        localIpAddress = element.getInetAddresses.nextElement().getHostAddress
      }
    }
    val port = config.startPort + startCount
    println("Command is: " + "docker run -d --net=" + config.networkName + " --name=red5-" + startCount + " -e \"red5_port=" + port +
      "\" -e \"red5_ip="+localIpAddress+"\" -p " + port + ":" + config.startPort + " bsolomon/red5-media:v1")
  }

  def main(args: Array[String]): Unit = {
    // read system config
    val configXml = XML.loadFile(args(0))
    val config = Config.fromXML(configXml)

    startManager(config)
  }
}
