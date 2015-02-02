package service

import controllers.CustomRoutesService
import play.api.mvc._
import securesocial.core.providers.{UsernamePasswordProvider, GoogleProvider, FacebookProvider}
import securesocial.core.{SecureSocial, RuntimeEnvironment}
import scala.collection.immutable.ListMap
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
		override lazy val providers = ListMap(
			// TODO support facebook
//			include(new FacebookProvider(routes, cacheService, oauth2ClientFor(FacebookProvider.Facebook))),
			// TODO support Twitter
//			include(new TwitterProvider(routes, cacheService, oauth1ClientFor(TwitterProvider.Twitter))),
			include(new GoogleProvider(routes, cacheService, oauth2ClientFor(GoogleProvider.Google))),
			include(new UsernamePasswordProvider[User](userService, avatarService, viewTemplates, passwordHashers))
		)
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
			//TODO parameterize SecuredActions with User type
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
				})
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
				})
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

}

trait AppContext[A]{
	val user : Option[User]
	val request: Request[A]
}





trait ServiceComponent extends AuthService