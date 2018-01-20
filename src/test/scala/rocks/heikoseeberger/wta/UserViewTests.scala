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
import akka.testkit.typed.{ BehaviorTestkit, TestInbox }
import eu.timepit.refined.auto.autoRefineV
import utest._

object UserViewTests extends TestSuite {
  import UserView._

  override def tests = Tests {
    val user = User("username": User.Username, "nickname": User.Nickname)

    'happyPath - {
      val userView = BehaviorTestkit(UserView())

      val lastSeqNoInbox = TestInbox[LastSeqNo]()
      userView.run(GetLastSeqNo(lastSeqNoInbox.ref))
      lastSeqNoInbox.expectMsg(LastSeqNo(0))

      val usersInbox = TestInbox[Users]()
      userView.run(GetUsers(usersInbox.ref))
      usersInbox.expectMsg(Users(Set.empty))

      val doneInbox = TestInbox[Done]()
      userView.run(AddUser(user, 1, doneInbox.ref))
      doneInbox.expectMsg(Done)
      userView.run(GetLastSeqNo(lastSeqNoInbox.ref))
      lastSeqNoInbox.expectMsg(LastSeqNo(1))
      userView.run(GetUsers(usersInbox.ref))
      usersInbox.expectMsg(Users(Set(user)))

      userView.run(RemoveUser("username": User.Username, 2, doneInbox.ref))
      doneInbox.expectMsg(Done)
      userView.run(GetLastSeqNo(lastSeqNoInbox.ref))
      lastSeqNoInbox.expectMsg(LastSeqNo(2))
      userView.run(GetUsers(usersInbox.ref))
      usersInbox.expectMsg(Users(Set.empty))
    }

    'illegalSeqNo - {
      val userView = BehaviorTestkit(UserView())

      val doneInbox = TestInbox[Done]()
      userView.run(AddUser(user, 42, doneInbox.ref))
      assert(!doneInbox.hasMessages)

      val lastSeqNoInbox = TestInbox[LastSeqNo]()
      userView.run(GetLastSeqNo(lastSeqNoInbox.ref))
      lastSeqNoInbox.expectMsg(LastSeqNo(0))
    }
  }
}
