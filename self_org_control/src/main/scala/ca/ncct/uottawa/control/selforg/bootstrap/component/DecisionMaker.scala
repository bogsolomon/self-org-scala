package ca.ncct.uottawa.control.selforg.bootstrap.component

import akka.actor.{ActorLogging, Actor, Props}
import ca.ncct.uottawa.control.selforg.bootstrap.component.DecisionMaker.{Accept, Reject, Nochange, DecisionType}
import ca.ncct.uottawa.control.selforg.bootstrap.component.Estimator.{Add, Remove, CountType}
import ca.ncct.uottawa.control.selforg.bootstrap.component.data.{Decision, EstimatedData}
import ca.ncct.uottawa.control.selforg.bootstrap.config.GenericConfig

/**
  * Created by Bogdan on 8/6/2016.
  */
object DecisionMaker {
  def props(config: GenericConfig): Props = Props(new DecisionMaker(config))

  sealed trait DecisionType
  case object Accept extends DecisionType
  case object Reject extends DecisionType
  case object Nochange extends DecisionType
}

class DecisionMaker(config: GenericConfig) extends Actor with ActorLogging {

  var lastDecision:DecisionType = Nochange
  val lowThreshold = config.params("LOWER_THRESHOLD_CHANGE_COUNT").toDouble
  val highThreshold = config.params("HIGH_THRESHOLD_CHANGE_COUNT").toDouble

  override def receive  = {
    case msg : EstimatedData => makeDecision(msg.count, msg.countType)
  }

  def makeDecision(count: Int, countType: CountType): Unit = {
    if (count > lowThreshold && countType == Remove && lastDecision != Accept) {
      lastDecision = Accept
      sender ! Decision(lastDecision)
    } else if (count > highThreshold && countType == Add && lastDecision != Reject) {
      lastDecision = Reject
      sender ! Decision(lastDecision)
    } else {
      sender ! Decision(Nochange)
    }
  }
}
