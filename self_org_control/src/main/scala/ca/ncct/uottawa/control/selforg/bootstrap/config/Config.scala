package ca.ncct.uottawa.control.selforg.bootstrap.config

/**
  * Created by Bogdan on 2/14/2016.
  */
case class Config(val sensors: List[SensorConfig], filter: FilterConfig) {

}

object Config {
  def fromXML(node: scala.xml.NodeSeq): Config =
    new Config(
      List[SensorConfig]((node \ "sensor").toList map { s => SensorConfig.fromXML(s) }: _*),
      FilterConfig()
    )
}
