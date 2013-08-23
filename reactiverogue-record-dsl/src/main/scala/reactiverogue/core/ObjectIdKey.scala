// Copyright 2013 Foursquare Labs Inc. All Rights Reserved.

package reactiverogue.core

import reactiverogue.record.MongoRecord
import reactiverogue.record.field.ObjectIdField
import reactivemongo.bson.BSONObjectID

/**
 * Mix this into a Record to add an ObjectIdField
 */
trait ObjectIdKey[OwnerType <: MongoRecord[OwnerType]] {
  self: OwnerType =>

  object _id extends ObjectIdField(this.asInstanceOf[OwnerType])

  // convenience method that returns the value of _id
  def id: BSONObjectID = _id.value
}