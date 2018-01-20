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

import akka.Done
import akka.actor.Scheduler
import akka.actor.typed.scaladsl.Actor
import akka.actor.typed.{ ActorRef, Behavior, Terminated, ActorSystem => TypedActorSystem }
import akka.http.scaladsl.model.StatusCodes.{ Conflict, Created, NoContent, NotFound, OK }
import akka.http.scaladsl.testkit.{ RouteTest, TestFrameworkInterface }
import akka.stream.{ ActorMaterializer, Materializer }
import akka.testkit.typed.scaladsl.TestProbe
import akka.util.Timeout
import de.heikoseeberger.akkahttpcirce.ErrorAccumulatingCirceSupport
import io.circe.parser.parse
import scala.concurrent.duration.DurationInt
import utest._

object ApiTests extends ActorSystemTests {
  import Api._
  import akka.actor.typed.scaladsl.adapter._

  private implicit val mat: Materializer = ActorMaterializer()

  override def tests = Tests {
    'behavior - {
      def api   = Api(Config("127.0.0.1", 8001, 42.seconds), system.deadLetters)
      val probe = TestProbe[Done]()(system.toTyped)
      val test =
        Actor.deferred[Done] { context =>
          val api1 = context.spawnAnonymous(api)
          val api2 = context.spawnAnonymous(api)
          context.watch(api1)
          context.watch(api2)
          def beh(apis: Set[ActorRef[Nothing]]): Behavior[Done] =
            Actor
              .immutable[Done] {
                case (_, Done) =>
                  if (apis.size == 1) probe.ref ! Done
                  Actor.stopped
              }
              .onSignal {
                case (context, Terminated(api)) =>
                  context.setReceiveTimeout(1.second, Done)
                  beh(apis + api)
              }
          beh(Set.empty)
        }
      system.spawnAnonymous(test)
      probe.expectMsg(Done)
    }
  }
}

object ApiRouteTests extends TestSuite with RouteTest with TestFrameworkInterface {
  import Api._
  import ErrorAccumulatingCirceSupport._
  import akka.actor.typed.scaladsl.adapter._

  private implicit val typedSystem: TypedActorSystem[Nothing] = system.toTyped

  private implicit val askTimeout: Timeout = 3.seconds

  private implicit val scheduler: Scheduler = system.scheduler

  override def tests = Tests {
    'get - {
      val userRepository = system.spawnAnonymous(Actor.empty[UserRepository.Command])
      Get() ~> route(userRepository) ~> check {
        val actualStatus = status
        assert(actualStatus == OK)
        val actualContent = responseAs[String]
        assert(actualContent == "GET received")
      }
    }

    'postInvalid - {
      val user           = parse("""{ "username": "", "nickname": "" }""")
      val userRepository = system.spawnAnonymous(Actor.empty[UserRepository.Command])
      Post("/", user) ~> route(userRepository) ~> check {
        val actualRejections = rejections
        assert(actualRejections.nonEmpty)
      }
    }

    'post - {
      val user = parse("""{ "username": "username", "nickname": "nickname" }""")
      val userRepository = system.spawnAnonymous {
        Actor.immutablePartial[UserRepository.Command] {
          case (_, UserRepository.AddUser(user, replyTo)) =>
            replyTo ! UserRepository.UserAdded(user)
            Actor.immutablePartial {
              case (_, UserRepository.AddUser(user, replyTo)) =>
                replyTo ! UserRepository.UsernameTaken(user.username)
                Actor.empty
            }
        }
      }
      Post("/", user) ~> route(userRepository) ~> check {
        val actualStatus = status
        assert(actualStatus == Created)
      }
      Post("/", user) ~> route(userRepository) ~> check {
        val actualStatus = status
        assert(actualStatus == Conflict)
      }
    }

    'delete - {
      val userRepository = system.spawnAnonymous {
        Actor.immutablePartial[UserRepository.Command] {
          case (_, UserRepository.RemoveUser(username, replyTo)) =>
            replyTo ! UserRepository.UserRemoved(username)
            Actor.immutablePartial {
              case (_, UserRepository.RemoveUser(username, replyTo)) =>
                replyTo ! UserRepository.UsernameUnknown(username)
                Actor.empty
            }
        }
      }
      Delete("/username") ~> route(userRepository) ~> check {
        val actualStatus = status
        assert(actualStatus == NoContent)
      }
      Delete("/username") ~> route(userRepository) ~> check {
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
