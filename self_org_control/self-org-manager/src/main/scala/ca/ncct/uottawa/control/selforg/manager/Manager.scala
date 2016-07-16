package ca.ncct.uottawa.control.selforg.manager

import java.net.{InetAddress, NetworkInterface}
import java.util

import akka.actor.{ActorLogging, Actor, ActorRef, Props}
import ca.ncct.uottawa.control.selforg.manager.config.Config

import scala.sys.process.Process

/**
  * Created by Bogdan on 7/16/2016.
  */

object Manager {
  def props(config: Config, filter: ActorRef): Props = Props(new Manager(config))
}

class Manager(config : Config) extends Actor with ActorLogging {
  import scala.concurrent.ExecutionContext.Implicits.global
  import scala.concurrent.duration._

  var startCount = 0

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
  var port = config.startPort + startCount
  val command = s"docker run -d --net=${config.networkName} --name=red5-$startCount " +
    s"-e red5_port=$port -e red5_ip=$localIpAddress -p $port:${config.startPort} bsolomon/red5-media:v1"
  log.debug("Base command is: " + command)

  override def preStart() = {
    context.system.scheduler.scheduleOnce(1000 millis, self, "tick")
  }

  // override postRestart so we don't call preStart and schedule a new message
  override def postRestart(reason: Throwable) = {}

  override def receive  = {
    case "tick" =>
      // send another periodic tick after the specified delay
      context.system.scheduler.scheduleOnce(60000 millis, self, "tick")
      startCount += 1
      port = config.startPort + startCount
      val command = s"docker run -d --net=${config.networkName} --name=red5-$startCount " +
        s"-e red5_port=$port -e red5_ip=$localIpAddress -p ${port}:${config.startPort} bsolomon/red5-media:v1"
      log.debug("New command is: " + command)
      Process(command).run()
  }
}
