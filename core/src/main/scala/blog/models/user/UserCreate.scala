package blog.models.user

import blog.domain.UserId
import blog.models.User

case class UserCreate (newUser: User, whoCreate: UserId)
