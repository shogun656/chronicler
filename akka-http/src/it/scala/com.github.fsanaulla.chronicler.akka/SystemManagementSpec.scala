package com.github.fsanaulla.chronicler.akka

import _root_.akka.actor.ActorSystem
import _root_.akka.testkit.TestKit
import com.github.fsanaulla.chronicler.akka.clients.AkkaManagementClient
import com.github.fsanaulla.chronicler.testing.it.ResultMatchers.NoContentResult
import com.github.fsanaulla.chronicler.testing.it.{DockerizedInfluxDB, Futures}
import com.github.fsanaulla.chronicler.testing.unit.FlatSpecWithMatchers

import scala.concurrent.ExecutionContext.Implicits.global

/**
  * Created by
  * Author: fayaz.sanaulla@gmail.com
  * Date: 07.09.17
  */
class SystemManagementSpec
  extends TestKit(ActorSystem())
    with FlatSpecWithMatchers
    with Futures
    with DockerizedInfluxDB {

  lazy val influx: AkkaManagementClient =
    Influx.management(host, port, Some(creds))

  "System api" should "ping InfluxDB" in {
    influx.ping.futureValue shouldEqual NoContentResult
  }
}
