package ca.ncct.uottawa.control.selforg.bootstrap.ants

import akka.cluster.protobuf.msg.ClusterMessages.Address

import scala.collection.mutable.ListBuffer

/**
  * Created by Bogdan on 9/8/2016.
  */
case class Ant(servers: List[Address], lastVisited: List[Int], pheromoneLevel: List[Double]) {
  def calculatePheromone(currentAddress: Address): Double = {
    0
  }

  def updateTables(currentAddress: Address, newPheromoneValue: Double, waitTime: Int): Triple[List[Address], List[Int], List[Double]] = {
    val newServers = new ListBuffer[Address]
    val newLastVisited = new ListBuffer[Int]
    val newPheromoneLevel = new ListBuffer[Double]
    val visitedIter = lastVisited.iterator
    val pheromoneIter = pheromoneLevel.iterator

    for (address <- servers) {
      if (!address.eq(currentAddress)) {
        newServers += address
        newLastVisited += (visitedIter.next + waitTime)
        newPheromoneLevel += pheromoneIter.next
      }
      else {
        newServers += address
        newLastVisited += 0
        newPheromoneLevel += newPheromoneValue
      }
    }

    Triple(newServers.toList, newLastVisited.toList, newPheromoneLevel.toList)
  }

  def jumpNextNode(servers: List[Address], lastVisited: List[Int], pheromoneLevel: List[Double]): Pair[Address, Double] = {
    Pair(servers.head, 15)
  }

  def receive(currentAddress: Address): Pair[Address, Double] = {
    val newPheromone = calculatePheromone(currentAddress)
    val newTable = updateTables(currentAddress, newPheromone, 10)
    return jumpNextNode(newTable._1, newTable._2, newTable._3)
  }
}
