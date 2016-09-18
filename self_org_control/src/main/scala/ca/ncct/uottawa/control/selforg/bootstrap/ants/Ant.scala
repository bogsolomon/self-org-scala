package ca.ncct.uottawa.control.selforg.bootstrap.ants

import akka.actor.Address

import scala.collection.mutable.ListBuffer
import scala.util.Random

/**
  * Created by Bogdan on 9/8/2016.
  */
case class Ant(serverData: List[Triple[Address, Int, Double]]) {
  def PHEROMONE_LEVEL = 10

  def WAIT_TIME = 15

  private def calculatePheromone(fuzzyFactor: Double): Double = {
    (1 - fuzzyFactor) * PHEROMONE_LEVEL
  }

  private def updateTables(currentAddress: Address, newPheromoneValue: Double, fuzzyFactor: Double,
                           knownServers:List[Address]): List[Triple[Address, Int, Double]] = {
    val rand:Random = new Random
    val newData = new ListBuffer[Triple[Address, Int, Double]]
    val waitTime = (WAIT_TIME / (1 - fuzzyFactor)).min(60).ceil.toInt
    val maxWait = 0

    for (serverDatum <- serverData) {
      if (!serverDatum._1.eq(currentAddress)) {
        newData += Triple(serverDatum._1, serverDatum._2 + waitTime, serverDatum._3);
        maxWait.max(serverDatum._2)
      }
      else {
        newData += Triple(serverDatum._1, 0, serverDatum._3 + newPheromoneValue);
      }
    }

    val unknown = knownServers.filter(newData.map(_._1).contains(_))

    for (unknownServer <- unknown) {
      newData += Triple(unknownServer, rand.nextInt(maxWait) + waitTime, 0);
    }

    newData.toList
  }

  private def jumpNextNode(serverData: List[Triple[Address, Int, Double]], currentAddress: Address,
                           knownServers:List[Address], newPheromoneValue: Double): Triple[Address, Double, Double] = {
    val r = scala.util.Random
    var sumOfTimes = 0
    var sumOfPheromones:Double = 0
    var probTable = new ListBuffer[Pair[Address, Double]]

    for (serverDatum <- serverData) {
      if (knownServers.contains(serverDatum._1)) {
        sumOfTimes += serverDatum._2
        sumOfPheromones += serverDatum._3
      }
    }

    for (serverDatum <- serverData) {
      if (knownServers.contains(serverDatum._1)) {
        probTable += Pair(serverDatum._1, (serverDatum._2 / sumOfTimes + serverDatum._3 / sumOfPheromones) / 2)
      }
    }
    probTable = probTable.sortWith(_._2 < _._2)
    val random = r.nextFloat
    var sumOfProbs:Double = 0

    for (prob <- probTable) {
      if (sumOfProbs + prob._2 < random ) {
        return Triple(prob._1, prob._2, newPheromoneValue)
      } else {
        sumOfProbs += prob._2
      }
    }

    Triple(probTable.last._1, probTable.last._2, newPheromoneValue)
  }

  def receive(currentAddress: Address, fuzzyFactor: Double, knownServers:List[Address]): Triple[Address, Double, Double] = {
    val newPheromone = calculatePheromone(fuzzyFactor)
    val newTable = updateTables(currentAddress, newPheromone, fuzzyFactor, knownServers)
    jumpNextNode(newTable, currentAddress, knownServers, newPheromone)
  }
}
