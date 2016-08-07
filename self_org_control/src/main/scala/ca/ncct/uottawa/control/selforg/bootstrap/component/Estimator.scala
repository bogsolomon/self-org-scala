package ca.ncct.uottawa.control.selforg.bootstrap.component

import akka.actor.{ActorLogging, Actor, Props}
import ca.ncct.uottawa.control.selforg.bootstrap.component.Estimator.CountType
import ca.ncct.uottawa.control.selforg.bootstrap.component.Estimator.CountType.CountType
import ca.ncct.uottawa.control.selforg.bootstrap.component.data.{EstimatedData, Model}
import ca.ncct.uottawa.control.selforg.bootstrap.config.GenericConfig

/**
  * Created by Bogdan on 8/2/2016.
  */
object Estimator {
  def props(config: GenericConfig): Props = Props(new Estimator(config))

  object CountType extends Enumeration {
    type CountType = Value
    val Add, Remove, Stable = Value
  }
}
class Estimator(config: GenericConfig) extends Actor with ActorLogging {

  val lowThreshold = config.params("LOWER_THRESHOLD").toDouble
  val highThreshold = config.params("HIGH_THRESHOLD").toDouble
  private var count:Int = 0
  private var countType:CountType = CountType.Stable

  override def receive  = stable

  def stable: Receive = {
    case msg : Model => msg.bucketLevel match {
      case x if x < lowThreshold => {
        countType = CountType.Remove
        count = 0
        context become remove
      }
      case x if x > highThreshold => {
        countType = CountType.Add
        count = 0
        context become add
      }
    }
  }

  def remove: Receive = {
    case msg : Model => msg.bucketLevel match {
      case x if x < lowThreshold => {
        count += 1
        sender ! EstimatedData(count, countType)
      }
      case x if x > highThreshold => {
        countType = CountType.Add
        count = 0
        context become add
      }
      case _ => context become stable
    }
  }

  def add: Receive = {
    case msg : Model => msg.bucketLevel match {
      case x if x < lowThreshold => {
        countType = CountType.Remove
        count = 0
        context become remove
      }
      case x if x > highThreshold => {
        count += 1
        sender ! EstimatedData(count, countType)
      }
      case _ => context become stable
    }
  }
}
