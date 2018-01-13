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

import akka.actor.CoordinatedShutdown.Reason
import akka.actor.{ ActorSystem, CoordinatedShutdown }
import akka.actor.typed.{ ActorRef, Behavior, Terminated }
import akka.actor.typed.scaladsl.Actor
import akka.stream.{ ActorMaterializer, Materializer }
import org.apache.logging.log4j.core.async.AsyncLoggerContextSelector
import org.apache.logging.log4j.scala.Logging
import pureconfig.loadConfigOrThrow

object Main extends Logging {
  import akka.actor.typed.scaladsl.adapter._

  private final case class TopLevelActorTerminated(actor: ActorRef[Nothing]) extends Reason

  final case class Config()

  def main(args: Array[String]): Unit = {
    sys.props += "log4j2.contextSelector" -> classOf[AsyncLoggerContextSelector].getName

    val config                     = loadConfigOrThrow[Config]("wta")
    val system                     = ActorSystem("wta")
    implicit val mat: Materializer = ActorMaterializer()(system)

    // Nothing not inferred here and below, see github.com/scala/bug/issues/9453
    system.spawn[Nothing](Main(config, CoordinatedShutdown(system)), "main")
  }

  def apply(config: Config, coordinatedShutdown: CoordinatedShutdown)(
      implicit mat: Materializer
  ): Behavior[Nothing] =
    Actor.deferred[Nothing] { context =>
      logger.info(s"${context.system.name} up and running")

      Actor.onSignal[Nothing] {
        case (_, Terminated(actor)) =>
          logger.error(s"Shutting down, because actor ${actor.path} terminated!")
          coordinatedShutdown.run(TopLevelActorTerminated(actor))
          Actor.empty
      }
    }
}
