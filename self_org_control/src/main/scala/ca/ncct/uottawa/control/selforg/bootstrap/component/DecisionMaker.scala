package ca.ncct.uottawa.control.selforg.bootstrap.component

import akka.actor.{ActorLogging, Actor, Props}
import ca.ncct.uottawa.control.selforg.bootstrap.component.DecisionMaker.DecisionType
import ca.ncct.uottawa.control.selforg.bootstrap.component.DecisionMaker.DecisionType.DecisionType
import ca.ncct.uottawa.control.selforg.bootstrap.component.Estimator.CountType
import ca.ncct.uottawa.control.selforg.bootstrap.component.Estimator.CountType._
import ca.ncct.uottawa.control.selforg.bootstrap.component.data.{Decision, EstimatedData}
import ca.ncct.uottawa.control.selforg.bootstrap.config.GenericConfig

/**
  * Created by Bogdan on 8/6/2016.
  */
object DecisionMaker {
  def props(config: GenericConfig): Props = Props(new DecisionMaker(config))

  object DecisionType extends Enumeration {
    type DecisionType = Value
    val Accept, Reject, Nochange = Value
  }
}

class DecisionMaker(config: GenericConfig) extends Actor with ActorLogging {

  var lastDecision:DecisionType = DecisionType.Nochange
  val lowThreshold = config.params("LOWER_THRESHOLD_CHANGE_COUNT").toDouble
  val highThreshold = config.params("HIGH_THRESHOLD_CHANGE_COUNT").toDouble

  override def receive  = {
    case msg : EstimatedData => makeDecision(msg.count, msg.countType)
  }

  def makeDecision(count: Int, countType: CountType): Unit = {
    if (count > lowThreshold && countType == CountType.Remove && lastDecision != DecisionType.Reject) {
      lastDecision = DecisionType.Reject
      sender ! Decision(lastDecision)
    } else if (count > highThreshold && countType == CountType.Add && lastDecision != DecisionType.Accept) {
      lastDecision = DecisionType.Reject
      sender ! Decision(lastDecision)
    } else {
      sender ! Decision(DecisionType.Nochange)
    }
  }
}
