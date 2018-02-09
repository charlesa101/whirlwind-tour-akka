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

import akka.actor.Scheduler
import akka.actor.typed.{ ActorSystem => TypedActorSystem }
import akka.actor.typed.scaladsl.Actor
import akka.http.scaladsl.model.StatusCodes.{ Conflict, Created, NoContent, NotFound, OK }
import akka.http.scaladsl.testkit.{ RouteTest, TestFrameworkInterface }
import akka.util.Timeout
import de.heikoseeberger.akkahttpcirce.ErrorAccumulatingCirceSupport
import eu.timepit.refined.auto.autoRefineV
import io.circe.parser.parse
import scala.concurrent.duration.DurationInt
import utest._

object ApiTests extends TestSuite with RouteTest with TestFrameworkInterface {
  import Api._
  import ErrorAccumulatingCirceSupport._
  import akka.actor.typed.scaladsl.adapter._
  import io.circe.generic.auto._
  import io.circe.refined._

  override def tests = Tests {
    implicit val typedSystem: TypedActorSystem[Nothing] = system.toTyped
    implicit val askTimeout: Timeout                    = 3.seconds
    implicit val scheduler: Scheduler                   = system.scheduler

    'get - {
      val expectedUsers =
        UserView.Users(Set(User("username": User.Username, "nickname": User.Nickname)))
      val userRepository = system.spawnAnonymous(Actor.empty[UserRepository.SerializableCommand])
      val userView = system.spawnAnonymous(Actor.immutablePartial[UserView.Command] {
        case (_, UserView.GetUsers(replyTo)) =>
          replyTo ! expectedUsers
          Actor.empty
      })
      Get() ~> route(userRepository, userView) ~> check {
        val actualStatus = status
        assert(actualStatus == OK)
        val actualUsers = responseAs[UserView.Users]
        assert(actualUsers == expectedUsers)
      }
    }

    'postInvalid - {
      val user           = parse("""{ "username": "", "nickname": "" }""")
      val userRepository = system.spawnAnonymous(Actor.empty[UserRepository.SerializableCommand])
      val userView       = system.spawnAnonymous(Actor.empty[UserView.Command])
      Post("/", user) ~> route(userRepository, userView) ~> check {
        val actualRejections = rejections
        assert(actualRejections.nonEmpty)
      }
    }

    'post - {
      val user = parse("""{ "username": "username", "nickname": "nickname" }""")
      val userRepository = system.spawnAnonymous {
        Actor.immutablePartial[UserRepository.SerializableCommand] {
          case (_, UserRepository.AddUser(user, replyTo)) =>
            replyTo ! UserRepository.UserAdded(user)
            Actor.immutablePartial {
              case (_, UserRepository.AddUser(user, replyTo)) =>
                replyTo ! UserRepository.UsernameTaken(user.username)
                Actor.empty
            }
        }
      }
      val userView = system.spawnAnonymous(Actor.empty[UserView.Command])
      Post("/", user) ~> route(userRepository, userView) ~> check {
        val actualStatus = status
        assert(actualStatus == Created)
      }
      Post("/", user) ~> route(userRepository, userView) ~> check {
        val actualStatus = status
        assert(actualStatus == Conflict)
      }
    }

    'delete - {
      val userRepository = system.spawnAnonymous {
        Actor.immutablePartial[UserRepository.SerializableCommand] {
          case (_, UserRepository.RemoveUser(username, replyTo)) =>
            replyTo ! UserRepository.UserRemoved(username)
            Actor.immutablePartial {
              case (_, UserRepository.RemoveUser(username, replyTo)) =>
                replyTo ! UserRepository.UsernameUnknown(username)
                Actor.empty
            }
        }
      }
      val userView = system.spawnAnonymous(Actor.empty[UserView.Command])
      Delete("/username") ~> route(userRepository, userView) ~> check {
        val actualStatus = status
        assert(actualStatus == NoContent)
      }
      Delete("/username") ~> route(userRepository, userView) ~> check {
        val actualStatus = status
        assert(actualStatus == NotFound)
      }
    }
  }

  override def failTest(msg: String) = throw new Exception(s"Test failed: $msg")

  override def utestAfterAll() = {
    cleanUp()
    super.utestAfterAll()
  }
}
