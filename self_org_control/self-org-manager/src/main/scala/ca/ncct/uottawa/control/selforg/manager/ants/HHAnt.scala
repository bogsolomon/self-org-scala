package ca.ncct.uottawa.control.selforg.manager.ants

import ca.ncct.uottawa.control.selforg.bootstrap.ants.Ant
import ca.ncct.uottawa.control.selforg.bootstrap.ants.Ant.MaxMorph

import scala.collection.mutable.ListBuffer
import scala.util.Random

/**
  * Created by Bogdan on 2016-09-29.
  */
case class HHAnt(ant: Ant, servCount: Int, nestCount: Int) {

  val random:Random = Random
  var simSolution: ListBuffer[Double] = new ListBuffer[Double]
  val newCount: Int =
    if (ant.morphType == MaxMorph) {
      servCount + 1 + random.nextInt(servCount)
    } else {
      (servCount - 1 - random.nextInt(servCount)) max 1
    }
  val solFitness = initSolution

  private var _nestId = nestCount
  private var _oldNestId = _nestId
  def nestId = _nestId
  def nestId_= (value:Int):Unit = {
    _oldNestId = _nestId
    _nestId = value
  }

  def initSolution: Double = {
    val servFrac: Double =
      if (ant.morphType == MaxMorph) {
        (newCount - servCount).toDouble / newCount
      } else {
        newCount.toDouble / servCount
      }

    val change : Int = (servFrac * servCount).toInt

    if (ant.morphType == MaxMorph) {
      for (i <- 0 until change) {
        simSolution += 0
      }
    }

    for (i <- change until ant.history.size) {
      simSolution += ant.history(i)._2
    }

    val avgPheromone = simSolution.sum / simSolution.size
    if (avgPheromone < ant.MIN_MORPH) {
      1
    } else {
      val percentage = (avgPheromone - ant.MIN_MORPH) / (ant.MAX_MORPH - ant.MIN_MORPH)
      1 - (percentage - 0.5).abs
    }
  }

  override def toString() = s"""(New count: ${newCount}, Fitness: ${solFitness}, NestId: ${_nestId}, OldNestId: ${_oldNestId})"""
}
