/**
 * Copyright 2012 Jorge Aliss (jaliss at gmail dot com) - twitter: @jaliss
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
package controllers

import play.api.Logger
import play.api.mvc.{Action, RequestHeader}
import securesocial.core._
import service._
import scala.concurrent.ExecutionContext.Implicits.global

import scala.concurrent.Future

trait AuthController extends securesocial.core.SecureSocial[User] with ServiceComponent{

  override implicit val env = AuthRuntimeEnvironment

  def userprofile = SecuredAction { implicit request =>
	  implicit val user = Some(request.user)
    Ok(views.html.userprofile(request.user))
  }

  // a sample action using an authorization implementation
//  def onlyTwitter = SecuredAction(WithProvider("google")) { implicit request =>
//    Ok("You can see this because you logged in using Twitter")
//  }

  def linkResult = SecuredAction { implicit request =>
	  implicit val user = Some(request.user)
    Ok(views.html.linkResult(request.user))
  }

}

// An Authorization implementation that only authorizes uses that logged in using twitter
case class WithProvider(provider: String) extends Authorization[User] {
  def isAuthorized(user: User, request: RequestHeader) = {
    user.main.providerId == provider
  }
}

object AuthController extends AuthController{

	def modifyUserType[T <: User](userKey: String, provider: String, userType: String) = SecuredAction.async { implicit request =>
		request.user match {
			case _@(_: Visitor | _: Contributor) => Future(Unauthorized("Current user does not have permission to modify user."))
			case _: SuperUser =>
				val res = env.userService.modifyUserType(userKey, provider, userType)
				res.map {
					s => Ok("Modify " + userKey + " to " + userType + ": " + s)
				}
		}
	}

}