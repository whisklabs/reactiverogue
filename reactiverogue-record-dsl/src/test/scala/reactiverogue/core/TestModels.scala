package reactiverogue.core

import reactivemongo.bson.BSONObjectID
import reactiverogue.record._
import reactiverogue.record.field._
import JsonFormats._

/////////////////////////////////////////////////
// Sample records for testing
/////////////////////////////////////////////////

object VenueStatus extends Enumeration {
  val open = Value("Open")
  val closed = Value("Closed")
}

class Venue extends MongoRecord[Venue] {
  def meta = Venue
  object legacyid extends LongField(this) { override def name = "legid" }
  object userid extends LongField(this)
  object venuename extends StringField(this)
  object mayor extends LongField(this)
  object mayor_count extends LongField(this)
  object closed extends BooleanField(this)
  object tags extends MongoListField[Venue, String](this)
  object popularity extends MongoListField[Venue, Long](this)
  object categories extends MongoListField[Venue, BSONObjectID](this)
  object geolatlng extends JsObjectField[Venue, LatLong](this) { override def name = "latlng" }
  object last_updated extends DateField(this)
  object status extends EnumNameField(this, VenueStatus) { override def name = "status" }
  object claims extends BsonRecordListField(this, VenueClaimBson) {
    override def zero = VenueClaimBson.createRecord
  }
  object lastClaim extends BsonRecordField(this, VenueClaimBson)
  object zipCode extends OptionalStringField(this)
}
object Venue extends Venue with MongoMetaRecord[Venue] {
  override def collectionName = "venues"
}

object ClaimStatus extends Enumeration {
  val pending = Value("Pending approval")
  val approved = Value("Approved")
}

object RejectReason extends Enumeration {
  val tooManyClaims = Value("too many claims")
  val cheater = Value("cheater")
  val wrongCode = Value("wrong code")
}

class VenueClaim extends MongoRecord[VenueClaim] {
  def meta = VenueClaim
  object userid extends LongField(this) { override def name = "uid" }
  object status extends EnumNameField(this, ClaimStatus)
  object reason extends EnumField(this, RejectReason)
  object date extends DateField(this)
  object venueid extends ObjectIdField(this)
}
object VenueClaim extends VenueClaim with MongoMetaRecord[VenueClaim] {
  override def fieldOrder = List(status, id, userid, venueid, reason)
  override def collectionName = "venueclaims"
}

class VenueClaimBson extends BsonRecord[VenueClaimBson] {
  def meta = VenueClaimBson
  object userid extends LongField(this) { override def name = "uid" }
  object status extends EnumNameField(this, ClaimStatus)
  object source extends BsonRecordField(this, SourceBson)
  object date extends DateField(this)
}
object VenueClaimBson extends VenueClaimBson with BsonMetaRecord[VenueClaimBson] {
  override def fieldOrder = List(status, userid, source, date)
}

class SourceBson extends BsonRecord[SourceBson] {
  def meta = SourceBson
  object name extends StringField(this)
  object url extends StringField(this)
}
object SourceBson extends SourceBson with BsonMetaRecord[SourceBson] {
  override def fieldOrder = List(name, url)
}

case class OneComment(timestamp: String, userid: Long, comment: String)
class Comment extends MongoRecord[Comment] {
  def meta = Comment
  object comments extends JsObjectListField[Comment, OneComment](this)
}
object Comment extends Comment with MongoMetaRecord[Comment] {
  override def collectionName = "comments"
}

class Tip extends MongoRecord[Tip] {
  def meta = Tip
  object legacyid extends LongField(this) { override def name = "legid" }
  object counts extends MongoMapField[Tip, Long](this)
  object userid extends LongField(this)
}
object Tip extends Tip with MongoMetaRecord[Tip] {
  override def collectionName = "tips"
}

class Like extends MongoRecord[Like] with Sharded {
  def meta = Like
  object userid extends LongField(this) /*with ShardKey[Long]*/
  object checkin extends LongField(this)
  object tip extends ObjectIdField(this)
}
object Like extends Like with MongoMetaRecord[Like] {
  override def collectionName = "likes"
}

object ConsumerPrivilege extends Enumeration {
  val awardBadges = Value("Award badges")
}

//class OAuthConsumer extends MongoRecord[OAuthConsumer] with ObjectIdKey[OAuthConsumer] {
//  def meta = OAuthConsumer
//  object privileges extends MongoListField[OAuthConsumer, ConsumerPrivilege.Value](this)
//}
//object OAuthConsumer extends OAuthConsumer with MongoMetaRecord[OAuthConsumer] {
//  override def collectionName = "oauthconsumers"
//  override def mongoIdentifier = RogueTestMongo
//}

// Used for selectCase tests.
case class V1(legacyid: Long)
case class V2(legacyid: Long, userid: Long)
case class V3(legacyid: Long, userid: Long, mayor: Long)
case class V4(legacyid: Long, userid: Long, mayor: Long, mayor_count: Long)
case class V5(legacyid: Long, userid: Long, mayor: Long, mayor_count: Long, closed: Boolean)
case class V6(legacyid: Long, userid: Long, mayor: Long, mayor_count: Long, closed: Boolean, tags: List[String])

class CalendarFld private () extends MongoRecord[CalendarFld] with ObjectIdPk[CalendarFld] {
  def meta = CalendarFld

  object inner extends BsonRecordField(this, CalendarInner)
}

object CalendarFld extends CalendarFld with MongoMetaRecord[CalendarFld] {
  override def collectionName = "calendar_fld"
}

class CalendarInner private () extends BsonRecord[CalendarInner] {
  def meta = CalendarInner

  object date extends DateField(this) //actually calendar field, not joda DateTime
}

object CalendarInner extends CalendarInner with BsonMetaRecord[CalendarInner]

