package ca.ncct.uottawa.control.selforg.manager.ants

import akka.actor.{Actor, ActorLogging, ActorRef, ActorSystem, Address, Props, RootActorPath}
import akka.cluster.ClusterEvent.{MemberEvent, MemberRemoved, MemberUp, UnreachableMember}
import akka.cluster.{Cluster, Member}
import akka.cluster.pubsub.DistributedPubSub
import akka.cluster.pubsub.DistributedPubSubMediator.{Publish, Subscribe, SubscribeAck}
import ca.ncct.uottawa.control.selforg.bootstrap.ants.{Ant, ControllerRegister, SLABreach}
import ca.ncct.uottawa.control.selforg.bootstrap.ants.Ant.NoMorph
import ca.ncct.uottawa.control.selforg.manager.common.{AddNode, RemoveNode}
import ca.ncct.uottawa.control.selforg.manager.config.AntSystemConfig

import scala.concurrent.duration._
import scala.collection.mutable.ListBuffer
import scala.util.Random

/**
  * Created by Bogdan on 2016-09-26.
  */

object AntSystem {
  def props(manager: ActorRef, antSystemConfig: AntSystemConfig, system: ActorSystem): Props = Props(new AntSystem(manager, antSystemConfig, system))
}

class AntSystem(manager: ActorRef, antSystemConfig: AntSystemConfig, system: ActorSystem) extends Actor with ActorLogging {

  var controlMembers = new scala.collection.mutable.ListBuffer[Member]()
  var controlMemberAddresses = scala.collection.mutable.Map[String, Address]()
  var activeAnts : ListBuffer[Ant] = new ListBuffer[Ant]
  var inactiveAnts : ListBuffer[HHAnt] = new ListBuffer[HHAnt]
  var deltaServerCount = 0
  val random:Random = Random
  val cluster = Cluster(system)

  val mediator = DistributedPubSub(context.system).mediator
  val topic = "antSubsystem"
  mediator ! Subscribe(topic, self)

  def THR = antSystemConfig.solutionFitnessThr

  import scala.concurrent.ExecutionContext.Implicits.global

  override def receive = {
    case MemberUp(member) => {
      log.info("Member is Up: {} {}", member.address, member.roles)
      if (member.roles.contains("control")) {
        controlMembers += member
      }
    }
    case UnreachableMember(member) => {
      log.info("Member detected as unreachable: {}", member)
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
      activeAnts += ant
      if (activeAnts.size == controlMembers.length) {
        log.info("Sending SLA Breach");
        mediator ! Publish("antSubsystem", SLABreach(true))
        log.info("Received all ants, starting optimization")
        // all ants received - optimize
        val newCount = houseHuntingOptimization
        deltaServerCount = math.abs(newCount - controlMembers.size)
        log.info("Delta servers: {}", deltaServerCount)
        val clusterSize = controlMembers.length
        if (newCount == controlMembers.length) {
          log.info("No change")
          context.system.scheduler.scheduleOnce(1 minute, self, "tick")
        } else if (newCount > controlMembers.length) {
          for (count <- 0 until deltaServerCount) {
            val instName = "red5-"+(clusterSize + count)
            val controllName = "red5-control-"+(clusterSize + count)
            manager ! AddNode(instName, controllName)
          }
        } else {
          for (count <- 0 until deltaServerCount) {
            val instName = "red5-"+(clusterSize - 1 - count)
            val controllName = "red5-control-"+(clusterSize - 1 - count)
            cluster.leave(controlMemberAddresses(instName))
            controlMemberAddresses -= instName
            manager ! RemoveNode(instName, controllName)
          }
          activeAnts.remove(0, deltaServerCount)
        }
      }
    }
    case AddNode | RemoveNode => {
      log.info("Server added/removed")
      deltaServerCount = deltaServerCount - 1
      if (deltaServerCount == 0) {
        log.info("All Servers added/removed")
        context.system.scheduler.scheduleOnce(1 minute, self, "tick")
      }
    }
    case "tick" => {
      for (activeAnt <- activeAnts) {
        log.info("Restarting ants")
        activeAnt.history = ListBuffer()
        activeAnt.morphType = NoMorph

        val destination: Int = if (controlMembers.size == 1) 0 else random.nextInt(controlMembers.size - 1)
        val member = controlMembers(destination)
        activeAnt.serverData = List(Tuple3(member.address, 0, 0))
        context.actorSelection(RootActorPath(member.address) / "user" / "antSystem") ! activeAnt
      }
      activeAnts.clear()
    }
    case SLABreach => {
      log.info("Received SLA breach detected")
    }
    case SubscribeAck(Subscribe(topic, None, `self`)) =>
      log.info("subscribing to mediator");
    case ControllerRegister(managedHost, address) => {
      controlMemberAddresses += (managedHost -> address)
    }
  }

