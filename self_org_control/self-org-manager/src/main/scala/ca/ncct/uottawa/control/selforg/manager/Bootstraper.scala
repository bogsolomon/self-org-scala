package ca.ncct.uottawa.control.selforg.manager

import akka.cluster.Cluster
import akka.cluster.ClusterEvent._
import ca.ncct.uottawa.control.selforg.manager.util.Utils
import com.typesafe.config.ConfigFactory

import scala.xml.XML
import akka.actor.{Props, ActorSystem}
import ca.ncct.uottawa.control.selforg.manager.config.Config

/**
  * Created by Bogdan on 2/11/2016.
  */
object Bootstraper {

  def startManager(systemConfig: Config): Unit = {
    val localIpAddress: String = if (System.getenv("is_seed").toBoolean) systemConfig.seedNode else Utils.findEth0Address
    val customConf = ConfigFactory.parseString(
      s"""remote {
        akka {
          remote {
            netty.tcp {
              hostname = "${localIpAddress}"
            }
          }
        }
        }""")
    val config = ConfigFactory.load()
    val system = ActorSystem("controlSystem", customConf.getConfig("remote").
        withFallback(ConfigFactory.parseString("akka.cluster.roles = [manager]")).
        withFallback(ConfigFactory.parseString(s"""akka.cluster.seed-nodes = ["akka.tcp://controlSystem@${systemConfig.seedNode}:2551"]""")).
        withFallback(config))
    system.actorOf(Props(classOf[Manager], systemConfig), "manager")

    val cluster = Cluster(system)
    val clusterListener = system.actorOf(Props(new ClusterMessageListener()))
    cluster.subscribe(clusterListener, initialStateMode = InitialStateAsEvents,
      classOf[MemberEvent], classOf[UnreachableMember], classOf[MemberUp], classOf[MemberRemoved])
  }

  def main(args: Array[String]): Unit = {
    var configLoc = "config.xml"

    if (args.length != 0) {
      configLoc = args(0)
    }

    // read system config
    val configXml = XML.loadFile(configLoc)
    val config = Config.fromXML(configXml)

    startManager(config)
  }
}
