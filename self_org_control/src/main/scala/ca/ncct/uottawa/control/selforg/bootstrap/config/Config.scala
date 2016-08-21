package ca.ncct.uottawa.control.selforg.bootstrap.config

import scala.collection.mutable

/**
  * Created by Bogdan on 2/14/2016.
  */
case class Config(val sensors: List[SensorConfig], filter: FilterConfig, coordinator: GenericConfig,
                  model: GenericConfig, estimatorConfig: GenericConfig, dmConfig: GenericConfig,
                  actuatorConfig: GenericConfig, adaptorConfig: GenericConfig, seedNode : String)

object Config {
  def fromXML(node: scala.xml.NodeSeq): Config =
    new Config(
      List[SensorConfig]((node \ "sensor").toList map { s => SensorConfig.fromXML(s) }: _*),
      FilterConfig.fromXML(node \ "filter_chain"),
      GenericConfig.fromXML(node \ "coordinator"),
      GenericConfig.fromXML(node \ "model"),
      GenericConfig.fromXML(node \ "estimator"),
      GenericConfig.fromXML(node \ "decisionMaker"),
      GenericConfig.fromXML(node \ "actuator"),
      GenericConfig.fromXML(node \ "adaptor"),
      (node \ "@cluster-seed").text
    )
}

case class SensorConfig(val sensorType:String, val managedServerHost: String, managedServerPort: Int, managedApp: String,
                        scheduledTime: Int)

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

case class FilterConfig(val filterConfigs: mutable.Map[String, mutable.Map[String, String]])

object FilterConfig {
  def fromXML(node: scala.xml.NodeSeq): FilterConfig = {
    var filterConfigs = mutable.Map[String, mutable.Map[String, String]]()
    (node \ "filter").toList foreach { s => {
      val className: String = (s \ "@class").text
      filterConfigs = filterConfigs + (className -> mutable.Map())
      (s \ "param").toList foreach { param => {
        filterConfigs(className) = filterConfigs(className) + ((param \ "@name").text -> (param \ "@value").text)
      }
      }
    }
    }
    new FilterConfig(filterConfigs)
  }
}

case class GenericConfig(val coordinatorType:String, val params:mutable.Map[String, String])
object GenericConfig {
  def fromXML(node: scala.xml.NodeSeq) : GenericConfig = {
    var params =  mutable.Map[String, String]()
    (node \ "param").toList foreach { param => {
      params = params + ((param \ "@name").text -> (param \ "@value").text)
    } }
    new GenericConfig(
      coordinatorType = (node \ "@type").text,
      params
    )
  }
}
