package blog.dsls

import blog.dsls.combined.CreateUpdateDelete
import blog.models.user.{UserCreate, UserDelete, UserUpdate}

trait Users[F[_]]
    extends CreateUpdateDelete[F, UserCreate, UserUpdate, UserDelete]
