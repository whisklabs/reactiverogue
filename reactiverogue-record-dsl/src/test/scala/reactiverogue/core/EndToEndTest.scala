package reactiverogue.core

import java.util.regex.Pattern

import org.scalatest._
import org.scalatest.time.{Millis, Seconds, Span}
import reactivemongo.bson._
import reactiverogue.core.QueryImplicits._

import scala.concurrent.Future
import scala.language.postfixOps

///**
// * Contains tests that test the interaction of Rogue with a real mongo.
// */
class EndToEndTest extends FunSuite with Matchers with TestMongoInstance with BeforeAndAfterEach {

  override implicit val patienceConfig = PatienceConfig(Span(3, Seconds), Span(10, Millis))

  def baseTestVenue(): Venue = {
    Venue.createRecord
      .legacyid(123)
      .userid(456)
      .venuename("test venue")
      .mayor(789)
      .mayor_count(3)
      .closed(false)
      .popularity(List(1L, 2L, 3L))
      .categories(List(BSONObjectID.generate))
      //      .geolatlng(LatLong(40.73, -73.98))
      .status(VenueStatus.open)
      .claims(List(VenueClaimBson.createRecord.userid(1234).status(ClaimStatus.pending),
                   VenueClaimBson.createRecord.userid(5678).status(ClaimStatus.approved)))
      .lastClaim(VenueClaimBson.createRecord.userid(5678).status(ClaimStatus.approved))
      .tags(List("test tag1", "some tag"))
  }

  def baseTestVenueClaim(vid: BSONObjectID): VenueClaim = {
    VenueClaim.createRecord
      .venueid(vid)
      .userid(123)
      .status(ClaimStatus.approved)
  }

  def baseTestTip(): Tip = {
    Tip.createRecord
      .legacyid(234)
      .counts(Map("foo" -> 1L, "bar" -> 2L))
  }

  override def afterEach() = {
    Venue.bulkDelete_!!!.futureValue
    Venue.count().futureValue should be(0)

    VenueClaim.bulkDelete_!!!.futureValue
    VenueClaim.count().futureValue should be(0)

    Like.bulkDelete_!!!.futureValue
  }

  test("Equality tests") {
    val v = baseTestVenue()
    v.save.futureValue
    val vc = baseTestVenueClaim(v.id.value)
    vc.save.futureValue

    // eqs
    metaRecordToQueryBuilder(Venue)
      .where(_.id eqs v.id.value)
      .fetch()
      .futureValue
      .map(_.id.value) shouldBe List(v.id.value)
    Venue.where(_.mayor eqs v.mayor.value).fetch().futureValue.map(_.id.value) shouldBe List(
      v.id.value)
    Venue.where(_.mayor eqs v.mayor.value).fetch().futureValue.map(_.id.value) shouldBe List(
      v.id.value)
    Venue
      .where(_.venuename eqs v.venuename.value)
      .fetch()
      .futureValue
      .map(_.id.value) shouldBe List(v.id.value)
    Venue.where(_.closed eqs false).fetch().futureValue.map(_.id.value) shouldBe List(v.id.value)

    Venue.where(_.mayor eqs 432432).fetch().futureValue.map(_.id.value) shouldBe Nil
    Venue.where(_.closed eqs true).fetch().futureValue.map(_.id.value) shouldBe Nil

    VenueClaim
      .where(_.status eqs ClaimStatus.approved)
      .fetch()
      .futureValue
      .map(_.id.value) shouldBe List(vc.id.value)
    VenueClaim.where(_.venueid eqs v.id.value).fetch().futureValue.map(_.id.value) shouldBe List(
      vc.id.value)
  }

