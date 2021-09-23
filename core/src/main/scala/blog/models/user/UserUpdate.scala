package blog.models.user

import blog.domain.UserId
import blog.models.User

case class UserUpdate(update: User, whichUpdate: UserId, whoUpdate: UserId)
