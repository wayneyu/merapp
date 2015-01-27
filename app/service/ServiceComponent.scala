package service

import controllers.CustomRoutesService
import securesocial.core.RuntimeEnvironment

/**
 * Created by wayneyu on 1/25/15.
 */
trait ServiceComponent {
  object AuthRuntimeEnvironment extends RuntimeEnvironment.Default[User] {
    override lazy val routes = new CustomRoutesService()
    override lazy val userService: RedisUserService = new RedisUserService()
    override lazy val eventListeners = List(new AuthEventListener())
  }
}
