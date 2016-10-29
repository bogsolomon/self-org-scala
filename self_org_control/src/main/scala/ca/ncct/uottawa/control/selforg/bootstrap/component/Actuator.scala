package ca.ncct.uottawa.control.selforg.bootstrap.component

import akka.actor.{Actor, ActorLogging, Props}
import ca.ncct.uottawa.control.selforg.bootstrap.component.DecisionMaker.{Accept, DecisionType, Nochange, Reject}
import ca.ncct.uottawa.control.selforg.bootstrap.component.data.{Decision, SensorMeasurement}
import ca.ncct.uottawa.control.selforg.bootstrap.config.GenericConfig
import com.watchtogether.autonomic.selforg.red5.manager.group.GroupManager
import com.watchtogether.common.ClientPolicyMessage
import spray.client.pipelining._
import spray.http.{HttpRequest, HttpResponse}

import scala.concurrent.Future
import scala.util.{Failure, Success}

/**
  * Created by Bogdan on 8/9/2016.
  */
object Actuator {
  def props(config: GenericConfig): Props = Props(new Actuator(config))
}
class Actuator(config: GenericConfig) extends Actor with ActorLogging {
  val contName = System.getenv("managed_host")
  var urlRequest:String = "http://http://172.30.4.2:8080/serverIp?managedHost="+contName
  var envHost = ""

  val pipeline: SendReceive = sendReceive
  val response: Future[HttpResponse] = pipeline {
    Get(urlRequest)
  }
  response.onComplete {
    case Success(s: HttpResponse) => {
      envHost = s.entity.toString
    }
    case Failure(error) => {
    }
  }

  log.debug("envHost: " + envHost)
  val envPort: Int = System.getenv("red5_port").toInt

  override def receive  = {
    case msg : Decision => actuate(msg.decision)
  }

  def actuate(decision: DecisionType): Unit = {
    decision match {
      case Accept => GroupManager.getManager.broadcastMessage(new ClientPolicyMessage(true, envHost, envPort))
      case Reject => GroupManager.getManager.broadcastMessage(new ClientPolicyMessage(false, envHost, envPort))
      case Nochange =>
    }
  }
}
