package ca.ncct.uottawa.control.selforg.bootstrap.component

import akka.actor.{Actor, ActorLogging, Props}
import ca.ncct.uottawa.control.selforg.bootstrap.component.DecisionMaker.DecisionType
import ca.ncct.uottawa.control.selforg.bootstrap.component.DecisionMaker.DecisionType._
import ca.ncct.uottawa.control.selforg.bootstrap.component.data.Decision
import ca.ncct.uottawa.control.selforg.bootstrap.config.GenericConfig
import com.watchtogether.autonomic.selforg.red5.manager.group.GroupManager
import com.watchtogether.common.ClientPolicyMessage

/**
  * Created by Bogdan on 8/9/2016.
  */
object Actuator {
  def props(config: GenericConfig): Props = Props(new Actuator(config))
}
class Actuator(config: GenericConfig) extends Actor with ActorLogging {


  override def receive  = {
    case msg : Decision => actuate(msg.decision)
  }

  def actuate(decision: DecisionType): Unit = {
    decision match {
      case DecisionType.Accept => GroupManager.getManager.broadcastMessage(new ClientPolicyMessage(true))
      case DecisionType.Reject => GroupManager.getManager.broadcastMessage(new ClientPolicyMessage(false))
      case DecisionType.Nochange =>
    }
  }
}
