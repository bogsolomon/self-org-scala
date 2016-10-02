package ca.ncct.uottawa.control.selforg.bootstrap.ants

import akka.actor.{Actor, ActorLogging, ActorRef, Address, Props, RootActorPath}
import akka.cluster.ClusterEvent.{MemberEvent, MemberRemoved, MemberUp, UnreachableMember}
import akka.cluster.pubsub.DistributedPubSub
import akka.cluster.pubsub.DistributedPubSubMediator.Subscribe
import akka.cluster.{Cluster, Member}
import ca.ncct.uottawa.control.selforg.bootstrap.ants.Ant.{MaxMorph, MinMorph, NoMorph}
import ca.ncct.uottawa.control.selforg.bootstrap.component.data.Model

import scala.collection.mutable
import scala.concurrent.duration._

/**
  * Created by Bogdan on 8/21/2016.
  */

object AntSystem {
  def props(instCount: Int): Props = Props(new AntSystem(instCount))
}

class AntSystem(instCount: Int) extends Actor with ActorLogging {

  case class SLABreach()
  case class AntJump(value: (Ant, Address))

  def PHEROMONE_DECAY = 10
  def DECAY_RATE = 15

  var hasSentAnt = false
  var controlMembers = scala.collection.mutable.Map[Member, Metrics]()
  val isFirst = instCount == 0
  var model: Model = null
  var pheromoneLevel:Double = 0
  var ants : mutable.LinkedHashSet[Ant] = new mutable.LinkedHashSet[Ant]
  var manager: Member = null
  var slaBreach: Boolean = false

  import scala.concurrent.ExecutionContext.Implicits.global

  val mediator = DistributedPubSub(context.system).mediator
  val topic = "antSubsystem"
  mediator ! Subscribe(topic, self)

  override def preStart() = {
    context.system.scheduler.scheduleOnce(DECAY_RATE seconds, self, "decay")
  }

  override def receive = {
    case MemberUp(member) => {
      log.info("Member is Up: {} {}", member.address, member.roles)
      if (member.roles.contains("control")) {
        controlMembers += (member -> new Metrics)
        if (!isFirst && !hasSentAnt) {
          context.actorSelection(RootActorPath(member.address) / "user" / "antSystem") ! Ant(List(Triple(member.address, 0, 0)))
          hasSentAnt = true
          log.info("Sent initial ant")
        }
      } else {
        manager = member
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
      var nextAddress: (Address, Double, Double) = null
      if (slaBreach) {
        log.info("SLA breach, ant going to manager")
        nextAddress = (manager.address, 0, 0)
      } else {
        nextAddress = ant.receive(Cluster(context.system).selfAddress, model.bucketLevel, controlMembers.keySet.map(_.address).toList)
      }
      log.info("Ant {} will jump to {} in {} seconds", ant, nextAddress._1, nextAddress._2.toLong)
      context.system.scheduler.scheduleOnce(nextAddress._2.toLong seconds, self, AntJump((ant, nextAddress._1)))
      pheromoneLevel += nextAddress._3
      ants += ant
    }
    case modelRec:Model => {
      model = modelRec
    }
    case jump:AntJump => {
      context.actorSelection(RootActorPath(jump.value._2) / "user" / "antSystem") ! jump.value._1
      log.info("Ant {} jumped to {}", jump.value._1, jump.value._2);
    }
    case "decay" => {
      pheromoneLevel = (pheromoneLevel + PHEROMONE_DECAY) max 0
      context.system.scheduler.scheduleOnce(DECAY_RATE seconds, self, "decay")
      val maxAnts = ants.count(_.morphType == MaxMorph)
      val minAnts = ants.count(_.morphType == MinMorph)
      val noAnts = ants.count(_.morphType == NoMorph)

      if (maxAnts * 2 > minAnts + noAnts || minAnts * 2 > maxAnts + noAnts) {
        slaBreach = true
        mediator ! SLABreach
      }
    }
    case SLABreach => {
      log.info("Received SLA breach detected")
      slaBreach = true
    }
  }
}

class Metrics {

}
