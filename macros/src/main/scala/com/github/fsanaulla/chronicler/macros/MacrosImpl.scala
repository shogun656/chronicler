/*
 * Copyright 2017-2018 Faiaz Sanaulla
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.github.fsanaulla.chronicler.macros

import com.github.fsanaulla.chronicler.macros.annotations.{field, tag, timestamp}

import scala.reflect.macros.blackbox

/**
  * Created by
  * Author: fayaz.sanaulla@gmail.com
  * Date: 13.02.18
  */
private[macros] final class MacrosImpl(val c: blackbox.Context) {
  import c.universe._

  private val TIMESTAMP_TYPE = tpdls[Long]

  private val SUPPORTED_TAGS_TYPES =
    Seq(tpdls[Option[String]], tpdls[String])

  final private val SUPPORTED_FIELD_TYPES =
    Seq(tpdls[Boolean], tpdls[Int], tpdls[Double], tpdls[String], tpdls[Float], TIMESTAMP_TYPE)

  /** return type dealias */
  def tpdls[A: TypeTag]: c.universe.Type = typeOf[A].dealias

  /** Check if this method valid timestamp */
  def isTimestamp(m: MethodSymbol): Boolean = {
    if (m.annotations.exists(_.tree.tpe =:= typeOf[timestamp])) {
      if (m.returnType =:= TIMESTAMP_TYPE) true
      else c.abort(c.enclosingPosition, s"@timestamp ${m.name} has unsupported type ${m.returnType}. Timestamp must be Long")
    } else false
  }

  /**
    * Generate read method for specified type
    * @param tpe  - for which type
    * @return     - AST that will be expanded to read method
    */
  def createReadMethod(tpe: c.universe.Type): Tree = {

    val bool = tpdls[Boolean]
    val int = tpdls[Int]
    val long = tpdls[Long]
    val double = tpdls[Double]
    val string = tpdls[String]
    val optString = tpdls[Option[String]]

    val (timeField, othFields) = tpe.decls.toList
      .collect { case m: MethodSymbol if m.isCaseAccessor => m }
      .partition(isTimestamp)

    if (timeField.size > 1)
      c.abort(c.enclosingPosition, "Only one field can be marked as @timestamp.")

    if (othFields.lengthCompare(1) < 0)
      c.abort(c.enclosingPosition, "Type parameter must be a case class with more then 1 fields.")

    val fields = othFields.map(m => m.name.decodedName.toString -> m.returnType.dealias)

    val constructorParams = fields
      .sortBy(_._1)
      .map { case (k, v) => TermName(k) -> v }
      .map {
        case (k, `bool`) => q"$k = $k.asBoolean"
        case (k, `string`) => q"$k = $k.asString"
        case (k, `int`) => q"$k = $k.asInt"
        case (k, `long`) => q"$k = $k.asLong"
        case (k, `double`) => q"$k = $k.asDouble"
        case (k, `optString`) => q"$k = $k.getString"
        case (_, other) => c.abort(c.enclosingPosition, s"Unsupported type $other")
      }

    val patternParams: List[Tree] = fields
      .map(_._1)
      .sorted // influx return results in alphabetical order
      .map(k => TermName(k))
      .map(k => pq"$k: JValue")

    val readMethodDefinition: Tree = if (timeField.nonEmpty) {

      val timestamp = TermName(timeField.head.name.decodedName.toString)

      val constructorTime: Tree = q"$timestamp = toNanoLong($timestamp.asString)"

      val patternTime: Tree = pq"$timestamp: JValue"

      val patterns: List[Tree] = patternTime :: patternParams
      val constructor: List[Tree] = constructorTime :: constructorParams

      // success case clause component
      val successPat = pq"Array(..$patterns)"
      val successBody = q"new $tpe(..$constructor)"
      val successCase = cq"$successPat => $successBody"

      // failure case clause component
      val failurePat = pq"_"
      val failureMsg = s"Can't deserialize $tpe object."
      val failureBody = q"throw new DeserializationException($failureMsg)"
      val failureCase = cq"$failurePat => $failureBody"

      val cases = successCase :: failureCase :: Nil

      q"js.vs match { case ..$cases }"

    } else {

      // success case clause component
      val successPat = pq"Array(..$patternParams)"
      val successBody = q"new $tpe(..$constructorParams)"
      val successCase = cq"$successPat => $successBody"

      // failure case clause component
      val failurePat = pq"_"
      val failureMsg = s"Can't deserialize $tpe object."
      val failureBody = q"throw new DeserializationException($failureMsg)"
      val failureCase = cq"$failurePat => $failureBody"

      val cases = successCase :: failureCase :: Nil

      q"js.vs.tail match { case ..$cases }"
    }

    q"""def read(js: JArray): $tpe = $readMethodDefinition"""
  }

  /**
    * Create write method for specified type
    * @param tpe - specified type
    * @return    - AST that will be expanded to write method
    */
  def createWriteMethod(tpe: c.Type): Tree = {

    /** Is it Option container*/
    def isOption(tpe: c.universe.Type): Boolean =
      tpe.typeConstructor =:= typeOf[Option[_]].typeConstructor

    /** Is it valid tag type */
    def isSupportedTagType(tpe: c.universe.Type): Boolean =
      SUPPORTED_TAGS_TYPES.exists(t => t =:= tpe)

    /** Is it valid field type */
    def isSupportedFieldType(tpe: c.universe.Type): Boolean =
      SUPPORTED_FIELD_TYPES.exists(t => t =:= tpe)

    /** Predicate for finding fields of instance marked with '@tag' annotation */
    def isTag(m: MethodSymbol): Boolean = {
      if (m.annotations.exists(_.tree.tpe =:= typeOf[tag])) {
        if (isSupportedTagType(m.returnType)) true
        else c.abort(c.enclosingPosition, s"@tag ${m.name} has unsupported type ${m.returnType}. Tag must have String or Optional[String]")
      } else false
    }

    /** Predicate for finding fields of instance marked with '@field' annotation */
    def isField(m: MethodSymbol): Boolean = {
      if (m.annotations.exists(_.tree.tpe =:= typeOf[field])) {
        if (isSupportedFieldType(m.returnType)) true
        else c.abort(c.enclosingPosition, s"Unsupported type for @field ${m.name}: ${m.returnType}")
      } else false
    }

    /** Check method for one of @tag, @field annotaions */
    def isMarked(m: MethodSymbol): Boolean = isTag(m) || isField(m)

    val (timeField, othField) = tpe.decls.toList
      .collect { case m: MethodSymbol if m.isCaseAccessor => m }
      .partition(isTimestamp)

    if (timeField.size > 1)
      c.abort(c.enclosingPosition, "Only one field can be marked as @timestamp.")

    if (othField.lengthCompare(1) < 0)
      c.abort(c.enclosingPosition, "Type parameter must be a case class with more then 1 fields")

    val (tagsMethods, fieldsMethods) = othField
      .filter(isMarked)
      .span {
        case m: MethodSymbol if isTag(m) => true
        case _ => false
      }

    val optTags: Seq[Tree] = tagsMethods collect {
      case m: MethodSymbol if isOption(m.returnType) =>
        q"${m.name.decodedName.toString} -> obj.${m.name}"
    }

    val nonOptTags: Seq[Tree] = tagsMethods collect {
      case m: MethodSymbol if !isOption(m.returnType) =>
        q"${m.name.decodedName.toString} -> obj.${m.name}"
    }

    val fields: Seq[Tree] = fieldsMethods map {
      m: MethodSymbol =>
        q"${m.name.decodedName.toString} -> obj.${m.name}"
    }

    def write(tpe: Type,
              fields: Seq[Tree],
              nonOptTags: Seq[Tree],
              optTags: Seq[Tree],
              optTime: Option[Tree]): c.universe.Tree = {

      q"""def write(obj: $tpe): String = {
                val fields: String =  Seq[(String, Any)](..$fields) map {
                  case (k, v: String) => k + "=" + "\"" + v + "\""
                  case (k, v: Int)    => k + "=" + v + "i"
                  case (k, v)         => k + "=" + v
                } mkString(",")

                val nonOptTags: String = Seq[(String, String)](..$nonOptTags) map {
                  case (k, v) if v.nonEmpty => k + "=" + v
                  case (k, _) => throw new IllegalArgumentException("Tag " + k + " can't be an empty string")
                } mkString(",")

                val optTags: String = Seq[(String, Option[String])](..$optTags) collect {
                  case (k, Some(v)) => k + "=" + v
                } mkString(",")

                val combTags: String = if (optTags.isEmpty) nonOptTags else nonOptTags + "," + optTags

                ${optTime.fold(q"""combTags + " " + fields trim""")(t => q"""combTags + " " + fields + " " + $t trim""")}
          }"""
    }

    timeField
      .headOption
      .map(m => q"obj.${m.name}") match {
        case None =>
          write(tpe, fields, nonOptTags, optTags, None)
        case some =>
          write(tpe, fields, nonOptTags, optTags, some)
      }
  }

  /***
    * Generate AST for current type at compile time.
    * @tparam T - Type parameter for whom will be generated AST
    */
  def writer_impl[T: c.WeakTypeTag]: Tree = {
    val tpe = c.weakTypeOf[T]
    q"""new InfluxWriter[$tpe] {${createWriteMethod(tpe)}} """
  }

  /***
    * Generate AST for current type at compile time.
    * @tparam T - Type parameter for whom will be generated AST
    */
  def reader_impl[T: c.WeakTypeTag]: Tree = {
    val tpe = c.weakTypeOf[T]

    q"""new InfluxReader[$tpe] {
          import jawn.ast.{JValue, JArray}
          import java.time.Instant
          import com.github.fsanaulla.chronicler.core.model.DeserializationException

          def toNanoLong(str: String): Long = {
            val i = Instant.parse(str)
            i.getEpochSecond * 1000000000 + i.getNano
          }

          ${createReadMethod(tpe)}
       }"""
  }

  /***
    * Generate AST for current type at compile time.
    * @tparam T - Type parameter for whom will be generated AST
    */
  def format_impl[T: c.WeakTypeTag]: Tree = {
    val tpe = c.weakTypeOf[T]

    q"""
       new InfluxFormatter[$tpe] {
          import jawn.ast.{JValue, JArray}
          import java.time.Instant
          import com.github.fsanaulla.chronicler.core.model.DeserializationException

          def toNanoLong(str: String): Long = {
            val i = Instant.parse(str)
            i.getEpochSecond * 1000000000 + i.getNano
          }

          ${createWriteMethod(tpe)}
          ${createReadMethod(tpe)}
       }"""
  }
}
