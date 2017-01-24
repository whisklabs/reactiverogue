package reactiverogue.record

import reactivemongo.api.DefaultDB
import reactivemongo.api.commands.{WriteConcern, WriteResult}
import reactiverogue.record.field.ObjectIdPk

import scala.concurrent.{ExecutionContext, Future}

trait MongoRecord[MyType <: MongoRecord[MyType]]
    extends BsonRecord[MyType]
    with ObjectIdPk[MyType] { self: MyType =>

  /**
    * The meta record (the object that contains the meta result for this type)
    */
  def meta: MongoMetaRecord[MyType]

  /**
    * Save the instance and return the instance
    */
  def save(concern: WriteConcern)(implicit ec: ExecutionContext,
                                  db: DefaultDB): Future[WriteResult] = {
    meta.save(this, concern)
  }

  /**
    * Save the instance and return the instance
    * WILL NOT RAISE MONGO SERVER ERRORS.
    * Use save(Boolean) or save(GetLastError) to control error behavior
    */
  def save(implicit ec: ExecutionContext, db: DefaultDB): Future[WriteResult] =
    save(WriteConcern.Default)

  /**
    * Delete the instance from backing store
    */
  def delete_!(implicit ec: ExecutionContext, db: DefaultDB): Future[WriteResult] =
    meta.delete_!(this)
}
