package ca.ncct.uottawa.control.selforg.manager.ants

import akka.actor.Address
import ca.ncct.uottawa.control.selforg.bootstrap.ants.Ant
import ca.ncct.uottawa.control.selforg.bootstrap.ants.Ant.MaxMorph

import scala.collection.mutable.ListBuffer
import scala.util.Random

/**
  * Created by Bogdan on 2016-09-29.
  */
class HHAnt(ant: Ant, servCount: Int) {

  var simSolution: ListBuffer[Double] = new ListBuffer[Double]

  def initSolution: Unit = {
    val newCount: Int =
      if (ant.morphType == MaxMorph) {
        servCount + Random.nextInt(servCount)
      } else {
        (servCount - Random.nextInt(servCount)) max 1
      }

    val servFrac: Double =
      if (ant.morphType == MaxMorph) {
        (newCount - servCount) / servCount
      } else {
        newCount / servCount
      }

    val change : Int = (servFrac * ant.history.size).toInt

    if (ant.morphType == MaxMorph) {
      for (i <- 0 to change) {
        simSolution += 0
      }
    }
  }

}