  test("Inequality tests") {
    val v = baseTestVenue()
    v.save.futureValue
    val vc = baseTestVenueClaim(v.id.value)
    vc.save.futureValue

    // neq,lt,gt, where the lone Venue has mayor_count=3, and the only
    // VenueClaim has status approved.
    Venue.where(_.mayor_count neqs 5).fetch().futureValue.map(_.id.value) shouldBe List(v.id.value)
    Venue.where(_.mayor_count < 5).fetch().futureValue.map(_.id.value) shouldBe List(v.id.value)
    Venue.where(_.mayor_count lt 5).fetch().futureValue.map(_.id.value) shouldBe List(v.id.value)
    Venue.where(_.mayor_count <= 5).fetch().futureValue.map(_.id.value) shouldBe List(v.id.value)
    Venue.where(_.mayor_count lte 5).fetch().futureValue.map(_.id.value) shouldBe List(v.id.value)
    Venue.where(_.mayor_count > 5).fetch().futureValue.map(_.id.value) shouldBe Nil
    Venue.where(_.mayor_count gt 5).fetch().futureValue.map(_.id.value) shouldBe Nil
    Venue.where(_.mayor_count >= 5).fetch().futureValue.map(_.id.value) shouldBe Nil
    Venue.where(_.mayor_count gte 5).fetch().futureValue.map(_.id.value) shouldBe Nil
    Venue.where(_.mayor_count between (3, 5)).fetch().futureValue.map(_.id.value) shouldBe List(
      v.id.value)
    VenueClaim
      .where(_.status neqs ClaimStatus.approved)
      .fetch()
      .futureValue
      .map(_.id.value) shouldBe Nil
    VenueClaim
      .where(_.status neqs ClaimStatus.pending)
      .fetch()
      .futureValue
      .map(_.id.value) shouldBe List(vc.id.value)
  }

  test("Select queries") {
    val v = baseTestVenue()
    v.save.futureValue

    val base = Venue.where(_.id eqs v.id.value)
    base.select(_.id).fetch().futureValue shouldBe List(v.id.value)
    base.select(_.legacyid).fetch().futureValue shouldBe List(v.legacyid.value)
    base.select(_.legacyid, _.userid).fetch().futureValue shouldBe List(
      (v.legacyid.value, v.userid.value))
    base.select(_.legacyid, _.userid, _.mayor).fetch().futureValue shouldBe List(
      (v.legacyid.value, v.userid.value, v.mayor.value))
    base.select(_.legacyid, _.userid, _.mayor, _.mayor_count).fetch().futureValue shouldBe List(
      (v.legacyid.value, v.userid.value, v.mayor.value, v.mayor_count.value))
    base
      .select(_.legacyid, _.userid, _.mayor, _.mayor_count, _.closed)
      .fetch()
      .futureValue shouldBe List(
      (v.legacyid.value, v.userid.value, v.mayor.value, v.mayor_count.value, v.closed.value))
    base
      .select(_.legacyid, _.userid, _.mayor, _.mayor_count, _.closed, _.tags)
      .fetch()
      .futureValue shouldBe List(
      (v.legacyid.value,
       v.userid.value,
       v.mayor.value,
       v.mayor_count.value,
       v.closed.value,
       v.tags.value))
  }

  test("Select enum") {
    val v = baseTestVenue()
    v.save.futureValue
    Venue.where(_.id eqs v.id.value).select(_.status).fetch().futureValue shouldBe List(
      VenueStatus.open)
  }

  test("SelectCaseQueries") {
    val v = baseTestVenue()
    v.save.futureValue

    val base = Venue.where(_.id eqs v.id.value)
    base.selectCase(_.legacyid, V1).fetch().futureValue shouldBe List(V1(v.legacyid.value))
    base.selectCase(_.legacyid, _.userid, V2).fetch().futureValue shouldBe List(
      V2(v.legacyid.value, v.userid.value))
    base.selectCase(_.legacyid, _.userid, _.mayor, V3).fetch().futureValue shouldBe List(
      V3(v.legacyid.value, v.userid.value, v.mayor.value))
    base
      .selectCase(_.legacyid, _.userid, _.mayor, _.mayor_count, V4)
      .fetch()
      .futureValue shouldBe List(
      V4(v.legacyid.value, v.userid.value, v.mayor.value, v.mayor_count.value))
    base
      .selectCase(_.legacyid, _.userid, _.mayor, _.mayor_count, _.closed, V5)
      .fetch()
      .futureValue shouldBe List(
      V5(v.legacyid.value, v.userid.value, v.mayor.value, v.mayor_count.value, v.closed.value))
    base
      .selectCase(_.legacyid, _.userid, _.mayor, _.mayor_count, _.closed, _.tags, V6)
      .fetch()
      .futureValue shouldBe List(
      V6(v.legacyid.value,
         v.userid.value,
         v.mayor.value,
         v.mayor_count.value,
         v.closed.value,
         v.tags.value))
  }

