package ca.ncct.uottawa.control.selforg.manager

import java.nio.charset.StandardCharsets
import java.nio.file.{Files, Paths, StandardOpenOption}

import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import ca.ncct.uottawa.control.selforg.manager.common.{AddNode, RemoveNode}
import ca.ncct.uottawa.control.selforg.manager.config.Config
import spray.client.pipelining._
import spray.http.HttpResponse

import scala.concurrent.Future
import scala.concurrent.duration._
import scala.io.Source
import scala.util.{Failure, Success}

/**
  * Created by Bogdan on 7/16/2016.
  */

object Manager {
  def props(config: Config, filter: ActorRef): Props = Props(new Manager(config))
}

case class RemoveController(instName:String)

class Manager(config : Config) extends Actor with ActorLogging {

  import scala.concurrent.ExecutionContext.Implicits.global

  val PERSITENCE_FILE: String = "instance-count.txt"
  val RM_NODE_URL = "http://172.30.4.2:8080/removeNode?"

  var startCount = 0;
  if (Files.exists(Paths.get(PERSITENCE_FILE))) {
    startCount = Source.fromFile(PERSITENCE_FILE, "UTF-8").getLines().next().toInt
  } else {
    Files.createFile(Paths.get(PERSITENCE_FILE))
    Files.write(Paths.get(PERSITENCE_FILE), startCount.toString.getBytes(StandardCharsets.UTF_8))
  }

  var port = config.startPort + startCount

  override def receive  = {
    case AddNode(instName, controllName) =>
      addNode(instName, controllName, sender)
    case RemoveNode(instName, controllName) =>
      removeNode(instName, controllName, sender)
    case RemoveController(instName) =>
      removeControl(instName)
  }

  def addNode(instName: String, controlName:String, senderAct: ActorRef): Unit = {
    startCount += 1
    Files.write(Paths.get(PERSITENCE_FILE), startCount.toString.getBytes(StandardCharsets.UTF_8),
      StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.TRUNCATE_EXISTING)
    port = config.startPort + startCount
    val pipeline: SendReceive = sendReceive
    val response: Future[HttpResponse] = pipeline {
      Get(s"http://172.30.4.2:8080/addNode?instName=${instName}&controlName=${controlName}" +
        s"&port=${port}&netName=${config.networkName}&startPort=${config.startPort}")
    }
    response.onComplete {
      case Success(s: HttpResponse) => {
        log.debug("Add server answer: " + s.entity.asString)
        senderAct ! AddNode
      }
      case Failure(error) => {
      }
    }
  }

  def removeNode(instName: String, controllName:String, senderAct: ActorRef): Unit = {
    val pipeline: SendReceive = sendReceive
    val response: Future[HttpResponse] = pipeline {
      Get(s"http://172.30.4.2:8080/removeNode?instName=${instName}")
    }
    response.onComplete {
      case Success(s: HttpResponse) => {
        log.debug("Server removed answer: " + s.entity.asString)
        context.system.scheduler.scheduleOnce(1 minute, self, RemoveController(controllName))
        startCount -= 1
        Files.write(Paths.get(PERSITENCE_FILE), startCount.toString.getBytes(StandardCharsets.UTF_8),
          StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.TRUNCATE_EXISTING)
        senderAct ! RemoveNode
      }
      case Failure(error) => {
      }
    }
  }

  def removeControl(controlName:String): Unit = {
    val pipeline: SendReceive = sendReceive
    val response: Future[HttpResponse] = pipeline {
      Get(s"http://172.30.4.2:8080/removeNode?instName=${controlName}")
    }
    response.onComplete {
      case Success(s: HttpResponse) => {
        log.debug("Controller removed answer: " + s.entity.asString)
      }
      case Failure(error) => {
      }
    }
  }
}
