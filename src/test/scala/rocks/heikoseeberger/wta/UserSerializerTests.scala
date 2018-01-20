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

object UserSerializerTests extends ActorSystemTests {
  import User._
  import UserSerializer._

  private val serialization = SerializationExtension(system)

  override def tests = Tests {
    'user - {
      val expected = User("username": Username, "nickname": Nickname)
      val serializer =
        serialization.findSerializerFor(expected).asInstanceOf[SerializerWithStringManifest]
      val bytes  = serializer.toBinary(expected)
      val actual = serializer.fromBinary(bytes, UserManifest).asInstanceOf[User]
      assert(actual == expected)
    }
  }
}