  test("SelectSubfieldQueries") {
    val v = baseTestVenue()
    v.save.futureValue
    val t = baseTestTip()
    t.save.futureValue

    // select subfields
    Tip.where(_.id eqs t.id.value).select(_.counts at "foo").fetch().futureValue shouldBe List(
      Some(1L))

    val subuserids: List[Option[List[Long]]] =
      Venue.where(_.id eqs v.id.value).select(_.claims.subselect(_.userid)).fetch().futureValue
    subuserids shouldBe List(Some(List(1234, 5678)))

    // selecting a claims.userid when there is no top-level claims list should
    // have one element in the List for the one Venue, but an Empty for that
    // Venue since there's no list of claims there.
    Venue
      .where(_.id eqs v.id.value)
      .modify(_.claims unset)
      .and(_.lastClaim unset)
      .updateOne()
      .futureValue
    Venue
      .where(_.id eqs v.id.value)
      .select(_.lastClaim.subselect(_.userid))
      .fetch()
      .futureValue shouldBe List(None)
    Venue
      .where(_.id eqs v.id.value)
      .select(_.claims.subselect(_.userid))
      .fetch()
      .futureValue shouldBe List(None)
  }
  //
  //  //  @Ignore("These tests are broken because DummyField doesn't know how to convert a String to an Enum")
  //  //  def testSelectEnumSubfield: Unit = {
  //  //    val v = baseTestVenue()
  //  //    v.save.futureValue
  //  //
  //  //    // This behavior is broken because we get a String back from mongo, and at
  //  //    // that point we only have a DummyField for the subfield, and that doesn't
  //  //    // know how to convert the String to an Enum.
  //  //
  //  //    val statuses: List[Option[VenueClaimBson.status.MyType]] =
  //  //          Venue.where(_.id eqs v.id.value).select(_.lastClaim.subselect(_.status)) .fetch()
  //  //    // This assertion works.
  //  //    statuses must_== List(Some("Approved"))
  //  //    // This assertion is what we want, and it fails.
  //  //    // statuses must_== List(Some(ClaimStatus.approved))
  //  //
  //  //    val subuseridsAndStatuses: List[(Option[List[Long]], Option[List[VenueClaimBson.status.MyType]])] =
  //  //          Venue.where(_.id eqs v.id.value)
  //  //               .select(_.claims.subselect(_.userid), _.claims.subselect(_.status))
  //  //               .fetch()
  //  //    // This assertion works.
  //  //    subuseridsAndStatuses must_== List((Some(List(1234, 5678)), Some(List("Pending approval", "Approved"))))
  //  //
  //  //    // This assertion is what we want, and it fails.
  //  //    // subuseridsAndStatuses must_== List((Some(List(1234, 5678)), Some(List(ClaimStatus.pending, ClaimStatus.approved))))
  //  //  }
  //
  //  //  @Test
  //  //  def testReadPreference: Unit = {
  //  //    // Note: this isn't a real test of readpreference because the test mongo setup
  //  //    // doesn't have replicas. This basically just makes sure that readpreference
  //  //    // doesn't break everything.
  //  //    val v = baseTestVenue().save
  //  //
  //  //    // eqs
  //  //    Venue.where(_._id eqs v.id).fetch().map(_.id)                         must_== List(v.id)
  //  //    Venue.where(_._id eqs v.id).setReadPreference(ReadPreference.secondary).fetch().map(_.id)        must_== List(v.id)
  //  //    Venue.where(_._id eqs v.id).setReadPreference(ReadPreference.primary).fetch().map(_.id)       must_== List(v.id)
  //  //  }

