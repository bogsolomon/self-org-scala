package ca.ncct.uottawa.control.selforg.manager

import java.nio.charset.StandardCharsets
import java.nio.file.{StandardOpenOption, Files, Paths}

import akka.actor.{ActorLogging, Actor, ActorRef, Props}
import ca.ncct.uottawa.control.selforg.manager.common.{RemoveNode, AddNode}
import ca.ncct.uottawa.control.selforg.manager.config.Config
import scala.concurrent.duration._

import scala.io.Source
import scala.sys.process.Process

/**
  * Created by Bogdan on 7/16/2016.
  */

object Manager {
  def props(config: Config, filter: ActorRef): Props = Props(new Manager(config))
}

class Manager(config : Config) extends Actor with ActorLogging {

  import scala.concurrent.ExecutionContext.Implicits.global

  val PERSITENCE_FILE: String = "instance-count.txt"

  var startCount = 0;
  if (Files.exists(Paths.get(PERSITENCE_FILE))) {
    startCount = Source.fromFile(PERSITENCE_FILE, "UTF-8").getLines().next().toInt
  } else {
    Files.createFile(Paths.get(PERSITENCE_FILE))
    Files.write(Paths.get(PERSITENCE_FILE), startCount.toString.getBytes(StandardCharsets.UTF_8))
  }

  val localIpAddress: String = System.getenv("red5_ip")
  var port = config.startPort + startCount
  val command = s"docker run -d --net=${config.networkName} --name=red5-$startCount " +
    s"-e red5_port=$port -e red5_ip=$localIpAddress -p $port:${config.startPort} bsolomon/red5-media:v1"
  log.debug("Base command is: " + command)

  override def receive  = {
    case AddNode =>
      addNode
    case RemoveNode =>
      removeNode
    case "tick" =>
      val count = startCount + 1
      var command = s"docker stop red5-control-$count"
      log.debug("Stop command is: " + command)
      Process(command).run().exitValue()
      command = s"docker rm red5-control-$count"
      log.debug("Stop command is: " + command)
      Process(command).run().exitValue()
  }

  def addNode: Unit = {
    startCount += 1
    Files.write(Paths.get(PERSITENCE_FILE), startCount.toString.getBytes(StandardCharsets.UTF_8),
      StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.TRUNCATE_EXISTING)
    port = config.startPort + startCount
    val command = s"docker run -d --net=${config.networkName} --name=red5-$startCount " +
      s"-e red5_port=$port -e red5_ip=$localIpAddress -p ${port}:${config.startPort} bsolomon/red5-media:v1"
    log.debug("New command is: " + command)
    Process(command).run().exitValue()
    val commandControl = s"docker run -d --net=${config.networkName} --name=red5-control-$startCount " +
      s"-e red5_port=$port -e red5_ip=$localIpAddress -e managed_host=red5-$startCount " +
      s"-v /usr/local/docker-images/red5-control/config:/config bsolomon/red5-control:v1"
    log.debug("New command is: " + commandControl)
    Process(commandControl).run().exitValue()
    sender ! AddNode
  }

  def removeNode: Unit = {
    var command = s"docker stop red5-$startCount"
    log.debug("Stop command is: " + command)
    Process(command).run().exitValue()
    command = s"docker rm red5-$startCount"
    log.debug("Stop command is: " + command)
    Process(command).run().exitValue()
    context.system.scheduler.scheduleOnce(180 second, self, "tick")
    startCount -= 1
    Files.write(Paths.get(PERSITENCE_FILE), startCount.toString.getBytes(StandardCharsets.UTF_8),
      StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.TRUNCATE_EXISTING)
    sender ! RemoveNode
  }
}
