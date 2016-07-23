package ca.ncct.uottawa.control.selforg.manager

import ca.ncct.uottawa.control.selforg.manager.util.Utils
import com.typesafe.config.ConfigFactory

import scala.xml.XML
import akka.actor.{Props, ActorSystem}
import ca.ncct.uottawa.control.selforg.manager.config.Config

/**
  * Created by Bogdan on 2/11/2016.
  */
object Bootstraper {

  val localIpAddress: String = Utils.findEth0Address
  val customConf = ConfigFactory.parseString(
  s"""remote {
  akka {
    remote {
      netty.tcp {
        hostname = "${localIpAddress}"
      }
    }
  }
  }""")
  val config = ConfigFactory.load()
  val system = ActorSystem("controlSystem", customConf.getConfig("remote").withFallback(config))
  def startManager(config: Config): Unit = {
    system.actorOf(Props(classOf[Manager], config), "manager")
  }

  def main(args: Array[String]): Unit = {
    var configLoc = "config.xml"

    if (args.length != 0) {
      configLoc = args(0)
    }

    // read system config
    val configXml = XML.loadFile(configLoc)
    val config = Config.fromXML(configXml)

    startManager(config)
  }
}
