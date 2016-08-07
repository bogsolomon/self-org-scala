package ca.ncct.uottawa.control.selforg.bootstrap.component

import akka.actor.{ActorLogging, Actor, Props, ActorRef}
import ca.ncct.uottawa.control.selforg.bootstrap.component.data.{Decision, EstimatedData, Model, FilterMeasurement}
import ca.ncct.uottawa.control.selforg.bootstrap.config.GenericConfig

/**
  * Created by Bogdan on 7/31/2016.
  */
object Coordinator {
  def props(config: GenericConfig, model: ActorRef, estimator: ActorRef, dm: ActorRef): Props = Props(new Coordinator(config, model, estimator, dm))
}
class Coordinator(config: GenericConfig, model: ActorRef, estimator: ActorRef, dm: ActorRef) extends Actor with ActorLogging {

  override def receive  = {
    case msg : FilterMeasurement => coordinate(msg)
    case msg : Model => coordinate(msg)
    case msg : EstimatedData => coordinate(msg)
    case msg : Decision => coordinate(msg)
  }

  def coordinate(msg: FilterMeasurement): Unit = {
    log.debug("coordinate start")

    model ! msg
  }

  def coordinate(msg: Model): Unit = {
    log.debug("coordinate model update: " + msg)

    estimator ! msg
  }

  def coordinate(msg: EstimatedData): Unit = {
    log.debug("coordinate estimated date: " + msg)

    dm ! msg
  }

  def coordinate(msg: Decision): Unit = {
    log.debug("coordinate decision: " + msg)
  }
}
