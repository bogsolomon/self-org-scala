package ca.ncct.uottawa.control.selforg.bootstrap

import akka.actor._
import ca.ncct.uottawa.control.selforg.bootstrap.component._
import ca.ncct.uottawa.control.selforg.bootstrap.config._
import com.watchtogether.autonomic.selforg.red5.manager.group.GroupManager

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

  def createCoordinator(config: GenericConfig, model: ActorRef, estimator: ActorRef, decisionMaker: ActorRef, actuator: ActorRef) = {
    system.actorOf(Props(classOf[Coordinator], config, model, estimator, decisionMaker, actuator), "coordinator")
  }

  def createModel(config: GenericConfig) = {
    system.actorOf(Props(classOf[FuzzyModel], config), "model")
  }

  def createEstimator(config: GenericConfig) = {
    system.actorOf(Props(classOf[Estimator], config), "estimator")
  }

  def createDecisionMaker(config: GenericConfig) = {
    system.actorOf(Props(classOf[DecisionMaker], config), "decisionMaker")
  }

  def createActuator(config: GenericConfig) = {
    system.actorOf(Props(classOf[Actuator], config), "actuator")
  }

  def createControlLoop(config: Config): Unit = {
    GroupManager.getManager
    val model: ActorRef = createModel(config.model)
    val estimator: ActorRef = createEstimator(config.estimatorConfig)
    val decisionMaker: ActorRef = createDecisionMaker(config.dmConfig)
    val actuator: ActorRef = createActuator(config.actuatorConfig)
    val coordinator: ActorRef = createCoordinator(config.coordinator, model, estimator, decisionMaker, actuator)
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
