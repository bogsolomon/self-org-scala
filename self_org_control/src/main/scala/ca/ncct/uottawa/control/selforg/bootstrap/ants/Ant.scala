package ca.ncct.uottawa.control.selforg.bootstrap.ants

import akka.actor.Address
import ca.ncct.uottawa.control.selforg.bootstrap.ants.Ant.{MaxMorph, MinMorph, NoMorph, MorphType}

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

case class Ant(serverData: List[(Address, Int, Double)]) {

  def PHEROMONE_LEVEL = 10

  def WAIT_TIME = 15
  def HISTORY_SIZE = 5
  def MIN_MORPH = 3
  def MAX_MORPH = 10

  var history: ListBuffer[(Address, Double)] = new ListBuffer[(Address, Double)]
  var morphType : MorphType =  NoMorph

  private def calculatePheromone(fuzzyFactor: Double): Double = {
    (1 - fuzzyFactor) * PHEROMONE_LEVEL
  }

  private def updateTables(currentAddress: Address, newPheromoneValue: Double, fuzzyFactor: Double,
                           knownServers:List[Address]): List[(Address, Int, Double)] = {
    val rand:Random = new Random
    val newData = new ListBuffer[(Address, Int, Double)]
    val waitTime = (WAIT_TIME / (1 - fuzzyFactor)).min(60).ceil.toInt
    val maxWait = 0

    for (serverDatum <- serverData) {
      if (!serverDatum._1.eq(currentAddress)) {
        newData += Tuple3(serverDatum._1, serverDatum._2 + waitTime, serverDatum._3);
        maxWait.max(serverDatum._2)
      }
      else {
        newData += Tuple3(serverDatum._1, 0, serverDatum._3 + newPheromoneValue);
      }
    }

    val unknown = knownServers.filter(newData.map(_._1).contains(_))

    for (unknownServer <- unknown) {
      newData += Tuple3(unknownServer, rand.nextInt(maxWait) + waitTime, 0);
    }

    newData.toList
  }

  private def jumpNextNode(serverData: List[(Address, Int, Double)], currentAddress: Address,
                           knownServers:List[Address], newPheromoneValue: Double): (Address, Double, Double) = {
    val r = scala.util.Random
    var sumOfTimes = 0
    var sumOfPheromones:Double = 0
    var probTable = new ListBuffer[(Address, Double)]

    for (serverDatum <- serverData) {
      if (knownServers.contains(serverDatum._1)) {
        sumOfTimes += serverDatum._2
        sumOfPheromones += serverDatum._3
      }
    }

    for (serverDatum <- serverData) {
      if (knownServers.contains(serverDatum._1)) {
        probTable += Tuple2(serverDatum._1, (serverDatum._2 / sumOfTimes + serverDatum._3 / sumOfPheromones) / 2)
      }
    }
    probTable = probTable.sortWith(_._2 < _._2)
    val random = r.nextFloat
    var sumOfProbs:Double = 0

    for (prob <- probTable) {
      if (sumOfProbs + prob._2 < random ) {
        return Tuple3(prob._1, prob._2, newPheromoneValue)
      } else {
        sumOfProbs += prob._2
      }
    }

    Tuple3(probTable.last._1, probTable.last._2, newPheromoneValue)
  }

  def morph(): Unit = {
    val sum:Double = history.map(_._2).sum
    if ((sum / history.size) < MIN_MORPH) {
      morphType = MinMorph
    } else if  ((sum / history.size) > MAX_MORPH) {
      morphType = MaxMorph
    } else {
      morphType = NoMorph
    }
  }

  def receive(currentAddress: Address, fuzzyFactor: Double, knownServers:List[Address]): (Address, Double, Double) = {
    val newPheromone = calculatePheromone(fuzzyFactor)
    history += Tuple2(currentAddress, newPheromone)
    if (history.size > HISTORY_SIZE) {
      history.remove(0)
    }
    val newTable = updateTables(currentAddress, newPheromone, fuzzyFactor, knownServers)
    morph()
    jumpNextNode(newTable, currentAddress, knownServers, newPheromone)
  }
}
