package controllers

import play.Logger
import play.api.mvc.{RequestHeader, Action, AnyContent}
import securesocial.controllers.BaseLoginPage
import securesocial.core.{IdentityProvider, RuntimeEnvironment}
import securesocial.core.services.RoutesService
import service.User

class CustomLoginController(implicit override val env: RuntimeEnvironment[User]) extends BaseLoginPage[User] {
  override def login: Action[AnyContent] = {
    Logger.debug("using CustomLoginController")
    super.login
  }
}

class CustomRoutesService extends RoutesService.Default {
  override def loginPageUrl(implicit req: RequestHeader): String = controllers.routes.CustomLoginController.login().absoluteURL(IdentityProvider.sslEnabled)
}