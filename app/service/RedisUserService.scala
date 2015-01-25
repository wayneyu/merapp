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
package service

import play.api.Logger
import play.api.Play.current
import securesocial.core._
import securesocial.core.providers.{MailToken, UsernamePasswordProvider}
import securesocial.core.services.{SaveMode, UserService}

import scala.concurrent.Future
import play.api.cache.Cache

import scala.concurrent.ExecutionContext.Implicits._

class RedisUserService extends UserService[User] {
  val logger = Logger("application.controllers.InMemoryUserService")

  val userColletionKey = "users"
  val tokensColletionKey = "tokens"

  private def setUserById(userKey: UserKey, user: User) = {
    Cache.set(userKey.key, user)
    for (identity <- user.identities) {
      linkProfileToUser(ProfileKey(identity), user)
      EmailKey(identity) match {
        case Some(key) => linkEmailToUser(key, user)
        case None =>
      }
    }
  }

  private def addUser(user: User) = {
    val users = Cache.getAs[List[String]](userColletionKey)
    val updatedUsers = users match {
      case Some(l) => user.userkey::l
      case None => List(user.userkey)
    }
    Cache.set(userColletionKey, updatedUsers)
  }

  private def getUserById(userKey: UserKey): Option[User] = {
    getUserById(userKey.key)
  }

  private def getUserById(userKey: String): Option[User] = {
    Logger.info("getUserById: " + userKey)
    Cache.getAs[User](userKey)
  }

  private def getUserByProfile(profile: BasicProfile): Option[User] = {
    val userKey = Cache.getAs[String](ProfileKey(profile).key)
    userKey.flatMap[User]( getUserById )
  }

  private def getUserByEmail(emailKey: EmailKey): Option[User] = {
    val userKey = Cache.getAs[String](emailKey.key)
    userKey match {
      case Some(s) => Logger.info("getUserByEmail: found userKey: " + userKey)
      case None => Logger.info("no userkey found for email: " + emailKey.key)
    }
    userKey.flatMap[User]( getUserById )
  }

  private def linkProfileToUser(profileKey: ProfileKey, user: User) = {
    Cache.set(profileKey.key, UserKey(user).key)
  }

  private def linkEmailToUser(emailKey: EmailKey, user: User) = {
    Logger.info("linkEmailToUser: " + emailKey.key + "->" + UserKey(user).key)
    Cache.set(emailKey.key, UserKey(user).key)
  }

  def find(providerId: String, userId: String): Future[Option[BasicProfile]] = {
    Logger.info("find " + providerId + " " + userId)
    val result = getUserById(UserKey(providerId, userId)).map[BasicProfile](_.main)
    Future.successful(result)
  }

  def findByEmailAndProvider(email: String, providerId: String): Future[Option[BasicProfile]] = {
    Logger.info("findByEmailAndProvider " + email + " " + providerId)
    val result = getUserByEmail(EmailKey(providerId, email)).map[BasicProfile](_.main)
    Logger.info(result match {
      case Some(p) => p.userId
      case None => "user not found"
    })
    Future.successful(result)
  }

  private def findProfile(p: BasicProfile): Option[User] = {
    getUserByProfile(p)
  }

  private def updateProfile(profile: BasicProfile, user: User): Future[User] = {
    Logger.info("updateProfile should be: before: " + user + " after: " + profile )
    val identities = user.identities
    val idx = identities.indexWhere(i => i.providerId == profile.providerId && i.userId == profile.userId)
    val updatedList = idx match {
      case -1 => profile::identities
      case _ => identities.patch(idx, Seq(profile), 1)
    }
    val updatedMain = user.main match {
      case BasicProfile(profile.providerId, profile.userId, _, _, _, _, _, _, _, _, _) => profile
      case _ => user.main
    }
    val updatedUser = user match {
      case u: Visitor => Visitor(updatedMain, updatedList)
      case u: Contributor => Contributor(updatedMain, updatedList)
      case u: SuperUser => SuperUser(updatedMain, updatedList)
    }
    setUserById(user.userkey, updatedUser)
    Logger.info("updateProfile for User: " + user + ". with user: " + updatedUser)
    Future.successful(updatedUser)
  }

