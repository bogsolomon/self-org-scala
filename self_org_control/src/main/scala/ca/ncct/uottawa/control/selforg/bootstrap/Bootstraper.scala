package ca.ncct.uottawa.control.selforg.bootstrap

import akka.actor._
import ca.ncct.uottawa.control.selforg.bootstrap.component.{DataFilterChain, Red5Sensor}
import ca.ncct.uottawa.control.selforg.bootstrap.config.{FilterConfig, SensorConfig, Config}

import scala.xml.XML

/**
  * Created by Bogdan on 2/11/2016.
  */
object Bootstraper {

  val system = ActorSystem("controlSystem")

  def createSensor(config: SensorConfig, filter: ActorRef) = {
    val sensor = system.actorOf(Props(classOf[Red5Sensor], config, filter), "sensor")
  }

  def createFilter(config: FilterConfig) = {
    system.actorOf(Props(classOf[DataFilterChain], config), "filter")
  }

  def createControlLoop(config: Config): Unit = {
    config.sensors.foreach(x => createSensor(x, createFilter(config.filter)))

    val listener = system.actorOf(Props(new UnhandledMessageListener()))
    system.eventStream.subscribe(listener, classOf[UnhandledMessage])
  }

  def main(args: Array[String]): Unit = {
    // read system config
    val configXml = XML.loadFile(args(0))
    val config = Config.fromXML(configXml)

    createControlLoop(config)
  }
}

class UnhandledMessageListener extends Actor with ActorLogging {

  override def receive = {
    case message: UnhandledMessage =>
      log.error(s"CRITICAL! No actors found for message ${message.getMessage}")

    log.error("Shutting application down")
    System.exit(-1)
  }
}
