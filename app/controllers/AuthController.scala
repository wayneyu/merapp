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
import play.api.data.Form
import play.api.mvc._
import securesocial.core._
import service._
import scala.concurrent.ExecutionContext.Implicits.global

import scala.concurrent.Future

object AuthController extends ServiceComponent{

  def userprofile(user: User) = SuperUserAction { implicit context =>
	  implicit val environ: RuntimeEnvironment[User] = env
	  implicit val request: Request[AnyContent] = context.request
    Ok(views.html.userprofile(Some(user)))
  }

	def userprofile = VisitorAction.async { implicit context =>
		implicit val environ: RuntimeEnvironment[User] = env
		implicit val request: Request[AnyContent] = context.request
		Future(Ok(views.html.userprofile(None)))
	}

  // a sample action using an authorization implementation
//  def onlyTwitter = SecuredAction(WithProvider("google")) { implicit request =>
//    Ok("You can see this because you logged in using Twitter")
//  }

  def linkResult = VisitorAction.async { implicit context =>
    Future(Ok(views.html.linkResult()))
  }

	def users = SuperUserAction.async { implicit context =>
		val users = env.userService.getUsers.getOrElse(Nil)
		Future(Ok(views.html.users(users)))
	}

	def modifyUserType(userId: String, provider: String, userType: String) = SuperUserAction.async { implicit context =>
		val res = env.userService.modifyUserType(userId, provider, userType)
		res.map {
			s => Ok("Modify user with userId: " + userId + " to " + userType + ": " + s)
		}
	}

	def modifyUserSubmit() = SuperUserAction.async { implicit request =>
		val form = request.body.asFormUrlEncoded
		form match {
			case Some(map) => {
				env.userService.modifyUserType(
					map.getOrElse("userId", Seq(""))(0),
					map.getOrElse("providerId", Seq(""))(0),
					map.getOrElse("userType", Seq(""))(0)).map{
					s => Redirect(controllers.routes.AuthController.users())
				}
			}
			case None => Future(BadRequest("Modify user type failed: no data submitted."))
		}
	}

//	def backupUsers() = SecuredAction

}

// An Authorization implementation that only authorizes uses that logged in using twitter
case class WithProvider(provider: String) extends Authorization[User] {
  def isAuthorized(user: User, request: RequestHeader) = {
    user.main.providerId == provider
  }
}