  def save(userProfile: BasicProfile, mode: SaveMode): Future[User] = {
    mode match {
      case SaveMode.SignUp =>
        val newUser = Visitor(userProfile, List(userProfile)) // default to visitor, users are manually promoted by SuperUser
        setUserById(newUser.userkey, newUser)
        addUser(newUser)
        Future.successful(newUser)
      case SaveMode.LoggedIn =>
        // first see if there is a user with this BasicProfile already.
        findProfile(userProfile) match {
          case Some(existingUser) =>
            updateProfile(userProfile, existingUser)
          case None =>
            val newUser = Visitor(userProfile, List(userProfile))
            setUserById(UserKey(userProfile.providerId, userProfile.userId), newUser)
            addUser(newUser)
            Future.successful(newUser)
        }

      case SaveMode.PasswordChange =>
        findProfile(userProfile).map { user =>
          updateProfile(userProfile, user) }.getOrElse(
          // this should not happen as the profile will be there
          throw new Exception("missing profile)")
        )
    }
  }

  def link(current: User, to: BasicProfile): Future[User] = {
    updateProfile(to, current)
  }

  def saveToken(token: MailToken): Future[MailToken] = {
    Future.successful{
      Cache.set(token.uuid, token)
      val list = Cache.getAs[List[String]](tokensColletionKey)
      val updatedList = list match {
        case Some(l) => token.uuid::l
        case None => List(token.uuid)
      }
      Cache.set(tokensColletionKey, updatedList )
      token
    }
  }

  def findToken(token: String): Future[Option[MailToken]] = {
    Future.successful{
      Cache.getAs[MailToken](token)
    }
  }

  def deleteToken(uuid: String): Future[Option[MailToken]] = {
    findToken(uuid).map{
      case Some(token) =>
        Cache.remove(uuid)
        Some(token)
      case None => None
    }
  }

  def deleteExpiredTokens() {
    val tokens = Cache.getAs[List[String]](tokensColletionKey)
    val updatedTokens = tokens match {
      case Some(l) => l.filter( uuid => !Cache.getAs[MailToken](uuid).get.isExpired )
      case None =>
    }
    Cache.set(tokensColletionKey, updatedTokens)
  }

  override def updatePasswordInfo(user: User, info: PasswordInfo): Future[Option[BasicProfile]] = {
    Future.successful {
      val found = getUserById(user.userkey)
      Logger.info("updating password for user: " + found.get.toString)
      found.flatMap[BasicProfile]{ user =>
        user.identities.find(_.providerId == UsernamePasswordProvider.UsernamePassword)
      }.map{ profile =>
        val idx = user.identities.indexOf(profile)
        val updatedProfile = profile.copy(passwordInfo = Some(info))
        updateProfile(updatedProfile, user)
        updatedProfile
      }
    }
  }

  override def passwordInfoFor(user: User): Future[Option[PasswordInfo]] = {
    Future.successful {
      val found = getUserById(user.userkey)
      found.flatMap[BasicProfile] {
        _.identities.find(_.providerId == UsernamePasswordProvider.UsernamePassword)
      }.flatMap{ _.passwordInfo}
    }
  }
}

trait User extends Serializable{
  val SerialVersionUID: Long
  val main: BasicProfile
  val identities: List[BasicProfile]
  val accessMode: String
  def userkey: UserKey = UserKey(this)
  def emailkey: Option[EmailKey] = EmailKey(this)
}

case class Visitor(main: BasicProfile, identities: List[BasicProfile]) extends User{
  val SerialVersionUID = -1037302500704416308L
  val accessMode = "Visitor"
}
case class Contributor(main: BasicProfile, identities: List[BasicProfile]) extends User{
  val SerialVersionUID = -1037302500704416309L
  val accessMode = "Contributor"
}
case class SuperUser(main: BasicProfile, identities: List[BasicProfile]) extends User{
  val SerialVersionUID = -1037302500704416310L
  val accessMode = "SuperUser"
}


trait Key {
  def key: String
}

// use this class's hashCode to generate cache key
case class UserKey(providerId: String, userId: String) extends Key {
  def key = "u_" + providerId + "_" + userId
}
object UserKey{
  def apply(user: BasicProfile): UserKey = UserKey(user.providerId, user.userId)
  def apply(user: User): UserKey = apply(user.main)
}
case class EmailKey(providerId: String, email: String) extends Key {
  def key = "e_" + providerId + "_" + email
}
object EmailKey{
  def apply(user: BasicProfile): Option[EmailKey] = user.email.map[EmailKey]{ EmailKey(user.providerId, _) }
  def apply(user: User): Option[EmailKey] = apply(user.main)
}
case class ProfileKey(providerId: String, userId: String) extends Key {
  def key = "p_" + providerId + "_" + userId
}
object ProfileKey{
  def apply(user: BasicProfile): ProfileKey = ProfileKey(user.providerId, user.userId)
}