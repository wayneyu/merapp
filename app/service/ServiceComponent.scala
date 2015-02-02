package service

import controllers.CustomRoutesService
import play.api.mvc._
import securesocial.core.{SecureSocial, RuntimeEnvironment}
import scala.concurrent.ExecutionContext.Implicits.global

import scala.concurrent.Future
import scala.reflect.runtime.universe._

/**
 * Created by wayneyu on 1/25/15.
 */
trait AuthService extends securesocial.core.SecureSocial[User]{

	override implicit val env = AuthRuntimeEnvironment

	val SUPERUSER_ACTION_ERROR = "Service is restricted to SuperUser only."
	val CONTRIBUTOR_ACTION_ERROR  = "Service is restricted to Contributor and SuperUser only."
	val userAwareActionBuilder = new UserAwareActionBuilder()
	val securedActionBuilder = new SecuredActionBuilder()

	object AuthRuntimeEnvironment extends RuntimeEnvironment.Default[User] {
    override lazy val routes = new CustomRoutesService()
    override lazy val userService: RedisUserService = new RedisUserService()
    override lazy val eventListeners = List(new AuthEventListener())
  }

	class UserAwaredActionBuilder extends ActionBuilder[({ type R[A] = AuthContext[A] })#R] {
		override def invokeBlock[A](request: Request[A], block: (AuthContext[A]) => Future[Result]): Future[Result] = {
			userAwareActionBuilder.invokeBlock[A](request, {
				implicit request =>
					block(AuthContext(request.user, request))
			})
		}
	}
	object UserAwaredAction extends UserAwaredActionBuilder {
		def apply[A]() = new UserAwaredActionBuilder()
	}

	class SuperUserActionBuilder extends ActionBuilder[({ type R[A] = AuthContext[A] })#R] {
		override def invokeBlock[A](request: Request[A], block: (AuthContext[A]) => Future[Result]): Future[Result] = {
//			def meth[A : TypeTag](xs: List[A]) = typeOf[UT] match {
//				case t if t =:= typeOf[UT] => "list of strings"
//				case t if t <:< typeOf[Foo] => "list of foos"
//			}
			securedActionBuilder.invokeBlock[A]( request,
				{ implicit request =>
					implicit val user = Some(request.user)
					request.user match {
						case _:SuperUser => block(AuthContext(user, request))
						case _:User => Future(Unauthorized(SUPERUSER_ACTION_ERROR))
					}
				}
			)
		}
	}

	object SuperUserAction extends SuperUserActionBuilder {
		def apply[A]() = new SuperUserActionBuilder()
	}

	class ContributorActionBuilder extends ActionBuilder[({ type R[A] = AuthContext[A] })#R] {
		override def invokeBlock[A](request: Request[A], block: (AuthContext[A]) => Future[Result]): Future[Result] = {
			securedActionBuilder.invokeBlock[A]( request,
				{ implicit request =>
					implicit val user = Some(request.user)
					request.user match {
						case _ @ (_:SuperUser | _:Contributor) => block(AuthContext(user, request))
						case _:User => Future(Unauthorized(CONTRIBUTOR_ACTION_ERROR))
					}
				}
			)
		}
	}
	object ContributorAction extends ContributorActionBuilder {
		def apply[A]() = new ContributorActionBuilder()
	}

	class VisitorActionBuilder extends ActionBuilder[({ type R[A] = AuthContext[A] })#R] {
		override def invokeBlock[A](request: Request[A], block: (AuthContext[A]) => Future[Result]): Future[Result] = {
			securedActionBuilder.invokeBlock[A]( request,
				{ implicit request =>
					implicit val user = Some(request.user)
					request.user match {
						case _: User => block(AuthContext(user, request))
					}
				}
			)
		}
	}
	object VisitorAction extends VisitorActionBuilder {
		def apply[A]() = new VisitorActionBuilder()
	}

