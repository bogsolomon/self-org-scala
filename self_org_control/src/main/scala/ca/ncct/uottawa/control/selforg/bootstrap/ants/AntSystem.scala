package ca.ncct.uottawa.control.selforg.bootstrap.ants

import akka.actor.{Address, RootActorPath, ActorLogging, Actor}
import akka.cluster.ClusterEvent.{MemberEvent, MemberRemoved, UnreachableMember, MemberUp}
import akka.cluster.{Cluster, Member}
import ca.ncct.uottawa.control.selforg.bootstrap.component.data.Model
import scala.concurrent.duration._

import scala.collection.mutable.ListBuffer

/**
  * Created by Bogdan on 8/21/2016.
  */


class AntSystem(instCount: Int) extends Actor with ActorLogging {

  var hasSentAnt = false
  var controlMembers = scala.collection.mutable.Map[Member, Metrics]()
  val isFirst = instCount == 0
  var model: Model
  var nextAddresses: scala.collection.mutable.Map[Address, Ant]

  override def receive = {
    case MemberUp(member) => {
      log.info("Member is Up: {} {}", member.address, member.roles)
      if (member.roles.contains("control")) {
        controlMembers += (member -> new Metrics)
        if (!isFirst && !hasSentAnt) {
          context.actorSelection(RootActorPath(member.address) / "user" / "antSystem") ! Ant
          hasSentAnt
        }
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
    case ant:Ant => {
      log.info("Received ant: {}", ant)
      val nextAddress = ant.receive(Cluster(context.system).selfAddress, model.bucketLevel)
      nextAddresses += nextAddress._1 -> ant
      log.info("Ant {} will jump to {} in {}", ant, nextAddress._1, nextAddress._2.toLong * 1000)
      context.system.scheduler.scheduleOnce(nextAddress._2.toLong seconds, self, nextAddress)
    }
    case modelRec:Model =>
      model = modelRec
    case jump:Pair[Address, Double] => {
      
    }
  }
}

class Metrics {

}