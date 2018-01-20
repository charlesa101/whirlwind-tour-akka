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

import akka.actor.{ Actor, ActorLogging, ActorSystem, Props, Terminated }
import akka.actor.typed.SupervisorStrategy.restartWithBackoff
import akka.actor.typed.scaladsl.Actor.supervise
import akka.persistence.cassandra.query.scaladsl.CassandraReadJournal
import akka.persistence.query.PersistenceQuery
import akka.stream.{ ActorMaterializer, Materializer }
import org.apache.logging.log4j.core.async.AsyncLoggerContextSelector
import pureconfig.loadConfigOrThrow
import scala.concurrent.duration.FiniteDuration

object Main {

  final class Root(config: Config) extends Actor with ActorLogging {
    import akka.actor.typed.scaladsl.adapter._

    private implicit val mat: Materializer = ActorMaterializer()

    private val userRepository = context.spawn(UserRepository(), UserRepository.Name)

    private val userView = context.spawn(UserView(), UserView.Name)

    private val userProjection = {
      val readJournal =
        PersistenceQuery(context.system)
          .readJournalFor[CassandraReadJournal](CassandraReadJournal.Identifier)
      val userProjection =
        supervise(UserViewProjection(config.userViewProjection, readJournal, userView))
          .onFailure[UserViewProjection.EventStreamCompleteException](
            restartWithBackoff(config.userViewProjectionMinBackoff,
                               config.userViewProjectionMaxBackoff,
                               0)
          )
      context.spawn(userProjection, UserViewProjection.Name)
    }

    private val api = context.spawn(Api(config.api, userRepository, userView), Api.Name)

    context.watch(api)
    context.watch(userView)
    context.watch(userProjection)

    log.info(s"${context.system.name} up and running")

    override def receive = {
      case Terminated(actor) =>
        log.error(s"Shutting down, because actor ${actor.path} terminated!")
        context.system.terminate()
    }
  }

  final case class Config(userViewProjectionMinBackoff: FiniteDuration,
                          userViewProjectionMaxBackoff: FiniteDuration,
                          api: Api.Config,
                          userViewProjection: UserViewProjection.Config)

  def main(args: Array[String]): Unit = {
    sys.props += "log4j2.contextSelector" -> classOf[AsyncLoggerContextSelector].getName

    val config = loadConfigOrThrow[Config]("wta")
    val system = ActorSystem("wta")

    system.actorOf(Props(new Root(config)), "root")
  }
}
