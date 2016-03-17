package ca.ncct.uottawa.control.selforg.bootstrap.component

import akka.actor.Actor
import akka.event.Logging
import scala.concurrent.duration._

/**
  * Created by Bogdan on 2/21/2016.
  */
class Red5Sensor extends Actor {
  import context._
  import spray.http._
  import spray.client.pipelining._

  val log = Logging(context.system, this)

  override def preStart() =
    system.scheduler.scheduleOnce(500 millis, self, "tick")

  // override postRestart so we don't call preStart and schedule a new message
  override def postRestart(reason: Throwable) = {}

  override def receive  = {
    case "tick" =>
      // send another periodic tick after the specified delay
      system.scheduler.scheduleOnce(1000 millis, self, "tick")
      val pipeline: HttpRequest => Future[HttpResponse] = sendReceive
      val response: Future[HttpResponse] = pipeline(Get("http://spray.io/"))
      log.debug(this.toString)
  }
}
