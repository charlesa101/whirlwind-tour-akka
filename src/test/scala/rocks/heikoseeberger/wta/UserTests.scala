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

import cats.data.Validated.Valid
import eu.timepit.refined.auto.autoRefineV
import utest._

object UserTests extends TestSuite {
  import User._

  override val tests = Tests {
    'applyError - {
      val result    = User("", "")
      val errorSize = result.fold(_.size, _ => -1)
      assert(errorSize == 2)
    }

    'apply - {
      val result   = User("username", "nickname")
      val expected = Valid(User("username": Username, "nickname": Nickname))
      assert(result == expected)
    }

    'refineUsernameError - {
      assert(refineUsername("").isLeft)
      assert(refineUsername("1-2").isLeft)
    }

    'refineUsername - {
      val username = refineUsername("username").map(_.value)
      val expected = Right("username")
      assert(username == expected)
    }

    'refineNicknameError - {
      assert(refineNickname("").isLeft)
    }

    'refineNickname - {
      val nickname = refineUsername("nickname").map(_.value)
      val expected = Right("nickname")
      assert(nickname == expected)
    }
  }
}
