package ca.ncct.uottawa.control.selforg.manager.client

import akka.actor._
import ca.ncct.uottawa.control.selforg.manager.common.{RemoveNode, AddNode}
import com.typesafe.config.ConfigFactory

object Local extends App {

  val config = ConfigFactory.load()
  implicit val system = ActorSystem("LocalSystem", config.getConfig("local").withFallback(config))
  val localActor = system.actorOf(Props[LocalActor], name = "LocalActor")  // the local actor
  localActor ! "START"                                                     // start the action
  Thread.sleep(10000)
  localActor ! "STOP"                                                     // start the action

}

class LocalActor extends Actor {

  // create the remote actor (Akka 2.3 syntax)
  val remote = context.actorSelection("akka.tcp://controlSystem@172.30.4.2:5150/user/manager")
  var counter = 0

  def receive = {
    case "START" =>
      remote ! AddNode
    case "STOP" =>
      remote ! RemoveNode
    case msg: String =>
      println(s"LocalActor received message: '$msg'")
      if (counter < 5) {
        sender ! "Hello back to you"
        counter += 1
      }
  }
}