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

package com.github.fsanaulla.chronicler.core.api.management

import com.github.fsanaulla.chronicler.core.handlers.{QueryHandler, RequestHandler, ResponseHandler}
import com.github.fsanaulla.chronicler.core.model._
import com.github.fsanaulla.chronicler.core.query.RetentionPolicyManagementQuery
import com.github.fsanaulla.chronicler.core.utils.DefaultInfluxImplicits._

/**
  * Created by
  * Author: fayaz.sanaulla@gmail.com
  * Date: 08.08.17
  */
private[chronicler] trait RetentionPolicyManagement[M[_], Req, Resp, Uri, Entity] extends RetentionPolicyManagementQuery[Uri] {
  self: RequestHandler[M, Req, Resp, Uri]
    with ResponseHandler[M, Resp]
    with QueryHandler[Uri]
    with Mappable[M, Resp]
    with ImplicitRequestBuilder[Uri, Req]
    with HasCredentials =>

  /**
    * Create retention policy for specified database
    * @param rpName        - retention policy name
    * @param dbName        - database name
    * @param duration      - retention policy duration
    * @param replication   - replication factor
    * @param shardDuration - shard duration value
    * @param default       - use default
    * @return              - execution result
    */
  final def createRetentionPolicy(rpName: String,
                                  dbName: String,
                                  duration: String,
                                  replication: Int = 1,
                                  shardDuration: Option[String] = None,
                                  default: Boolean = false): M[WriteResult] = {

    require(replication > 0, "Replication must greater that 0")

    mapTo(
      execute(createRetentionPolicyQuery(rpName, dbName, duration, replication, shardDuration, default)),
      toResult
    )
  }

  /** Update retention policy */
  final def updateRetentionPolicy(
                                   rpName: String,
                                   dbName: String,
                                   duration: Option[String] = None,
                                   replication: Option[Int] = None,
                                   shardDuration: Option[String] = None,
                                   default: Boolean = false): M[WriteResult] =
    mapTo(
      execute(updateRetentionPolicyQuery(rpName, dbName, duration, replication, shardDuration, default)),
      toResult
    )


  /** Drop retention policy */
  final def dropRetentionPolicy(rpName: String, dbName: String): M[WriteResult] =
    mapTo(execute(dropRetentionPolicyQuery(rpName, dbName)), toResult)

  /** Show list of retention polices */
  final def showRetentionPolicies(dbName: String): M[QueryResult[RetentionPolicyInfo]] =
    mapTo(execute(showRetentionPoliciesQuery(dbName)), toQueryResult[RetentionPolicyInfo])

}
