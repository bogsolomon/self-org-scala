package ca.ncct.uottawa.control.selforg.bootstrap.ants

/**
  * Created by Bogdan on 10/3/2016.
  */

case class AntSystemConfig(decayAmount: Int, decayRate: Int, antWaitTime: Int, antPheromone: Int, antHistorySize: Int,
                           maxMorphLevel: Int, minMorphLevel: Int)

object AntSystemConfig {
  def fromXML(node: scala.xml.NodeSeq) : AntSystemConfig =
    new AntSystemConfig(
      decayAmount = (node \ "decayAmount").text.toInt,
      decayRate = (node \ "decayRate").text.toInt,
      antWaitTime = (node \ "antWaitTime").text.toInt,
      antPheromone = (node \ "antPheromone").text.toInt,
      antHistorySize = (node \ "antHistorySize").text.toInt,
      maxMorphLevel = (node \ "maxMorphLevel").text.toInt,
      minMorphLevel = (node \ "minMorphLevel").text.toInt
    )
}
