package ca.ncct.uottawa.control.selforg.bootstrap.config

import scala.collection.mutable

/**
  * Created by Bogdan on 2/14/2016.
  */
case class Config(val sensors: List[SensorConfig], filter: FilterConfig, coordinator: CoordinatorConfig, model: ModelConfig)

object Config {
  def fromXML(node: scala.xml.NodeSeq): Config =
    new Config(
      List[SensorConfig]((node \ "sensor").toList map { s => SensorConfig.fromXML(s) }: _*),
      FilterConfig.fromXML(node \ "filter_chain"),
      CoordinatorConfig.fromXML(node \ "coordinator"),
      ModelConfig.fromXML(node \ "coordinator")
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

case class CoordinatorConfig(val coordinatorType:String, val params:mutable.Map[String, String])
object CoordinatorConfig {
  def fromXML(node: scala.xml.NodeSeq) : CoordinatorConfig = {
    var params =  mutable.Map[String, String]()
    (node \ "param").toList foreach { param => {
      params = params + ((param \ "@name").text -> (param \ "@value").text)
    } }
    new CoordinatorConfig(
      coordinatorType = (node \ "@type").text,
      params
    )
  }
}

case class ModelConfig(val modelType:String, val params:mutable.Map[String, String])
object ModelConfig {
  def fromXML(node: scala.xml.NodeSeq) : ModelConfig = {
    var params =  mutable.Map[String, String]()
    (node \ "param").toList foreach { param => {
      params = params + ((param \ "@name").text -> (param \ "@value").text)
    } }
    new ModelConfig(
      modelType = (node \ "@type").text,
      params
    )
  }
}

case class EstimatorConfig(val estimatorType:String, val params:mutable.Map[String, String])
object EstimatorConfig {
  def fromXML(node: scala.xml.NodeSeq) : EstimatorConfig = {
    var params =  mutable.Map[String, String]()
    (node \ "param").toList foreach { param => {
      params = params + ((param \ "@name").text -> (param \ "@value").text)
    } }
    new EstimatorConfig(
      estimatorType = (node \ "@type").text,
      params
    )
  }
}
