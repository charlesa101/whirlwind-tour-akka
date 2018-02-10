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

import akka.actor.typed.{ ActorRef, Behavior }
import akka.persistence.typed.scaladsl.PersistentActor
import akka.persistence.typed.scaladsl.PersistentActor.{ CommandHandler, Effect }
import java.io.{ Serializable => JavaSerializable }
import org.apache.logging.log4j.scala.Logging

object UserRepository extends Logging {
  import User._

  sealed trait Serializable extends JavaSerializable

  // Message protocol – start

  sealed trait Command
  sealed trait SerializableCommand extends Command with Serializable
  sealed trait Event               extends Serializable

  final case class AddUser(user: User, replyTo: ActorRef[AddUserReply]) extends SerializableCommand
  sealed trait AddUserReply
  final case class UsernameTaken(username: Username) extends AddUserReply with Serializable
  final case class UserAdded(user: User)             extends AddUserReply with Event

  final case class RemoveUser(username: Username, replyTo: ActorRef[RemoveUserReply])
      extends SerializableCommand
  sealed trait RemoveUserReply
  final case class UsernameUnknown(username: Username) extends RemoveUserReply with Serializable
  final case class UserRemoved(username: Username)     extends RemoveUserReply with Event

  final case object Stop extends Command

  // Message protocol – end

  final case class State(usernames: Set[String] = Set.empty)

  final val Name = "user-repository"

  def apply(): Behavior[Command] =
    PersistentActor.immutable(Name, State(), commandHandler, eventHandler)

  def addUser(user: User)(replyTo: ActorRef[AddUserReply]): AddUser =
    AddUser(user, replyTo)

  def removeUser(username: Username)(replyTo: ActorRef[RemoveUserReply]): RemoveUser =
    RemoveUser(username, replyTo)

  private def commandHandler =
    CommandHandler[Command, Event, State] {
      case (_, State(usernames), AddUser(user @ User(username, _), replyTo)) =>
        if (usernames.contains(username.value)) {
          logger.info(s"Username $username taken")
          replyTo ! UsernameTaken(username)
          Effect.none
        } else {
          val userAdded = UserAdded(user)
          Effect
            .persist(userAdded)
            .andThen { _ =>
              logger.info(s"User with username $username added")
              replyTo ! userAdded
            }
        }

      case (_, State(usernames), RemoveUser(username, replyTo)) =>
        if (!usernames.contains(username.value)) {
          logger.info(s"Username $username unknown")
          replyTo ! UsernameUnknown(username)
          Effect.none
        } else {
          val userRemoved = UserRemoved(username)
          Effect
            .persist(userRemoved)
            .andThen { _ =>
              logger.info(s"User with username $username removed")
              replyTo ! userRemoved
            }
        }

      case (_, _, Stop) =>
        Effect.stop
    }

  private def eventHandler(state: State, event: Event) =
    event match {
      case UserAdded(user)       => state.copy(state.usernames + user.username.value)
      case UserRemoved(username) => state.copy(state.usernames - username.value)
    }
}
