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
import akka.actor.typed.{ ActorRef, Behavior }
import akka.actor.typed.scaladsl.Actor
import akka.actor.typed.scaladsl.AskPattern.Askable
import akka.cluster.Cluster
import akka.cluster.ddata.{ ORSet, ORSetKey }
import akka.cluster.ddata.Replicator.WriteLocal
import akka.cluster.ddata.typed.scaladsl.Replicator
import akka.persistence.query.EventEnvelope
import akka.persistence.query.scaladsl.EventsByPersistenceIdQuery
import akka.stream.Materializer
import akka.stream.scaladsl.{ Flow, Sink }
import akka.util.Timeout
import akka.{ Done, NotUsed }
import cats.instances.string._
import cats.syntax.eq._
import org.apache.logging.log4j.scala.Logging
import scala.concurrent.ExecutionContext
import scala.concurrent.duration.FiniteDuration

object UserViewProjection extends Logging {
  import akka.actor.typed.scaladsl.adapter._

  // Message protocol – start

  sealed trait Command
  final case object Stop                              extends Command
  private final case object HandleEventStreamComplete extends Command

  // Message protocol – end

  abstract class EventStreamCompleteException
      extends IllegalStateException("Event stream completed unexpectedly!")
  private final case object EventStreamCompleteException extends EventStreamCompleteException

  final case class Config(askTimeout: FiniteDuration)

  final val Name = "user-projection"

  final val usersKey: ORSetKey[User] =
    ORSetKey("users")

  def apply(
      config: Config,
      readJournal: EventsByPersistenceIdQuery,
      userView: ActorRef[UserView.Command],
      replicator: ActorRef[Replicator.Command]
  )(implicit mat: Materializer): Behavior[Command] =
    Actor.deferred { context =>
      import context.executionContext
      implicit val c: Cluster   = Cluster(context.system.toUntyped)
      implicit val t: Timeout   = config.askTimeout
      implicit val s: Scheduler = context.system.scheduler
      val self                  = context.self

      readJournal
        .eventsByPersistenceId(UserRepository.Name, 0, Long.MaxValue)
        .via(project(replicator))
        .runWith(Sink.onComplete(_ => self ! HandleEventStreamComplete))

      Actor.immutable {
        case (_, HandleEventStreamComplete) => throw EventStreamCompleteException
        case (_, Stop)                      => Actor.stopped
      }
    }

  def project(
      replicator: ActorRef[Replicator.Command]
  )(implicit cluster: Cluster,
    askTimeout: Timeout,
    scheduler: Scheduler,
    ec: ExecutionContext): Flow[EventEnvelope, Done, NotUsed] = {

    val update = Replicator.Update(usersKey, ORSet.empty[User], WriteLocal) _

    def remove(username: User.Username)(users: ORSet[User]) =
      users.elements.find(_.username.value === username.value).fold(users)(users - _)

    Flow[EventEnvelope]
      .collect {
        case EventEnvelope(_, _, _, event: UserRepository.Event) => event
      }
      .mapAsync(1) {
        case UserRepository.UserAdded(user) => (replicator ? update(_ + user)).map(_ => Done)
        case UserRepository.UserRemoved(un) => (replicator ? update(remove(un))).map(_ => Done)
      }
  }
}
