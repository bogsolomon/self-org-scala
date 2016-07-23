package ca.ncct.uottawa.control.selforg.manager

import java.net.{InetAddress, NetworkInterface}
import java.nio.charset.StandardCharsets
import java.nio.file.{StandardOpenOption, Files, Paths}
import java.util

import akka.actor.{ActorLogging, Actor, ActorRef, Props}
import ca.ncct.uottawa.control.selforg.manager.common.{RemoveNode, AddNode}
import ca.ncct.uottawa.control.selforg.manager.config.Config

import scala.io.Source
import scala.sys.process.Process

/**
  * Created by Bogdan on 7/16/2016.
  */

object Manager {
  def props(config: Config, filter: ActorRef): Props = Props(new Manager(config))
}

class Manager(config : Config) extends Actor with ActorLogging {
  val PERSITENCE_FILE: String = "instance-count.txt"

  var startCount = 0;
  if (Files.exists(Paths.get(PERSITENCE_FILE))) {
    startCount = Source.fromFile(PERSITENCE_FILE, "UTF-8").getLines().next().toInt
  } else {
    Files.createFile(Paths.get(PERSITENCE_FILE))
    Files.write(Paths.get(PERSITENCE_FILE), startCount.toString.getBytes(StandardCharsets.UTF_8))
  }

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

  override def receive  = {
    case AddNode =>
      addNode
    case RemoveNode =>
      removeNode
  }

  def addNode: Unit = {
    startCount += 1
    Files.write(Paths.get(PERSITENCE_FILE), startCount.toString.getBytes(StandardCharsets.UTF_8),
      StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.TRUNCATE_EXISTING)
    port = config.startPort + startCount
    val command = s"docker run -d --net=${config.networkName} --name=red5-$startCount " +
      s"-e red5_port=$port -e red5_ip=$localIpAddress -p ${port}:${config.startPort} bsolomon/red5-media:v1"
    log.debug("New command is: " + command)
    Process(command).run()
  }

  def removeNode: Unit = {
    var command = s"docker stop red5-$startCount"
    log.debug("Stop command is: " + command)
    Process(command).run().exitValue()
    command = s"docker rm red5-$startCount"
    log.debug("Stop command is: " + command)
    Process(command).run()
    startCount -= 1
    Files.write(Paths.get(PERSITENCE_FILE), startCount.toString.getBytes(StandardCharsets.UTF_8),
      StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.TRUNCATE_EXISTING)
  }
}
