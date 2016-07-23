package ca.ncct.uottawa.control.selforg.manager

import com.typesafe.config.ConfigFactory

import scala.xml.XML
import akka.actor.{Props, ActorSystem}
import ca.ncct.uottawa.control.selforg.manager.config.Config

/**
  * Created by Bogdan on 2/11/2016.
  */
object Bootstraper {

  val config = ConfigFactory.load()
  val system = ActorSystem("controlSystem", config.getConfig("remote").withFallback(config))
  def startManager(config: Config): Unit = {
    system.actorOf(Props(classOf[Manager], config), "manager")
  }

  def main(args: Array[String]): Unit = {
    // read system config
    val configXml = XML.loadFile(args(0))
    val config = Config.fromXML(configXml)

    startManager(config)
  }
}
