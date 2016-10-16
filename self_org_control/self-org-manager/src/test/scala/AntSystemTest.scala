/*
import akka.actor.{ActorSystem, Address}
import akka.cluster.{Member, MemberStatus}
import akka.testkit.{TestActorRef, TestKit}
import ca.ncct.uottawa.control.selforg.bootstrap.ants.{Ant, AntSystemConfig}
import ca.ncct.uottawa.control.selforg.bootstrap.ants.Ant.MaxMorph
import ca.ncct.uottawa.control.selforg.manager.ants.{AntSystem, Metrics}
import org.scalatest.{Matchers, WordSpecLike}

import scala.collection.mutable.ListBuffer

/**
  * Created by Bogdan on 10/1/2016.
  */
class AntSystemTest extends TestKit(ActorSystem("testSystem")) with WordSpecLike with Matchers {

  "The ant system" should {
    val testActor = TestActorRef[AntSystem]
    val antSystemConfig : AntSystemConfig = AntSystemConfig(0, 0, 0, 0, 0, 0, 0)

    "return a value when it has at least 2 ants and 2 servers" in {
      val antSystem: AntSystem = testActor.underlyingActor
      val ant1: Ant = new Ant(List(), antSystemConfig)
      ant1.history = ListBuffer(Tuple2(null, 12), Tuple2(null, 12))
      ant1.morphType = MaxMorph
      val ant2: Ant = new Ant(List(), antSystemConfig)
      ant2.morphType = MaxMorph
      ant2.history = ListBuffer(Tuple2(null, 12), Tuple2(null, 12))
      antSystem.activeAnts = ListBuffer(ant1, ant2)
      antSystem.controlMemberSize = 2
      antSystem.houseHuntingOptimization
    }
  }

}
*/
