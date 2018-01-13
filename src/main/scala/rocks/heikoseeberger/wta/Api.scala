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
import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl.Actor
import akka.http.scaladsl.Http
import akka.http.scaladsl.Http.ServerBinding
import akka.http.scaladsl.server.{ Directives, Route }
import akka.stream.Materializer
import de.heikoseeberger.akkahttpcirce.ErrorAccumulatingCirceSupport
import java.net.InetSocketAddress
import org.apache.logging.log4j.scala.Logging
import scala.util.{ Failure, Success }

object Api extends Logging {

  sealed trait Command
  private final case object HandleBindFailure                      extends Command
  private final case class HandleBound(address: InetSocketAddress) extends Command

  final case class Config(address: String, port: Int)

  final val Name = "api"

  def apply(config: Config)(implicit mat: Materializer): Behavior[Command] = {
    import config._

    Actor.deferred { context =>
      import context.executionContext

      implicit val s: ActorSystem = {
        import akka.actor.typed.scaladsl.adapter._
        context.system.toUntyped
      }
      val self = context.self

      Http()
        .bindAndHandle(route, address, port)
        .onComplete {
          case Failure(_)                      => self ! HandleBindFailure
          case Success(ServerBinding(address)) => self ! HandleBound(address)
        }

      Actor.immutable {
        case (_, HandleBindFailure) =>
          logger.error(s"Stopping, because cannot bind to $address:$port!")
          Actor.stopped

        case (_, HandleBound(address)) =>
          logger.info(s"Bound to $address")
          Actor.ignore
      }
    }
  }

  def route: Route = {
    import Directives._
    import ErrorAccumulatingCirceSupport._
    import io.circe.generic.auto._
    import io.circe.refined._

    val usernameMatcher = Segment.flatMap(s => User.refineUsername(s).toOption)

    pathEndOrSingleSlash {
      get {
        complete {
          "GET received"
        }
      } ~
      post {
        entity(as[User]) { user =>
          complete {
            s"POST $user received"
          }
        }
      }
    } ~
    path(usernameMatcher) { username =>
      delete {
        complete {
          s"DELETE $username received"
        }
      }
    }
  }
}
