package ca.ncct.uottawa.control.selforg.bootstrap.component

import akka.actor.{ActorLogging, Actor, Props}
import ca.ncct.uottawa.control.selforg.bootstrap.component.Estimator.{Add, Remove, Stable, CountType}
import ca.ncct.uottawa.control.selforg.bootstrap.component.data.{EstimatedData, Model}
import ca.ncct.uottawa.control.selforg.bootstrap.config.GenericConfig

/**
  * Created by Bogdan on 8/2/2016.
  */
object Estimator {
  def props(config: GenericConfig): Props = Props(new Estimator(config))

  sealed trait CountType
  case object Add extends CountType
  case object Remove extends CountType
  case object Stable extends CountType
}
class Estimator(config: GenericConfig) extends Actor with ActorLogging {

  val lowThreshold = config.params("LOWER_THRESHOLD").toDouble
  val highThreshold = config.params("HIGH_THRESHOLD").toDouble
  private var count:Int = 0
  private var countType:CountType = Stable

  override def receive  = stable

  def stable: Receive = {
    case msg : Model => msg.bucketLevel match {
      case x if x < lowThreshold =>
        log.debug("From stable -> remove")
        countType = Remove
        count = 0
        context become remove
      case y if y > highThreshold =>
        log.debug("From stable -> add")
        countType = Add
        count = 0
        context become add
      case _ =>
        log.debug("From stable -> stable")
        countType = Stable
    }
  }

  def remove: Receive = {
    case msg : Model => msg.bucketLevel match {
      case x if x < lowThreshold => {
        count += 1
        log.debug("From remove -> remove " + count)
        sender ! EstimatedData(count, countType)
      }
      case y if y > highThreshold => {
        countType = Add
        count = 0
        log.debug("From remove -> add " + count)
        context become add
      }
      case _ =>
        log.debug("From remove -> stable ")
        context become stable
    }
  }

  def add: Receive = {
    case msg : Model => msg.bucketLevel match {
      case x if x < lowThreshold => {
        countType = Remove
        count = 0
        log.debug("From add -> remove " + count)
        context become remove
      }
      case y if y > highThreshold => {
        count += 1
        log.debug("From add -> add " + count)
        sender ! EstimatedData(count, countType)
      }
      case _ =>
        log.debug("From add -> stable ")
        context become stable
    }
  }
}
