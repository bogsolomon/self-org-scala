package ca.ncct.uottawa.control.selforg.bootstrap.ants

import akka.actor.Address

import scala.collection.mutable.ListBuffer

/**
  * Created by Bogdan on 9/8/2016.
  */
case class Ant(serverData: List[Triple[Address, Int, Double]]) {
  def PHEROMONE_LEVEL = 10

  def WAIT_TIME = 15

  private def calculatePheromone(fuzzyFactor: Double): Double = {
    (1 - fuzzyFactor) * PHEROMONE_LEVEL
  }

  private def updateTables(currentAddress: Address, newPheromoneValue: Double, fuzzyFactor: Double): List[Triple[Address, Int, Double]] = {
    val newData = new ListBuffer[Triple[Address, Int, Double]]
    val waitTime = (WAIT_TIME / (1 - fuzzyFactor)).min(60).ceil.toInt

    for (serverDatum <- serverData) {
      if (!serverDatum._1.eq(currentAddress)) {
        newData += Triple(serverDatum._1, serverDatum._2 + waitTime, serverDatum._3);
      }
      else {
        newData += Triple(serverDatum._1, 0, serverDatum._3 + newPheromoneValue);
      }
    }

    newData.toList
  }

  private def jumpNextNode(serverData: List[Triple[Address, Int, Double]], currentAddress: Address): Pair[Address, Double] = {
    val r = scala.util.Random
    var sumOfTimes = 0
    var sumOfPheromones:Double = 0
    var probTable = new ListBuffer[Pair[Address, Double]]

    for (serverDatum <- serverData) {
      if (!serverDatum._1.eq(currentAddress)) {
        sumOfTimes += serverDatum._2
        sumOfPheromones += serverDatum._3
      }
    }

    for (serverDatum <- serverData) {
      if (!serverDatum._1.eq(currentAddress)) {
        probTable += Pair(serverDatum._1, (serverDatum._2 / sumOfTimes + serverDatum._3 / sumOfPheromones) / 2)
      }
    }
    probTable = probTable.sortWith(_._2 < _._2)
    val random = r.nextFloat
    var sumOfProbs:Double = 0

    for (prob <- probTable) {
      if (sumOfProbs + prob._2 < random ) {
        return prob
      } else {
        sumOfProbs += prob._2
      }
    }

    probTable.last
  }

  def receive(currentAddress: Address, fuzzyFactor: Double): Pair[Address, Double] = {
    val newPheromone = calculatePheromone(fuzzyFactor)
    val newTable = updateTables(currentAddress, newPheromone, fuzzyFactor)
    jumpNextNode(newTable, currentAddress)
  }
}
