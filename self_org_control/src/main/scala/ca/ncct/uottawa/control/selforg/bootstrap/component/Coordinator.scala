package ca.ncct.uottawa.control.selforg.bootstrap.component

import akka.actor.{ActorLogging, Actor, Props, ActorRef}
import ca.ncct.uottawa.control.selforg.bootstrap.component.data.{Model, FilterMeasurement}
import ca.ncct.uottawa.control.selforg.bootstrap.config.CoordinatorConfig

/**
  * Created by Bogdan on 7/31/2016.
  */
object Coordinator {
  def props(config: CoordinatorConfig, model: ActorRef): Props = Props(new Coordinator(config, model))
}
class Coordinator(config: CoordinatorConfig, model: ActorRef) extends Actor with ActorLogging {

  override def receive  = {
    case msg : FilterMeasurement => coordinate(msg)
    case msg : Model => coordinate(msg)
  }

  def coordinate(msg: FilterMeasurement): Unit = {
    log.debug("coordinate start")

    model ! msg
  }

  def coordinate(msg: Model): Unit = {
    log.debug("coordinate model update: " + msg)
  }
}
