package ca.ncct.uottawa.control.selforg.bootstrap.component

import akka.actor.{Props, ActorLogging, Actor}
import ca.ncct.uottawa.control.selforg.bootstrap.component.data.SensorMeasurement
import ca.ncct.uottawa.control.selforg.bootstrap.config.{FilterConfig, SensorConfig}

/**
  * Created by Bogdan on 3/26/2016.
  */
object DataFilterChain {
  def props(config: FilterConfig): Props = Props(new DataFilterChain(config))
}

class DataFilterChain(config: FilterConfig) extends Actor with ActorLogging {
  override def receive  = {
    case msg : SensorMeasurement => filter(msg)
  }

  def filter(msg: SensorMeasurement): Unit = {
    log.info("" + msg)
  }
}