	case class AuthContext[A](user : Option[User], request: Request[A]) extends WrappedRequest(request) with AppContext[A]
//	case class UserAwaredContext[A](requestWithUser: RequestWithUser[A]) extends WrappedRequest(requestWithUser) with AppContext[A] {
//		val user = requestWithUser.user
//		val request = requestWithUser
//	}
//	case class SecuredContext[A](securedRequest: SecuredRequest[A]) extends WrappedRequest(securedRequest) with AppContext[A] {
//		val user = Some(securedRequest.user)
//		val request = securedRequest
//	}
//	case class ContextWithUser[A](override val user : Option[User], override val authenticator: Option[Authenticator[User]], override val request: Request[A])
//		extends RequestWithUser(user, authenticator, request) with AppContext[A]
//	case class SecuredContext[A](currentUser : Option[User], override val authenticator: Authenticator[User], override val request: Request[A])
//		extends SecuredRequest(currentUser.get, authenticator, request) with AppContext[A]

//	class SuperUserActionBuilder extends SecuredActionBuilder{
//
//		def apply[B](bp: BodyParser[B])(f: AuthContext[B] => Result): Action[B] = {
//			SecuredAction(bp) { implicit request =>
//				implicit val user = Some(request.user)
//				request.user match {
//					case _:SuperUser => f(AuthContext(user, request))
//					case _:User => Unauthorized(SUPERUSER_ACTION_ERROR)
//				}
//			}
//		}
//
//		def apply(f: AuthContext[AnyContent] => Result): Action[AnyContent] = {
//			SecuredAction { implicit request =>
//				implicit val user = Some(request.user)
//				request.user match {
//					case _:SuperUser => f(AuthContext(user, request))
//					case _:User => Unauthorized(SUPERUSER_ACTION_ERROR)
//				}
//			}
//		}
//
//		def async[B](bp: BodyParser[B])(f: AuthContext[B] => Future[Result]): Action[B] = {
//			SecuredAction.async(bp) { implicit request =>
//				implicit val user = Some(request.user)
//				request.user match {
//					case _ @ (_:Visitor | _:Contributor) => Future(Unauthorized(SUPERUSER_ACTION_ERROR))
//					case _:SuperUser => f(AuthContext(user, request))
//				}
//			}
//		}
//
//		def async(f: AuthContext[AnyContent] => Future[Result]): Action[AnyContent] = {
//			SecuredAction.async { implicit request =>
//				implicit val user = Some(request.user)
//				request.user match {
//					case _ @ (_:Visitor | _:Contributor) => Future(Unauthorized(SUPERUSER_ACTION_ERROR))
//					case _:SuperUser => f(AuthContext(user, request))
//				}
//			}
//		}
//	}



//	class ContributorActionBuilder extends SecuredActionBuilder{
//
//		def apply[A](bp: BodyParser[A])(f: AuthContext[A] => Result): Action[A] = {
//			SecuredAction(bp) { implicit request =>
//				implicit val user = Some(request.user)
//				request.user match {
//					case _:Visitor => Unauthorized(CONTRIBUTOR_ACTION_ERROR)
//					case _ @ (_:SuperUser | _:Contributor) => f(AuthContext(user, request))
//				}
//			}
//		}
//
//		def apply(f: AuthContext[AnyContent] => Result): Action[AnyContent] = {
//			SecuredAction { implicit request =>
//				implicit val user = Some(request.user)
//				request.user match {
//					case _:Visitor => Unauthorized(CONTRIBUTOR_ACTION_ERROR)
//					case _ @ (_:SuperUser | _:Contributor) => f(AuthContext(user, request))
//				}
//			}
//		}
//
//		def async[A](bp: BodyParser[A])(f: AuthContext[A] => Future[Result]): Action[A] = {
//			SecuredAction.async(bp) { implicit request =>
//				implicit val user = Some(request.user)
//				request.user match {
//					case _:Visitor => Future(Unauthorized(CONTRIBUTOR_ACTION_ERROR))
//					case _ @ (_:SuperUser | _:Contributor) => f(AuthContext(user, request))
//				}
//			}
//		}
//
//		def async(f: AuthContext[AnyContent] => Future[Result]): Action[AnyContent] = {
//			SecuredAction.async { implicit request =>
//				implicit val user = Some(request.user)
//				request.user match {
//					case _:Visitor => Future(Unauthorized(CONTRIBUTOR_ACTION_ERROR))
//					case _ @ (_:SuperUser | _:Contributor) => f(AuthContext(user, request))
//				}
//			}
//		}
//	}



//	class VisitorActionBuilder extends UserAwareActionBuilder {
//
//		def apply[A](bp: BodyParser[A])(f: AuthContext[A] => Result): Action[A] = {
//			SecuredAction(bp) { implicit request =>
//				implicit val user = Some(request.user)
//				f(AuthContext(user, request))
//			}
//		}
//
//		def apply(f: AuthContext[AnyContent] => Result): Action[AnyContent] = {
//			SecuredAction { implicit request =>
//				implicit val user = Some(request.user)
//				f(AuthContext(user, request))
//			}
//		}
//
//		def async[A](bp: BodyParser[A])(f: AuthContext[A] => Future[Result]): Action[A] = {
//			SecuredAction.async(bp) { implicit request =>
//				implicit val user = Some(request.user)
//				f(AuthContext(user, request))
//			}
//		}
//
//		def async(f: AuthContext[AnyContent] => Future[Result]): Action[AnyContent] = {
//			SecuredAction.async { implicit request =>
//				implicit val user = Some(request.user)
//				f(AuthContext(user, request))
//			}
//		}
//	}



//	class UserAwaredActionBuilder extends UserAwareActionBuilder {
//
//		def apply[A](bp: BodyParser[A])(f: AuthContext[A] => Result): Action[A] = {
//			UserAwareAction(bp) { implicit request =>
//				implicit val user = request.user
//				f(AuthContext(user, request))
//			}
//		}
//
//		def apply(f: AuthContext[AnyContent] => Result): Action[AnyContent] = {
//			UserAwareAction { implicit request =>
//				implicit val user = request.user
//				f(AuthContext(user, request))
//			}
//		}
//
//		def async[A](bp: BodyParser[A])(f: AuthContext[A] => Future[Result]): Action[A] = {
//			UserAwareAction.async(bp) { implicit request =>
//				implicit val user = request.user
//				f(AuthContext(user, request))
//			}
//		}
//
//		def async(f: AuthContext[AnyContent] => Future[Result]): Action[AnyContent] = {
//			UserAwareAction.async { implicit request =>
//				implicit val user = request.user
//				f(AuthContext(user, request))
//			}
//		}
//	}


}

trait AppContext[A]{
	val user : Option[User]
	val request: Request[A]
}





trait ServiceComponent extends AuthService