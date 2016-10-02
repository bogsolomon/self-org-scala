package ca.ncct.uottawa.control.selforg.manager.ants

import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import akka.cluster.ClusterEvent.{MemberEvent, MemberRemoved, MemberUp, UnreachableMember}
import akka.cluster.Member
import ca.ncct.uottawa.control.selforg.bootstrap.ants.Ant
import ca.ncct.uottawa.control.selforg.manager.common.{AddNode, RemoveNode}

import scala.collection.mutable.ListBuffer
import scala.util.Random

/**
  * Created by Bogdan on 2016-09-26.
  */

object AntSystem {
  def props(manager: ActorRef): Props = Props(new AntSystem(manager))
}

class AntSystem(manager: ActorRef) extends Actor with ActorLogging {

  var controlMembers = scala.collection.mutable.Map[Member, Metrics]()
  var activeAnts : ListBuffer[Ant] = new ListBuffer[Ant]
  var inactiveAnts : ListBuffer[HHAnt] = new ListBuffer[HHAnt]
  var controlMemberSize : Int = -1

  def THR = 0.1

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
    case ant:Ant => {
      log.info("Received ant: {}", ant)
      activeAnts += ant
      if (activeAnts.size + 1 == controlMembers.size) {
        // all ants received - optimize
        val newCount = houseHuntingOptimization
        if (newCount > controlMembers.size) {
          for (count <- 0 until newCount - controlMembers.size) {
            manager ! AddNode
          }
        } else {
          for (count <- 0 until controlMembers.size - newCount) {
            manager ! RemoveNode
            activeAnts.remove(0, controlMembers.size - newCount)
          }
        }
      }
    }
  }

  def houseHuntingOptimization: Int = {
    var initSolutions = new ListBuffer[HHAnt]
    val size = controlMemberSize max controlMembers.size
    // Round 1 - all ants are initialized with solutions
    activeAnts.zipWithIndex.map {case (s,i) => initSolutions += new HHAnt(s, size, i)}
    // Check suitabilities
    initSolutions.filter(_.solFitness < THR).map(inactiveAnts += _)
    initSolutions = initSolutions.filterNot(inactiveAnts.toSet)
    houseHuntingRound(initSolutions)
  }

  def houseHuntingRound(initSolutions: ListBuffer[HHAnt]): Int = {
    var prevNestCounts: Map[Int, Int] = Map()
    initSolutions.foreach(ant => prevNestCounts = prevNestCounts + (ant.nestId -> 1))
    // Round 2 - go home nest and recruit randomly
    val recruited: ListBuffer[(HHAnt, HHAnt)] = antRecruitment(initSolutions)
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

    if (nests.size == 1) {
      return nests.head._2(0).newCount
    }

    var newInitSolutions: ListBuffer[HHAnt] = ListBuffer()
    nests.foreach(nest => {
      if (nest._2.size < prevNestCounts(nest._1)) {
        // nest has decreased in size so it should be dropped. Filter out all ants going to it
        newInitSolutions = initSolutions.filterNot(ant => nest._2.contains(ant))
      }
    })
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
      recruited += Tuple2(initSolutions(0), HHAnt(initSolutions(0).ant, 0, -1))
    }

    recruited
  }
}

class Metrics {

}
