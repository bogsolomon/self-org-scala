package ca.ncct.uottawa.control.selforg.bootstrap.component

import akka.actor.{ActorLogging, Actor, Props}
import ca.ncct.uottawa.control.selforg.bootstrap.component.data.{Model, FilterMeasurement}
import ca.ncct.uottawa.control.selforg.bootstrap.config.GenericConfig
import com.fuzzylite.term.Trapezoid
import com.fuzzylite.variable.InputVariable

/**
  * Created by Bogdan on 7/31/2016.
  */
object FuzzyModel {
  def props(config: GenericConfig): Props = Props(new FuzzyModel(config))
}

class FuzzyModel(config: GenericConfig) extends Actor with ActorLogging {
  val clientsIV: InputVariable = new InputVariable
  clientsIV.addTerm(new Trapezoid("", 0, config.params("CLIENTS_THR").toDouble, Double.MaxValue, Double.MaxValue))
  val cpuIV: InputVariable = new InputVariable
  cpuIV.addTerm(new Trapezoid("", 0, config.params("CPU_THR").toDouble, Double.MaxValue, Double.MaxValue))
  val streamsInIV: InputVariable = new InputVariable
  streamsInIV.addTerm(new Trapezoid("", 0, config.params("STREAMSIN_THR").toDouble, Double.MaxValue, Double.MaxValue))
  val streamsOutIV: InputVariable = new InputVariable
  streamsOutIV.addTerm(new Trapezoid("", 0, config.params("STREAMSOUT_THR").toDouble, Double.MaxValue, Double.MaxValue))

  var started = false

  override def receive = {
    case msg: FilterMeasurement => updateModel(msg)
  }

  def updateModel(msg: FilterMeasurement): Unit = {
    log.debug("updateModel: " + msg.toString)

    val clients = msg.sensorMeasurement.user.localUsers
    val streamsIn = msg.sensorMeasurement.stream.inStreams
    val cpu = msg.sensorMeasurement.cpu
    val packetsIn = msg.packetIn
    val packetsOut = msg.packetOut

    var confidenceR1: Double = clientsIV.fuzzify(clients).split("/")(0).toDouble

    log.debug("clientsIV: " + confidenceR1)

    if (packetsIn != 0 && streamsIn != 0) {
      val outValue: Double = streamsOutIV.fuzzify(packetsOut / (packetsIn / streamsIn)).split("/")(0).toDouble
      log.debug("streamsOutValue: " + outValue)
      confidenceR1 = (confidenceR1 + outValue) / 2
      log.debug("confidenceR1: " + confidenceR1)
    }
    else {
      confidenceR1 = confidenceR1 / 2
      log.debug("confidenceR1: " + confidenceR1)
    }

    var confidenceR2: Double = streamsInIV.fuzzify(packetsIn).split("/")(0).toDouble
    log.debug("streamsInIV: " + confidenceR2)
    val cpuValue: Double = cpuIV.fuzzify(cpu).split("/")(0).toDouble
    log.debug("cpuValue: " + cpuValue)
    confidenceR2 = (confidenceR2 + cpuValue) / 2
    log.debug("confidenceR2: " + confidenceR2)

    if (packetsIn != 0 || started) {
      sender ! Model((confidenceR1 + confidenceR2) / 2)
      started = true
    }
  }
}

