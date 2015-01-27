package controllers

import play.Logger
import play.api.mvc.{RequestHeader, Action, AnyContent}
import securesocial.controllers.BaseLoginPage
import securesocial.core.{IdentityProvider, RuntimeEnvironment}
import securesocial.core.services.RoutesService
import service.{ServiceComponent, User}

trait CustomLoginController extends BaseLoginPage[User] with ServiceComponent {

  override implicit val env = AuthRuntimeEnvironment

  override def login: Action[AnyContent] = {
    Logger.debug("using CustomLoginController")
    super.login
  }
}

class CustomRoutesService extends RoutesService.Default {
  override def loginPageUrl(implicit req: RequestHeader): String = controllers.routes.CustomLoginController.login().absoluteURL(IdentityProvider.sslEnabled)
}

object CustomLoginController extends CustomLoginController