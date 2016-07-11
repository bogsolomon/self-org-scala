package ca.ncct.uottawa.control.selforg.bootstrap.component

import akka.actor.{ActorRef, Props, ActorLogging, Actor}
import ca.ncct.uottawa.control.selforg.bootstrap.component.data.SensorMeasurement
import ca.ncct.uottawa.control.selforg.bootstrap.config.SensorConfig
import scala.concurrent.Future
import scala.concurrent.duration._
import scala.util.{Failure, Success}

object Red5Sensor {
  def props(config: SensorConfig, filter: ActorRef): Props = Props(new Red5Sensor(config, filter))
}

/**
  * Created by Bogdan on 2/21/2016.
  */
class Red5Sensor(config: SensorConfig, filter: ActorRef) extends Actor with ActorLogging {

  def SERVLET_URL = "serverStats"
  var measurementURL:String = "http://" + config.managedServerHost + ":" + config.managedServerPort + "/" + config.managedApp + "/" + SERVLET_URL

  import spray.http._
  import spray.client.pipelining._
  import scala.concurrent.ExecutionContext.Implicits.global

  def mapMediaType: HttpResponse => HttpResponse = {
    case x@HttpResponse(_, _, _, _) => {
       x.copy(x.status, HttpEntity(ContentType(MediaTypes.`application/xml`), x.entity.data))
    }
  }

  override def preStart() = {
    context.system.scheduler.scheduleOnce(config.scheduledTime millis, self, "tick")
  }

  // override postRestart so we don't call preStart and schedule a new message
  override def postRestart(reason: Throwable) = {}

  override def receive  = {
    case "tick" =>
      // send another periodic tick after the specified delay
      context.system.scheduler.scheduleOnce(config.scheduledTime millis, self, "tick")
      val pipeline: HttpRequest => Future[SensorMeasurement] = sendReceive ~> mapMediaType ~> unmarshal[SensorMeasurement]
      val response: Future[SensorMeasurement] = pipeline(Get(measurementURL))
      log.debug(this.toString)
      response.onComplete {
        case Success(s: SensorMeasurement) => {
          filter ! s
        }
        case Failure(error) =>
          log.error(error, "Couldn't get sensor metrics")
      }
  }
}
