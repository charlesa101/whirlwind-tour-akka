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

import akka.{ Done, NotUsed }
import akka.actor.Scheduler
import akka.persistence.query.EventEnvelope
import akka.persistence.query.scaladsl.EventsByPersistenceIdQuery
import akka.stream.Materializer
import akka.stream.scaladsl.{ Flow, Sink, Source }
import akka.actor.typed.{ ActorRef, Behavior }
import akka.actor.typed.scaladsl.Actor
import akka.actor.typed.scaladsl.AskPattern.Askable
import akka.util.Timeout
import org.apache.logging.log4j.scala.Logging
import scala.concurrent.duration.FiniteDuration

object UserViewProjection extends Logging {

  // Message protocol – start

  sealed trait Command
  private final case object HandleEventStreamComplete extends Command

  // Message protocol – end

  abstract class EventStreamCompleteException
      extends IllegalStateException("Event stream completed unexpectedly!")
  private final case object EventStreamCompleteException extends EventStreamCompleteException

  final case class Config(askTimeout: FiniteDuration)

  final val Name = "user-projection"

  def apply(config: Config,
            readJournal: EventsByPersistenceIdQuery,
            userView: ActorRef[UserView.Command])(implicit mat: Materializer): Behavior[Command] =
    Actor.deferred { context =>
      def eventsByPersistenceId(lastSeqNo: Long) =
        readJournal.eventsByPersistenceId(UserRepository.Name, lastSeqNo + 1, Long.MaxValue)

      implicit val t: Timeout   = config.askTimeout
      implicit val s: Scheduler = context.system.scheduler
      val self                  = context.self
      Source
        .fromFuture(userView ? UserView.GetLastSeqNo)
        .via(project(eventsByPersistenceId, userView))
        .runWith(Sink.onComplete(_ => self ! HandleEventStreamComplete))

      Actor.immutable {
        case (_, HandleEventStreamComplete) => throw EventStreamCompleteException
      }
    }

  def project(
      eventsByPersistenceId: Long => Source[EventEnvelope, NotUsed],
      userView: ActorRef[UserView.Command]
  )(implicit askTimeout: Timeout, scheduler: Scheduler): Flow[UserView.LastSeqNo, Done, NotUsed] =
    Flow[UserView.LastSeqNo]
      .flatMapConcat {
        case UserView.LastSeqNo(lastSeqNo) => eventsByPersistenceId(lastSeqNo)
      }
      .collect {
        case EventEnvelope(_, _, seqNo, event: UserRepository.Event) => (seqNo, event)
      }
      .mapAsync(1) {
        case (seqNo, UserRepository.UserAdded(user)) => userView ? UserView.addUser(user, seqNo)
        case (seqNo, UserRepository.UserRemoved(un)) => userView ? UserView.removeUser(un, seqNo)
      }
}
