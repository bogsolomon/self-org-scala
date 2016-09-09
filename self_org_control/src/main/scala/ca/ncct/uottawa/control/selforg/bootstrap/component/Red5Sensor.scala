package ca.ncct.uottawa.control.selforg.bootstrap.component

import akka.actor.{ActorRef, Props, ActorLogging, Actor}
import akka.cluster.Cluster
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

  val managedServerHost = System.getenv("managed_host")
  def SERVLET_URL = "serverStats"
  var measurementURL:String = "http://" + managedServerHost + ":" + config.managedServerPort + "/" + config.managedApp + "/" + SERVLET_URL
  var MAX_FAIL = 5
  var failCount = 0

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
      response.onComplete {
        case Success(s: SensorMeasurement) => {
          log.debug(s.toString)
          filter ! s
          failCount = 0
        }
        case Failure(error) => {
          failCount += 1
          log.error(error, "Couldn't get sensor metrics")
          if (failCount > MAX_FAIL) {
            log.info("Leaving cluster")
            Cluster(context.system).leave(Cluster(context.system).selfAddress)
            log.info("Shutting down")
            context.system.shutdown()
          }
        }
      }
  }
}
