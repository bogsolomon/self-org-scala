package ca.ncct.uottawa.docker.proxy

import akka.actor.ActorSystem
import spray.routing.SimpleRoutingApp

import scala.sys.process._
import scala.util.{Failure, Success}

object Main extends App with SimpleRoutingApp {

  implicit val system = ActorSystem("simple-routing-app")

  import system.dispatcher

  startServer("172.30.4.2", port = 8080) {
    get {
      path("serverIp") {
        parameter('managedHost) {
          managedHost => {
            val ipAddr: String = getIpAddress(managedHost)
            complete(ipAddr)
          }
        }
      } ~
        path("addNode") {
          parameters('instName, 'controlName, 'port.as[Int], 'netName, 'startPort.as[Int]) {
            (instName, controlName, port, netName, startPort) => {
              val mediaServCommand = s"docker -H 172.30.4.2:4000 run -d --net=${netName} --name=$instName " +
                s"-e red5_port=$port -v /etc/hostname:/hostname -p ${port}:${startPort} bsolomon/red5-media:v1"
              logRequest("New command is: " + mediaServCommand, akka.event.Logging.InfoLevel)
              var exitValue = Process(mediaServCommand).run().exitValue()
              val commandControl = s"docker -H 172.30.4.2:4000 run -d --net=${netName} --name=$controlName " +
                s"-e red5_port=$port -v /etc/hostname:/hostname -e managed_host=$instName " +
                s"-v /usr/local/docker-images/red5-control/config:/config bsolomon/red5-control:v1"
              logRequest("New command is: " + commandControl, akka.event.Logging.InfoLevel)
              exitValue += Process(commandControl).run().exitValue()
              complete("addNode:" + exitValue)
            }
          }
        } ~
        path("removeNode") {
          parameter('instName) {
            instName => {
              val stopCommand = s"docker -H 172.30.4.2:4000 stop $instName"
              logRequest("New command is: " + stopCommand, akka.event.Logging.InfoLevel)
              var exitValue = Process(stopCommand).run().exitValue()
              val rmComand = s"docker -H 172.30.4.2:4000 rm $instName"
              logRequest("New command is: " + rmComand, akka.event.Logging.InfoLevel)
              exitValue += Process(rmComand).run().exitValue()
              complete("removeNode:" + exitValue)
            }
          }
        }
    }
  }.onComplete {
    case Success(b) =>
      println(s"Successfully bound to ${b.localAddress}")
    case Failure(ex) =>
      println(ex.getMessage)
      system.terminate()
  }

  def getIpAddress(managedHost: String): String = {
    val result = s"docker -H 172.30.4.2:4000 ps -a".lineStream_!

    def findHost: String = {
      result.foreach(line => {
        logRequest("Parse line: " + line, akka.event.Logging.InfoLevel)
        if (line.contains(managedHost)) {
          val hostName = line.substring(line.lastIndexOf(" "), line.lastIndexOf("/"))
          logRequest("hostName: " + hostName, akka.event.Logging.InfoLevel)
          if (hostName.trim.equals("cloud1")) {
            return "172.30.3.1"
          } else if (hostName.trim.equals("cloud2")) {
            return "172.30.4.2"
          } else if (hostName.trim.equals("cloud5")) {
            return "172.30.6.5"
          }
        }
      })
      ""
    }

    findHost
  }
}
