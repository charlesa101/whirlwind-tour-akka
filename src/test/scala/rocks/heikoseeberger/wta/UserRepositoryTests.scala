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

import akka.testkit.typed.scaladsl.TestProbe
import eu.timepit.refined.auto.autoRefineV
import utest._

// TODO Must be ActorSystemTests until persistent behavior can be wrapped!
object UserRepositoryTests extends ActorSystemTests {
  import UserRepository._
  import akka.actor.typed.scaladsl.adapter._

  override val tests = Tests {
    'protocol - {
      val user           = User("username": User.Username, "nickname": User.Nickname)
      val userRepository = system.spawnAnonymous(UserRepository())

      val addUserReplyProbe = TestProbe[AddUserReply]()
      userRepository ! AddUser(user, addUserReplyProbe.ref)
      addUserReplyProbe.expectMsg(UserAdded(user))
      userRepository ! AddUser(user, addUserReplyProbe.ref)
      addUserReplyProbe.expectMsg(UsernameTaken(user.username))

      val removeUserReplyProbe = TestProbe[RemoveUserReply]
      userRepository ! RemoveUser(user.username, removeUserReplyProbe.ref)
      removeUserReplyProbe.expectMsg(UserRemoved(user.username))
      userRepository ! RemoveUser(user.username, removeUserReplyProbe.ref)
      removeUserReplyProbe.expectMsg(UsernameUnknown(user.username))
    }
  }
}
