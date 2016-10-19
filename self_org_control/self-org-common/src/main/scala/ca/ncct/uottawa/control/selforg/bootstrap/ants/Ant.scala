package ca.ncct.uottawa.control.selforg.bootstrap.ants

import akka.actor.Address
import ca.ncct.uottawa.control.selforg.bootstrap.ants.Ant.{MaxMorph, MinMorph, MorphType, NoMorph}

import scala.collection.mutable.ListBuffer
import scala.util.Random

/**
  * Created by Bogdan on 9/8/2016.
  */
object Ant {
  sealed trait MorphType
  case object MaxMorph extends MorphType
  case object MinMorph extends MorphType
  case object NoMorph extends MorphType
}

case class SLABreach(breach:Boolean)

case class Ant(var serverData: List[(Address, Int, Double)], config: AntSystemConfig) {

  def PHEROMONE_LEVEL = config.antPheromone

  def WAIT_TIME = config.antWaitTime
  def HISTORY_SIZE = config.antHistorySize
  def MIN_MORPH = config.minMorphLevel
  def MAX_MORPH = config.maxMorphLevel

  var history: ListBuffer[(Address, Double)] = new ListBuffer[(Address, Double)]
  var morphType : MorphType =  NoMorph

  private def calculatePheromone(fuzzyFactor: Double): Double = {
    (1 - fuzzyFactor) * PHEROMONE_LEVEL
  }

  private def updateTables(currentAddress: Address, newPheromoneValue: Double, fuzzyFactor: Double,
                           knownServers:List[Address]): (List[(Address, Int, Double)], Int) = {
    val rand:Random = new Random
    val newData = new ListBuffer[(Address, Int, Double)]
    val waitTime = (WAIT_TIME / (1 - fuzzyFactor)).min(60).ceil.toInt
    val maxWait = 0

    serverData.foreach(serverDatum => {
      if (!serverDatum._1.eq(currentAddress)) {
        newData += Tuple3(serverDatum._1, serverDatum._2 + waitTime, serverDatum._3);
        maxWait.max(serverDatum._2)
      }
      else {
        newData += Tuple3(serverDatum._1, 0, newPheromoneValue);
      }
    })

    val newDataServers: Set[Address] = newData.map(_._1).toSet
    val unknown = knownServers.filterNot(newDataServers)

    for (unknownServer <- unknown) {
      newData += Tuple3(unknownServer, rand.nextInt(maxWait) + waitTime, 0);
    }

    Tuple2(newData.toList, waitTime)
  }

  private def jumpNextNode(serverData: (List[(Address, Int, Double)], Int), currentAddress: Address,
                           knownServers:List[Address], newPheromoneValue: Double): (Address, Double, Double) = {
    val r = scala.util.Random
    var sumOfTimes = 0
    var sumOfPheromones:Double = 0
    var probTable = new ListBuffer[(Address, Double)]

    if (knownServers.length == 1) {
      this.serverData = serverData._1
      return Tuple3(currentAddress, serverData._2, newPheromoneValue)
    }

    for (serverDatum <- serverData._1) {
      if (knownServers.contains(serverDatum._1)) {
        sumOfTimes += serverDatum._2
        sumOfPheromones += serverDatum._3
      }
    }

    for (serverDatum <- serverData._1) {
      if (knownServers.contains(serverDatum._1)) {
        probTable += Tuple2(serverDatum._1, (serverDatum._2 / sumOfTimes + serverDatum._3 / sumOfPheromones) / 2)
      }
    }
    probTable += Tuple2(currentAddress, (newPheromoneValue / sumOfPheromones) / 2)

    probTable = probTable.sortWith(_._2 < _._2)
    val random = r.nextFloat
    var sumOfProbs:Double = 0

    for (prob <- probTable) {
      if (sumOfProbs + prob._2 < random ) {
        return Tuple3(prob._1, serverData._2, newPheromoneValue)
      } else {
        sumOfProbs += prob._2
      }
    }

    this.serverData = serverData._1
    Tuple3(probTable.last._1, serverData._2, newPheromoneValue)
  }

  def morph(): Unit = {
    val sum:Double = history.map(_._2).sum
    if ((sum / history.size) < MIN_MORPH) {
      morphType = MaxMorph
    } else if  ((sum / history.size) > MAX_MORPH) {
      morphType = MinMorph
    } else {
      morphType = NoMorph
    }
  }

  def receive(currentAddress: Address, pLevel:Double, fuzzyFactor: Double, knownServers:List[Address]): (Address, Double, Double) = {
    val newPheromone = calculatePheromone(fuzzyFactor)
    history += Tuple2(currentAddress, newPheromone + pLevel)
    if (history.size > HISTORY_SIZE) {
      history.remove(0)
    }
    val newTable = updateTables(currentAddress, newPheromone + pLevel, fuzzyFactor, knownServers)
    morph()
    jumpNextNode(newTable, currentAddress, knownServers, newPheromone + pLevel)
  }
}
