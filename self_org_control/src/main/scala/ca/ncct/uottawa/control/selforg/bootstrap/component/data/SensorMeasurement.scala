package ca.ncct.uottawa.control.selforg.bootstrap.component.data

import spray.http.MediaTypes
import spray.httpx.unmarshalling.Unmarshaller

import scala.xml.NodeSeq

/**
  * Created by Bogdan on 2/18/2016.
  */
case class SensorMeasurement(cpu: Double, room: RoomStats, user: UserStats, network: NetworkStats, stream : StreamStats)

case class RoomStats(clients: Int, rooms: Int)
case class UserStats(users: Int, localUsers: Int)
case class NetworkStats(latency: Double, bwUp: Double, bwDown: Double)
case class StreamStats(inBw: Double, outBw: Double, inStreams: Int, outStreams : Int)

object SensorMeasurement {
  implicit val SensorMeasurementUnmarshaller: Unmarshaller[SensorMeasurement] = Unmarshaller.delegate[NodeSeq, SensorMeasurement](MediaTypes.`text/xml`, MediaTypes.`application/xml`) {
    nodeSeq =>
      SensorMeasurement.fromXML(nodeSeq)
  }

  def fromXML(node: scala.xml.NodeSeq) : SensorMeasurement = {
    SensorMeasurement(
      cpu = (node \ "cpu").text.toDouble,
      room = RoomStats(
        clients = ((node \ "roomStats") \ "clientsInRooms").text.toInt,
        rooms = ((node \ "roomStats") \ "rooms").text.toInt
      ),
      user = UserStats(
        users = ((node \ "userStats") \ "users").text.toInt,
        localUsers = ((node \ "userStats") \ "localUsers").text.toInt
      ),
      network = NetworkStats(
        latency = ((node \ "networkStats") \ "avgLatency2").text.toDouble,
        bwUp = ((node \ "networkStats") \ "avgBwUp").text.toDouble,
        bwDown = ((node \ "networkStats") \ "avgBwDown").text.toDouble
      ),
      stream = StreamStats(
        inBw = ((node \ "streamStats") \ "clientInStreamBw").text.toDouble,
        outBw = ((node \ "streamStats") \ "clientOutStreamBw").text.toDouble,
        inStreams = ((node \ "streamStats") \ "clientStreams").text.toInt,
        outStreams = ((node \ "streamStats") \ "clientOutStreams").text.toInt
      )
    )
  }
}
