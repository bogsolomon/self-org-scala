package ca.ncct.uottawa.control.selforg.bootstrap.component.data

/**
  * Created by Bogdan on 2/18/2016.
  */
case class SensorMeasurement(cpu: Double) {

}

object SensorMeasurement {
  def fromXML(node: scala.xml.NodeSeq) : SensorMeasurement =
    new SensorMeasurement(
      cpu = ((node \ "stats") \ "@cpu").text.toDouble
    )
}
