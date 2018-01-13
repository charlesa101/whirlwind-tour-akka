/*
 * Copyright 2018 Heiko Seeberger
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

package rocks.heikoseeberger.wta

import akka.http.scaladsl.model.StatusCodes.OK
import akka.http.scaladsl.testkit.{ RouteTest, TestFrameworkInterface }
import de.heikoseeberger.akkahttpcirce.ErrorAccumulatingCirceSupport
import io.circe.parser.parse
import utest._

object ApiTests extends TestSuite with RouteTest with TestFrameworkInterface {
  import Api._
  import ErrorAccumulatingCirceSupport._

  override def tests = Tests {
    'get - {
      Get() ~> route ~> check {
        val actualStatus = status
        assert(actualStatus == OK)
        val actualContent = responseAs[String]
        assert(actualContent == "GET received")
      }
    }

    'postInvalid - {
      val user = parse("""{ "username": "", "nickname": "" }""")
      Post("/", user) ~> route ~> check {
        val actualRejections = rejections
        assert(actualRejections.nonEmpty)
      }
    }

    'post - {
      val user = parse("""{ "username": "username", "nickname": "nickname" }""")
      Post("/", user) ~> route ~> check {
        val actualStatus = status
        assert(actualStatus == OK)
        val actualContent = responseAs[String]
        assert(actualContent == "POST User(username,nickname) received")
      }
    }

    'delete - {
      Delete("/username") ~> route ~> check {
        val actualStatus = status
        assert(actualStatus == OK)
        val actualContent = responseAs[String]
        assert(actualContent == s"DELETE username received")
      }
    }
  }

  override def failTest(msg: String) = throw new Exception(s"Test failed: $msg")

  override def utestAfterAll() = {
    cleanUp()
    super.utestAfterAll()
  }
}
