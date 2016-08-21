package ca.ncct.uottawa.control.selforg.bootstrap

import java.net.{InetAddress, NetworkInterface}
import java.util

import akka.actor._
import ca.ncct.uottawa.control.selforg.bootstrap.component._
import ca.ncct.uottawa.control.selforg.bootstrap.config._
import com.typesafe.config.ConfigFactory
import com.watchtogether.autonomic.selforg.red5.manager.group.GroupManager

import scala.xml.XML

/**
  * Created by Bogdan on 2/11/2016.
  */
object Bootstraper {

  def createSensor(config: SensorConfig, filter: ActorRef, system: ActorSystem) = {
    system.actorOf(Props(classOf[Red5Sensor], config, filter), "sensor")
  }

  def createFilter(config: FilterConfig, coordinator: ActorRef, system: ActorSystem) = {
    system.actorOf(Props(classOf[DataFilterChain], config, coordinator), "filter")
  }

  def createCoordinator(config: GenericConfig, model: ActorRef, estimator: ActorRef, decisionMaker: ActorRef, actuator: ActorRef, system: ActorSystem) = {
    system.actorOf(Props(classOf[Coordinator], config, model, estimator, decisionMaker, actuator), "coordinator")
  }

  def createModel(config: GenericConfig, system: ActorSystem) = {
    system.actorOf(Props(classOf[FuzzyModel], config), "model")
  }

  def createEstimator(config: GenericConfig, system: ActorSystem) = {
    system.actorOf(Props(classOf[Estimator], config), "estimator")
  }

  def createDecisionMaker(config: GenericConfig, system: ActorSystem) = {
    system.actorOf(Props(classOf[DecisionMaker], config), "decisionMaker")
  }

  def createActuator(config: GenericConfig, system: ActorSystem) = {
    system.actorOf(Props(classOf[Actuator], config), "actuator")
  }

  def createControlLoop(systemConfig: Config): Unit = {
    val localIpAddress: String = findEth0Address
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
    val system = ActorSystem("controlSystem", customConf.getConfig("remote").
      withFallback(ConfigFactory.parseString("akka.cluster.roles = [control]")).
      withFallback(ConfigFactory.parseString(s"""akka.cluster.seed-nodes = ["akka.tcp://controlSystem@${systemConfig.seedNode}:2551"]""")).
      withFallback(config))

    GroupManager.getManager
    val model: ActorRef = createModel(systemConfig.model, system)
    val estimator: ActorRef = createEstimator(systemConfig.estimatorConfig, system)
    val decisionMaker: ActorRef = createDecisionMaker(systemConfig.dmConfig, system)
    val actuator: ActorRef = createActuator(systemConfig.actuatorConfig, system)
    val coordinator: ActorRef = createCoordinator(systemConfig.coordinator, model, estimator, decisionMaker, actuator, system)
    val filter: ActorRef = createFilter(systemConfig.filter, coordinator, system)
    systemConfig.sensors.foreach(x => {
      createSensor(x, filter, system)
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

  def findEth0Address: String = {
    val interfaces = NetworkInterface.getNetworkInterfaces
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
}

class UnhandledMessageListener extends Actor with ActorLogging {

  override def receive = {
    case message: UnhandledMessage =>
      log.error(s"CRITICAL! No actors found for message ${message.getMessage}")

    log.error("Shutting application down")
    System.exit(-1)
  }
}
