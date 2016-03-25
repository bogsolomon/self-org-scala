package ca.ncct.uottawa.control.selforg.bootstrap

import akka.actor.{Props, ActorSystem}
import ca.ncct.uottawa.control.selforg.bootstrap.component.Red5Sensor
import ca.ncct.uottawa.control.selforg.bootstrap.config.{SensorConfig, Config}

import scala.xml.XML

/**
  * Created by Bogdan on 2/11/2016.
  */
object Bootstraper {

  val system = ActorSystem("controlSystem")

  def createSensor(config: SensorConfig) = {
    val sensor = system.actorOf(Props(classOf[Red5Sensor], config), "sensor")
  }

  def createControlLoop(config: Config): Unit = {
    config.sensors.foreach(x => createSensor(x))
  }

  def main(args: Array[String]): Unit = {
    // read system config
    val configXml = XML.loadFile(args(0))
    val config = Config.fromXML(configXml)

    createControlLoop(config)
  }
}
