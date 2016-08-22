package ca.ncct.uottawa.control.selforg.bootstrap.ants

import akka.actor.{ActorLogging, Actor}
import akka.cluster.ClusterEvent.{MemberEvent, MemberRemoved, UnreachableMember, MemberUp}
import akka.cluster.Member

/**
  * Created by Bogdan on 8/21/2016.
  */


class ClusterMessageListener extends Actor with ActorLogging {

  var controlMembers = scala.collection.mutable.Map[Member, Metrics]()

  override def receive = {
    case MemberUp(member) => {
      log.info("Member is Up: {} {}", member.address, member.roles)
      if (member.roles.contains("control")) {
        controlMembers += (member -> new Metrics)
      }
    }
    case UnreachableMember(member) => {
      log.info("Member detected as unreachable: {}", member)
      controlMembers -= member
    }
    case MemberRemoved(member, previousStatus) => {
      log.info("Member is Removed: {} after {}",
        member.address, previousStatus)
      controlMembers -= member
    }
    case _: MemberEvent => // ignore
    log.info("Members: {}", controlMembers)
  }
}

class Metrics {

}
