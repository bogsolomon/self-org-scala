package ca.ncct.uottawa.control.selforg.bootstrap.ants

import akka.actor.{Actor, ActorLogging, Address, Props, RootActorPath}
import akka.cluster.ClusterEvent.{MemberEvent, MemberRemoved, MemberUp, UnreachableMember}
import akka.cluster.pubsub.DistributedPubSub
import akka.cluster.pubsub.DistributedPubSubMediator.{Publish, Subscribe, SubscribeAck}
import akka.cluster.{Cluster, Member}
import ca.ncct.uottawa.control.selforg.bootstrap.ants.Ant.{MaxMorph, MinMorph, NoMorph}
import ca.ncct.uottawa.control.selforg.bootstrap.component.data.Model

import scala.collection.mutable
import scala.concurrent.duration._

/**
  * Created by Bogdan on 8/21/2016.
  */

object AntSystem {
  def props(instCount: Int, antSystemConfig: AntSystemConfig): Props = Props(new AntSystem(instCount, antSystemConfig))
}

class AntSystem(instCount: Int, antSystemConfig: AntSystemConfig) extends Actor with ActorLogging {

  case class AntJump(value: (Ant, Address))

  def PHEROMONE_DECAY = antSystemConfig.decayAmount
  def DECAY_RATE = antSystemConfig.decayRate

  var controlMembers = scala.collection.mutable.Map[Member, Metrics]()
  var model: Option[Model] = None
  var pheromoneLevel:Double = (antSystemConfig.maxMorphLevel + antSystemConfig.minMorphLevel) / 2
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
      } else {
        manager = member
        val ant = Ant(List(Tuple3(Cluster(context.system).selfAddress, 0, 0)), antSystemConfig, java.util.UUID.randomUUID.toString)
        log.info("Ant {} created for {}", ant, Cluster(context.system).selfAddress)
        self ! ant
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
      log.info("Received ant: {} by {}", ant, Cluster(context.system).selfAddress)
      var nextAddress: (Address, Double, Double) = null
      if (slaBreach) {
        log.info("SLA breach, ant going to manager")
        nextAddress = (manager.address, 0, 0)
        pheromoneLevel = (antSystemConfig.maxMorphLevel + antSystemConfig.minMorphLevel) / 2
      } else {
        val fuzzyFactor = model match {
          case Some(m) => m.bucketLevel
          case None => 0
        }
        nextAddress = ant.receive(Cluster(context.system).selfAddress, pheromoneLevel, fuzzyFactor, controlMembers.keySet.map(_.address).toList)
        pheromoneLevel = nextAddress._3
      }
      ants += ant
      log.info("Ant {} will jump to {} in {} seconds", ant, nextAddress._1, nextAddress._2.toLong)
      context.system.scheduler.scheduleOnce(nextAddress._2.toLong seconds, self, AntJump((ant, nextAddress._1)))
    }
    case modelRec:Model => {
      model = Some(modelRec)
    }
    case jump:AntJump => {
      context.actorSelection(RootActorPath(jump.value._2) / "user" / "antSystem") ! jump.value._1
      ants -= jump.value._1
      log.info("Ant {} jumped to {}", jump.value._1, jump.value._2);
    }
    case "decay" => {
      pheromoneLevel = (pheromoneLevel - PHEROMONE_DECAY) max 0
      context.system.scheduler.scheduleOnce(DECAY_RATE seconds, self, "decay")
      val maxAnts = ants.count(_.morphType == MaxMorph)
      val minAnts = ants.count(_.morphType == MinMorph)
      val noAnts = ants.count(_.morphType == NoMorph)
      log.info("Decaying maxAnts {} minAnts {} noAnts {} pheromone {}", maxAnts, minAnts, noAnts, pheromoneLevel);

      if (maxAnts > minAnts + noAnts || minAnts > maxAnts + noAnts) {
        slaBreach = true
      }
    }
    case SLABreach(breach:Boolean) => {
      log.info("Received SLA breach {}", breach)
      slaBreach = false
    }
    case SubscribeAck(Subscribe(topic, None, `self`)) =>
      log.info("subscribing to mediator");
  }
}

class Metrics {

}