  def houseHuntingOptimization: Int = {
    var initSolutions = new ListBuffer[HHAnt]
    val size = controlMembers.size
    // Round 1 - all ants are initialized with solutions
    activeAnts.zipWithIndex.map {case (s,i) => initSolutions += new HHAnt(s, size, i)}
    log.info("Initial solutions {}", initSolutions)
    if (initSolutions.size == 1) {
      return initSolutions.head.newCount
    }
    // Check suitabilities
    initSolutions.filter(_.solFitness < THR).map(inactiveAnts += _)
    initSolutions = initSolutions.filterNot(inactiveAnts.toSet)
    log.info("Initial solutions with good fitness {}", initSolutions)
    houseHuntingRound(initSolutions)
  }

  def houseHuntingRound(initSolutions: ListBuffer[HHAnt]): Int = {
    var prevNestCounts: Map[Int, Int] = Map()
    initSolutions.foreach(ant => prevNestCounts = prevNestCounts + (ant.nestId -> 1))
    log.info("Round 2 previous nests {}", prevNestCounts)
    // Round 2 - go home nest and recruit randomly
    val recruited: ListBuffer[(HHAnt, HHAnt)] = antRecruitment(initSolutions)
    log.info("Round 2 recruitment {}", recruited)
    // Round 3 - Ants go to new nest and check nests which increased/decreased
    var nests: Map[Int, List[HHAnt]] = Map()
    recruited.foreach(ant => {
      var prevList: List[HHAnt] = nests.getOrElse(ant._1.nestId, Nil)
      prevList = ant._1 :: prevList
      if (ant._2.nestCount != -1) {
        prevList = ant._2 :: prevList
      }
      nests = nests + (ant._1.nestId -> prevList)
    })
    log.info("Round 3 nests {}", nests)

    if (nests.size == 1) {
      return nests.head._2.head.newCount
    }

    var newInitSolutions = ListBuffer[HHAnt]()
    nests.foreach( x => {x._2.foreach( y=> {newInitSolutions += y}) })
    log.info("Round 4 newInitSolutions before filtering {}", newInitSolutions)
    nests.foreach(nest => {
      if (nest._2.size < prevNestCounts(nest._1)) {
        log.info("Nest  {} is dropping out with {} ants", nest._1, nest._2.size)
        // nest has decreased in size so it should be dropped. Filter out all ants going to it
        newInitSolutions = newInitSolutions.filterNot(ant => nest._2.contains(ant))
      }
    })
    log.info("Round 4 newInitSolutions {}", newInitSolutions)
    houseHuntingRound(newInitSolutions)
  }

  def antRecruitment(initSolutions: ListBuffer[HHAnt]): ListBuffer[(HHAnt, HHAnt)] = {
    val recruited:ListBuffer[(HHAnt, HHAnt)] = new ListBuffer[(HHAnt, HHAnt)]

    while (initSolutions.size > 1) {
      val ant1 = initSolutions.remove(Random.nextInt(initSolutions.size))
      val ant2 = initSolutions.remove(Random.nextInt(initSolutions.size))
      ant2.nestId = ant1.nestId
      recruited += Tuple2(ant1, ant2)
    }

    if (initSolutions.nonEmpty) {
      recruited += Tuple2(initSolutions.head, HHAnt(initSolutions.head.ant, initSolutions.head.servCount, initSolutions.head.nestId))
    }

    recruited
  }
}

class Metrics {

}
