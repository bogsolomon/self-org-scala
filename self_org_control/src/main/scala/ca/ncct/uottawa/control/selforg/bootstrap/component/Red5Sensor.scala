package ca.ncct.uottawa.control.selforg.bootstrap.component

import akka.actor.{Props, ActorLogging, Actor}
import ca.ncct.uottawa.control.selforg.bootstrap.component.data.SensorMeasurement
import ca.ncct.uottawa.control.selforg.bootstrap.config.SensorConfig
import scala.concurrent.Future
import scala.concurrent.duration._
import scala.util.Try

object Red5Sensor {
  def props(config: SensorConfig): Props = Props(new Red5Sensor(config))
}

/**
  * Created by Bogdan on 2/21/2016.
  */
class Red5Sensor(config: SensorConfig) extends Actor with ActorLogging {

  def SERVLET_URL = "serverStats"
  var measurementURL:String = "http://" + config.managedServerHost + ":" + config.managedServerPort + "/" + config.managedApp + "/" + SERVLET_URL

  import spray.http._
  import spray.client.pipelining._
  import scala.concurrent.ExecutionContext.Implicits.global

  override def preStart() = {
    context.system.scheduler.scheduleOnce(config.scheduledTime millis, self, "tick")
  }

  // override postRestart so we don't call preStart and schedule a new message
  override def postRestart(reason: Throwable) = {}

  override def receive  = {
    case "tick" =>
      // send another periodic tick after the specified delay
      context.system.scheduler.scheduleOnce(config.scheduledTime millis, self, "tick")
      val pipeline: HttpRequest => Future[SensorMeasurement] = sendReceive ~> unmarshal[SensorMeasurement]
      val response: Future[SensorMeasurement] = pipeline(Get(measurementURL))
      log.debug(this.toString)
      response.onComplete(
    case SUcc
      )
  }
}
