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

import cats.data.{ NonEmptyList, Validated }
import eu.timepit.refined.api.Refined
import eu.timepit.refined.boolean.And
import eu.timepit.refined.char.LetterOrDigit
import eu.timepit.refined.collection.{ Forall, NonEmpty }
import eu.timepit.refined.refineV

object User {

  type Username           = Refined[String, UsernameRefinement]
  type UsernameRefinement = And[NonEmpty, Forall[LetterOrDigit]]

  type Nickname           = Refined[String, NicknameRefinement]
  type NicknameRefinement = NonEmpty

  def apply(username: String, nickname: String): Validated[NonEmptyList[String], User] = {
    import cats.syntax.apply._
    import cats.syntax.either._
    val validatedUsername = refineUsername(username).toValidatedNel
    val validatedNickname = refineNickname(nickname).toValidatedNel
    (validatedUsername, validatedNickname).mapN(new User(_, _))
  }

  def refineUsername(username: String): Either[String, Username] =
    refineV[UsernameRefinement](username)

  def refineNickname(nickname: String): Either[String, Nickname] =
    refineV[NicknameRefinement](nickname)
}

final case class User(username: User.Username, nickname: User.Nickname)
