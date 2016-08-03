package ca.ncct.uottawa.control.selforg.bootstrap

import akka.actor._
import ca.ncct.uottawa.control.selforg.bootstrap.component.{FuzzyModel, Coordinator, DataFilterChain, Red5Sensor}
import ca.ncct.uottawa.control.selforg.bootstrap.config._

import scala.xml.XML

/**
  * Created by Bogdan on 2/11/2016.
  */
object Bootstraper {

  val system = ActorSystem("controlSystem")

  def createSensor(config: SensorConfig, filter: ActorRef) = {
    val sensor = system.actorOf(Props(classOf[Red5Sensor], config, filter), "sensor")
  }

  def createFilter(config: FilterConfig, coordinator: ActorRef) = {
    system.actorOf(Props(classOf[DataFilterChain], config, coordinator), "filter")
  }

  def createCoordinator(config: CoordinatorConfig, model: ActorRef) = {
    system.actorOf(Props(classOf[Coordinator], config, model), "coordinator")
  }

  def createModel(config: ModelConfig) = {
    system.actorOf(Props(classOf[FuzzyModel], config), "model")
  }

  def createControlLoop(config: Config): Unit = {
    val model: ActorRef = createModel(config.model)
    val coordinator: ActorRef = createCoordinator(config.coordinator, model)
    val filter: ActorRef = createFilter(config.filter, coordinator)
    config.sensors.foreach(x => {
      createSensor(x, filter)
    })

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
