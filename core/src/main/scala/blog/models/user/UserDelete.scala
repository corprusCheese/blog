package blog.models.user

import blog.domain.UserId

case class UserDelete (userId: UserId, whoDelete: UserId)
