package ca.ncct.uottawa.control.selforg.bootstrap.component

import akka.actor.{ActorRef, Props, ActorLogging, Actor}
import ca.ncct.uottawa.control.selforg.bootstrap.component.data.{FilterMeasurement, SensorMeasurement}
import ca.ncct.uottawa.control.selforg.bootstrap.config.{FilterConfig, SensorConfig}

/**
  * Created by Bogdan on 3/26/2016.
  */
object DataFilterChain {
  def props(config: FilterConfig, coordinator: ActorRef): Props = Props(new DataFilterChain(config, coordinator))
}

class DataFilterChain(config: FilterConfig, coordinator: ActorRef) extends Actor with ActorLogging {
  val packetSize = config.filterConfigs("BandwithToPacketFilter")("PACKET_SIZE").toInt * 8

  override def receive  = {
    case msg : SensorMeasurement => filter(msg)
  }

  def filter(msg: SensorMeasurement): Unit = {
    val packetsIn = if (msg.stream.inBw.isNaN) 0 else msg.stream.inBw * 1000 / packetSize
    val packetsOut = if (msg.stream.outBw.isNaN) 0 else msg.stream.outBw * 1000 / packetSize
    log.debug("Vals:"+packetsIn+"/"+packetsOut)

    coordinator ! FilterMeasurement(msg, packetsIn, packetsOut)
  }
}