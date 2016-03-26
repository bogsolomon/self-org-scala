package ca.ncct.uottawa.control.selforg.bootstrap.component.data

import spray.http.MediaTypes
import spray.httpx.unmarshalling.Unmarshaller

import scala.xml.NodeSeq

/**
  * Created by Bogdan on 2/18/2016.
  */
case class SensorMeasurement(cpu: Double) {

}

object SensorMeasurement {
  implicit val SensorMeasurementUnmarshaller: Unmarshaller[SensorMeasurement] = Unmarshaller.delegate[NodeSeq, SensorMeasurement](MediaTypes.`text/xml`, MediaTypes.`application/xml`, MediaTypes.`application/octet-stream`) {
    nodeSeq =>
      SensorMeasurement.fromXML(nodeSeq)
  }

  def fromXML(node: scala.xml.NodeSeq) : SensorMeasurement =
    new SensorMeasurement(
      cpu = ((node \ "stats") \ "@cpu").text.toDouble
    )
}
