package blog.storage.combined

import blog.storage.actions._

/**
  * trait for automation of crud operations
  * @tparam F is effect
  * @tparam C is class for creation
  * @tparam U is class for update
  * @tparam D is class for delete
 */
trait CreateUpdateDelete[F[_], C, U, D]
    extends Create[F, C]
    with Update[F, U]
    with Delete[F, D]
