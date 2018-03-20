package com.github.fsanaulla.async.utils

import java.net.URLEncoder

import com.github.fsanaulla.core.model._
import spray.json.{DeserializationException, JsArray, JsNumber, JsString}

/**
  * Created by fayaz on 11.07.17.
  */
object TestHelper {

  implicit class StringRich(val str: String) extends AnyVal {
    def encode: String = URLEncoder.encode(str)
  }

  final val currentNanoTime: Long = System.currentTimeMillis() * 1000000
  final val OkResult = Result(200, isSuccess = true)
  final val NoContentResult = Result(204, isSuccess = true)
  final val AuthErrorResult = Result(401, isSuccess = false, Some(new AuthorizationException("unable to parse authentication credentials")))

  case class FakeEntity(firstName: String,
                        lastName: String,
                        age: Int)

  implicit object FormattableFE extends InfluxFormatter[FakeEntity] {
    override def write(obj: FakeEntity): String =
      s"firstName=${obj.firstName},lastName=${obj.lastName} age=${obj.age}"

    override def read(js: JsArray): FakeEntity = js.elements match {
      case Vector(_, JsNumber(age), JsString(fname), JsString(lname)) => FakeEntity(fname, lname, age.toInt)
      case _ => throw DeserializationException(s"Can't deserialize $RetentionPolicyInfo object")
    }
  }

  def queryTesterAuth(query: String)(credentials: InfluxCredentials): String =
    s"http://localhost:8086/query?q=${query.encode}&p=${credentials.password.encode}&u=${credentials.username.encode}"


  def queryTesterAuth(db: String, query: String)(credentials: InfluxCredentials): String =
    s"http://localhost:8086/query?q=${query.encode}&p=${credentials.password.encode}&db=${db.encode}&u=${credentials.username.encode}"


  def queryTester(query: String): String = {
    s"http://localhost:8086/query?q=${query.encode}"
  }

  def queryTester(db: String, query: String): String = {
    s"http://localhost:8086/query?q=${query.encode}&db=${db.encode}"
  }

  def queryTester(mp: Map[String, String]): String = {
    val s = mp.map {
      case (k, v) => s"$k=${v.encode}"
    }.mkString("&")

    s"http://localhost:8086/write?$s"
  }
}