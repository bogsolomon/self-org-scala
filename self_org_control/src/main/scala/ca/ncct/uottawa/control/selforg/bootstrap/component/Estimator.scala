package ca.ncct.uottawa.control.selforg.bootstrap.component

import akka.actor.{ActorLogging, Actor, Props}
import ca.ncct.uottawa.control.selforg.bootstrap.component.Estimator.CountType
import ca.ncct.uottawa.control.selforg.bootstrap.component.Estimator.CountType.CountType
import ca.ncct.uottawa.control.selforg.bootstrap.component.data.Model
import ca.ncct.uottawa.control.selforg.bootstrap.config.EstimatorConfig

/**
  * Created by Bogdan on 8/2/2016.
  */
object Estimator {
  def props(config: EstimatorConfig): Props = Props(new Estimator(config))

  object CountType extends Enumeration {
    type CountType = Value
    val Add, Remove, Unav = Value
  }
}
class Estimator(config: EstimatorConfig) extends Actor with ActorLogging {

  private var count:Int = 0
  private var countType:CountType = CountType.Unav

  override def receive  = empty

  def empty: Receive = {
    case msg : Model => msg.bucketLevel match {
      case x if x < 0 => {
        countType = CountType.Remove
        context become remove
      }
    }
  }

  def remove: Receive = {
    case msg : Model
  }
}