  test("FindAndModifyTests") {
    val v1 = Venue
      .where(_.venuename eqs "v1")
      .findAndModify(_.userid setTo 5)
      .upsertOne(returnNew = false)
      .futureValue
    v1 shouldBe None

    val v2 = Venue
      .where(_.venuename eqs "v2")
      .findAndModify(_.userid setTo 5)
      .upsertOne(returnNew = true)
      .futureValue
    v2.map(_.userid.value) shouldBe Some(5)

    val v3 = Venue
      .where(_.venuename eqs "v2")
      .findAndModify(_.userid setTo 6)
      .upsertOne(returnNew = false)
      .futureValue
    v3.map(_.userid.value) shouldBe Some(5)

    val v4 = Venue
      .where(_.venuename eqs "v2")
      .findAndModify(_.userid setTo 7)
      .upsertOne(returnNew = true)
      .futureValue
    v4.map(_.userid.value) shouldBe Some(7)
  }

  test("RegexQueriesTest") {
    val v = baseTestVenue()
    v.save.futureValue
    Venue
      .where(_.id eqs v.id.value)
      .and(_.venuename startsWith "test v")
      .count
      .futureValue shouldBe 1
    Venue
      .where(_.id eqs v.id.value)
      .and(_.venuename matches ".es. v".r)
      .count
      .futureValue shouldBe 1
    Venue
      .where(_.id eqs v.id.value)
      .and(_.venuename matches "Tes. v".r)
      .count
      .futureValue shouldBe 0
    Venue
      .where(_.id eqs v.id.value)
      .and(_.venuename matches Pattern.compile("Tes. v", Pattern.CASE_INSENSITIVE))
      .count
      .futureValue shouldBe 1
    Venue
      .where(_.id eqs v.id.value)
      .and(_.venuename matches "test .*".r)
      .and(_.legacyid in List(v.legacyid.value))
      .count
      .futureValue shouldBe 1
    Venue
      .where(_.id eqs v.id.value)
      .and(_.venuename matches "test .*".r)
      .and(_.legacyid nin List(v.legacyid.value))
      .count
      .futureValue shouldBe 0
    Venue.where(_.tags matches """some\s.*""".r).count.futureValue shouldBe 1
  }

  test("LimitTests") {
    Future.traverse(1 to 50)(_ => baseTestVenue().save).futureValue

    val q = Venue.select(_.id)
    q.limit(10).fetch().futureValue.length shouldBe 10
  }

  test("CountTest") {
    Future.traverse(1 to 10)(_ => baseTestVenue().save).futureValue
    val q = Venue.select(_.id)
    q.count().futureValue shouldBe 10
    q.limit(3).count().futureValue shouldBe 3
    q.limit(15).count().futureValue shouldBe 10
    q.skip(5).count().futureValue shouldBe 5
    q.skip(12).count().futureValue shouldBe 0
    q.skip(3).limit(5).count().futureValue shouldBe 5
    q.skip(8).limit(4).count().futureValue shouldBe 2
  }

  test("DistinctTest") {
    Future.traverse(1 to 15)(i => baseTestVenue().userid(i % 3).save).futureValue
    Venue.where(_.mayor eqs 789).distinct(_.userid).futureValue.length shouldBe 3
    //      Venue.where(_.mayor eqs 789).countDistinct(_.userid) must_== 3
  }

  test("SliceTest") {
    baseTestVenue().tags(List("1", "2", "3", "4")).save.futureValue
    Venue.select(_.tags.slice(2)).get().futureValue shouldBe Some(List("1", "2"))
    Venue.select(_.tags.slice(-2)).get().futureValue shouldBe Some(List("3", "4"))
    Venue.select(_.tags.slice(1, 2)).get().futureValue shouldBe Some(List("2", "3"))
  }
}
