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

import akka.serialization.{ SerializationExtension, SerializerWithStringManifest }
import eu.timepit.refined.auto.autoRefineV
import utest._

object UserRepositorySerializerTests extends ActorSystemTests {
  import UserRepository._
  import UserRepositorySerializer._
  import akka.actor.typed.scaladsl.adapter._

  private val serialization = SerializationExtension(system)

  private val user = User("username": User.Username, "nickname": User.Nickname)

  override def tests = Tests {
    'addUser - {
      val expected   = AddUser(user, system.deadLetters)
      val serializer = findSerializer(expected)
      val bytes      = serializer.toBinary(expected)
      val actual     = serializer.fromBinary(bytes, AddUserManifest).asInstanceOf[AddUser]
      assert(actual == expected)
    }

    'usernameTaken - {
      val expected   = UsernameTaken(user.username)
      val serializer = findSerializer(expected)
      val bytes      = serializer.toBinary(expected)
      val actual     = serializer.fromBinary(bytes, UsernameTakenManifest).asInstanceOf[UsernameTaken]
      assert(actual == expected)
    }

    'userAdded - {
      val expected   = UserAdded(user)
      val serializer = findSerializer(expected)
      val bytes      = serializer.toBinary(expected)
      val actual     = serializer.fromBinary(bytes, UserAddedManifest).asInstanceOf[UserAdded]
      assert(actual == expected)
    }

    'removeUser - {
      val expected   = RemoveUser(user.username, system.deadLetters)
      val serializer = findSerializer(expected)
      val bytes      = serializer.toBinary(expected)
      val actual     = serializer.fromBinary(bytes, RemoveUserManifest).asInstanceOf[RemoveUser]
      assert(actual == expected)
    }

    'usernameUnknown - {
      val expected   = UsernameUnknown(user.username)
      val serializer = findSerializer(expected)
      val bytes      = serializer.toBinary(expected)
      val actual =
        serializer.fromBinary(bytes, UsernameUnknownManifest).asInstanceOf[UsernameUnknown]
      assert(actual == expected)
    }

    'userRemoved - {
      val expected   = UserRemoved(user.username)
      val serializer = findSerializer(expected)
      val bytes      = serializer.toBinary(expected)
      val actual     = serializer.fromBinary(bytes, UserRemovedManifest).asInstanceOf[UserRemoved]
      assert(actual == expected)
    }
  }

  private def findSerializer(expected: AnyRef) =
    serialization.findSerializerFor(expected).asInstanceOf[SerializerWithStringManifest]
}
