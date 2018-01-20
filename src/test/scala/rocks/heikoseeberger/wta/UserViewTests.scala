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
import akka.testkit.typed.scaladsl.TestProbe
import eu.timepit.refined.auto.autoRefineV
import scala.concurrent.duration.DurationInt
import utest._

object UserViewTests extends ActorSystemTests {
  import UserView._
  import akka.actor.typed.scaladsl.adapter._

  override def tests = Tests {
    'happyPath - {
      val user     = User("username": User.Username, "nickname": User.Nickname)
      val userView = system.spawnAnonymous(UserView())

      val lastSeqNoProbe = TestProbe[LastSeqNo]()
      userView ! GetLastSeqNo(lastSeqNoProbe.ref)
      lastSeqNoProbe.expectMsg(LastSeqNo(0))

      val usersProbe = TestProbe[Users]()
      userView ! GetUsers(usersProbe.ref)
      usersProbe.expectMsg(Users(Set.empty))

      val doneProbe = TestProbe[Done]()
      userView ! AddUser(user, 1, doneProbe.ref)
      doneProbe.expectMsg(Done)
      userView ! GetLastSeqNo(lastSeqNoProbe.ref)
      lastSeqNoProbe.expectMsg(LastSeqNo(1))
      userView ! GetUsers(usersProbe.ref)
      usersProbe.expectMsg(Users(Set(user)))

      userView ! RemoveUser("username": User.Username, 2, doneProbe.ref)
      doneProbe.expectMsg(Done)
      userView ! GetLastSeqNo(lastSeqNoProbe.ref)
      lastSeqNoProbe.expectMsg(LastSeqNo(2))
      userView ! GetUsers(usersProbe.ref)
      usersProbe.expectMsg(Users(Set.empty))
    }

    'illegalSeqNo - {
      val user      = User("username": User.Username, "nickname": User.Nickname)
      val userView  = system.spawnAnonymous(UserView())

      val doneProbe = TestProbe[Done]()
      userView ! AddUser(user, 42, doneProbe.ref)
      doneProbe.expectNoMsg(100.milliseconds)

      val lastSeqNoProbe = TestProbe[LastSeqNo]()
      userView ! GetLastSeqNo(lastSeqNoProbe.ref)
      lastSeqNoProbe.expectMsg(LastSeqNo(0))
    }
  }
}
