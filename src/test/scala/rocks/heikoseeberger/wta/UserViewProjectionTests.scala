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
import akka.persistence.query.{ EventEnvelope, Offset }
import akka.stream.{ ActorMaterializer, Materializer }
import akka.stream.scaladsl.{ Sink, Source }
import akka.util.Timeout
import eu.timepit.refined.auto.autoRefineV
import scala.concurrent.duration.DurationInt
import utest._

object UserViewProjectionTests extends ActorSystemTests {
  import UserViewProjection._
  import akka.actor.typed.scaladsl.adapter._
  import system.dispatcher

  private implicit val mat: Materializer = ActorMaterializer()

  override def tests = Tests {
    'project - {
      implicit val t: Timeout   = 1.second
      implicit val s: Scheduler = system.scheduler
      val user                  = User("username": User.Username, "nickname": User.Nickname)
      val username              = user.username

      def eventsByPersistenceId(lastSeqNo: Long) =
        Source(
          Vector(
            EventEnvelope(Offset.noOffset,
                          UserRepository.Name,
                          lastSeqNo + 1,
                          UserRepository.UserAdded(user)),
            EventEnvelope(Offset.noOffset,
                          UserRepository.Name,
                          lastSeqNo + 2,
                          UserRepository.UserRemoved(username))
          )
        )

      val userView =
        system.spawnAnonymous(Actor.immutablePartial[UserView.Command] {
          case (_, UserView.AddUser(`user`, 43, replyTo)) =>
            replyTo ! Done
            Actor.same
          case (_, UserView.RemoveUser(`username`, 44, replyTo)) =>
            replyTo ! Done
            Actor.same
        })

      Source
        .single(42)
        .map(UserView.LastSeqNo(_))
        .via(project(eventsByPersistenceId, userView))
        .runWith(Sink.seq)
        .map { result =>
          val size = result.size
          assert(size == 2)
        }
    }
  }
}
