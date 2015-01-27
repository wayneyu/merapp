/**
 * Copyright 2014 Jorge Aliss (jaliss at gmail dot com) - twitter: @jaliss
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
 *
 */
import java.lang.reflect.Constructor

import controllers.{ServiceComponent, CustomRoutesService}
import org.sedis.Dress
import play._
import securesocial.core.{AuthenticationMethod, RuntimeEnvironment}
import service.{User, RedisUserService, AuthEventListener}

import play.api.Play.current
import redis.clients.jedis._
import com.typesafe.plugin.RedisPlugin


object Global extends play.api.GlobalSettings with ServiceComponent{

  /**
   * The runtime environment for this sample app.
   */
//  object AuthRuntimeEnvironment extends RuntimeEnvironment.Default[User] {
//    override lazy val routes = new CustomRoutesService()
//    override lazy val userService: RedisUserService = new RedisUserService()
//    override lazy val eventListeners = List(new AuthEventListener())
//  }


  /**
   * An implementation that checks if the controller expects a RuntimeEnvironment and
   * passes the instance to it if required.
   *
   * This can be replaced by any DI framework to inject it differently.
   *
   * @param controllerClass
   * @tparam A
   * @return
   */
  override def getControllerInstance[A](controllerClass: Class[A]): A = {
    val instance = controllerClass.getConstructors.find { c =>
      val params = c.getParameterTypes
      params.length == 1 && params(0) == classOf[RuntimeEnvironment[User]]
    }.map {
      _.asInstanceOf[Constructor[A]].newInstance(AuthRuntimeEnvironment)
    }
    instance.getOrElse(super.getControllerInstance(controllerClass))
  }


//  override def onStart(app: Application) = {
//    app.plugin[RedisPlugin].
//    val pool = use[RedisPlugin].sedisPool
//    pool.withJedisClient { client =>
//      Option[String] single = Dress.up(client).get("single")
//    }
//  }

}
