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

import akka.actor.ActorSystem
import akka.actor.typed.{ ActorSystem => TypedActorSystem }
import scala.concurrent.Await
import scala.concurrent.duration.DurationInt
import utest.TestSuite

trait ActorSystemTests extends TestSuite {
  import akka.actor.typed.scaladsl.adapter._

  protected implicit val system: ActorSystem = ActorSystem()

  protected implicit val typedSystem: TypedActorSystem[Nothing] = system.toTyped

  override def utestAfterAll() = {
    Await.ready(system.terminate(), 42.seconds)
    super.utestAfterAll()
  }
}
