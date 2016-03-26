package ca.ncct.uottawa.control.selforg.bootstrap.config

/**
  * Created by Bogdan on 2/18/2016.
  */
case class SensorConfig(val sensorType:String, val managedServerHost: String, managedServerPort: Int, managedApp: String,
                        scheduledTime: Int) {

}

object SensorConfig {
  def fromXML(node: scala.xml.NodeSeq) : SensorConfig =
    new SensorConfig(
      sensorType = (node \ "@type").text,
      managedServerHost = ((node \ "managed-server") \ "@host").text,
      managedServerPort = ((node \ "managed-server") \ "@port").text.toInt,
      managedApp = ((node \ "managed-server") \ "@app").text,
      scheduledTime = ((node \ "scheduler") \ "@time").text.toInt
    )
}

case class FilterConfig() {

}

object FilterConfig {
  def fromXML(node: scala.xml.NodeSeq) : FilterConfig =
    new FilterConfig()
}
