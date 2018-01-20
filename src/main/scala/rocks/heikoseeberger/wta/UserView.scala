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
import akka.actor.typed.{ ActorRef, Behavior }
import akka.actor.typed.scaladsl.Actor
import cats.instances.string.catsKernelStdOrderForString
import cats.syntax.eq.catsSyntaxEq
import org.apache.logging.log4j.scala.Logging

object UserView extends Logging {

  // Message protocol – start

  sealed trait Command

  final case class GetUsers(replyTo: ActorRef[Users]) extends Command
  final case class Users(users: Set[User])

  final case class GetLastSeqNo(replyTo: ActorRef[LastSeqNo]) extends Command
  final case class LastSeqNo(nextSeqNo: Long)

  final case class AddUser(user: User, seqNo: Long, replyTo: ActorRef[Done]) extends Command

  final case class RemoveUser(username: User.Username, seqNo: Long, replyTo: ActorRef[Done])
      extends Command

  // Message protocol – end

  final val Name = "user-view"

  def apply(users: Set[User] = Set.empty, lastSeqNo: Long = 0): Behavior[Command] =
    Actor.immutable {
      case (_, GetUsers(replyTo)) =>
        replyTo ! Users(users)
        Actor.same

      case (_, GetLastSeqNo(replyTo)) =>
        replyTo ! LastSeqNo(lastSeqNo)
        Actor.same

      case (_, AddUser(user @ User(username, _), seqNo, replyTo)) =>
        if (seqNo != lastSeqNo + 1) {
          logger.error(s"Resetting, because seqNo $seqNo not successor of lastSeqNo $lastSeqNo!")
          UserView()
        } else {
          logger.debug(s"User with username $username added")
          replyTo ! Done
          UserView(users + user, seqNo)
        }

      case (_, RemoveUser(username, seqNo, replyTo)) =>
        if (seqNo != lastSeqNo + 1) {
          logger.error(s"Resetting, because seqNo $seqNo not successor of lastSeqNo $lastSeqNo!")
          UserView()
        } else {
          logger.debug(s"User with username $username removed")
          replyTo ! Done
          UserView(users.filterNot(_.username.value === username.value), seqNo)
        }
    }

  def addUser(user: User, seqNo: Long)(replyTo: ActorRef[Done]): AddUser =
    AddUser(user, seqNo, replyTo)

  def removeUser(username: User.Username, seqNo: Long)(replyTo: ActorRef[Done]): RemoveUser =
    RemoveUser(username, seqNo, replyTo)
}