package reactiverogue.bson

import reactivemongo.bson._
import java.util.regex.Pattern
import java.util.Date

trait BSONSerializable[T] {

  def asBSONValue(value: T): BSONValue

  def fromBSONValue: PartialFunction[BSONValue, T]
}

object BSONSerializable {
  def apply[T: BSONSerializable]: BSONSerializable[T] = implicitly[BSONSerializable[T]]

  implicit object BooleanIsBSONType extends BSONSerializable[Boolean] {
    override def asBSONValue(v: Boolean): BSONValue = BSONBoolean(v)

    override def fromBSONValue = {
      case BSONBoolean(v) => v
    }
  }

  implicit object StringIsBSONSerializable extends BSONSerializable[String] {
    override def asBSONValue(v: String): BSONValue = BSONString(v)

    override def fromBSONValue = {
      case BSONString(v) => v
    }
  }

  implicit object IntIsBSONSerializable extends BSONSerializable[Int] {
    override def asBSONValue(v: Int): BSONValue = BSONInteger(v)

    override def fromBSONValue = {
      case BSONInteger(v) => v
      case BSONDouble(v) => v.toInt
      case BSONLong(v) => v.toInt
    }
  }

  implicit object LongIsBSONSerializable extends BSONSerializable[Long] {
    override def asBSONValue(v: Long): BSONValue = BSONLong(v)

    override def fromBSONValue = {
      case BSONLong(v) => v
      case BSONInteger(v) => v.toLong
      case BSONDouble(v) => v.toLong
    }
  }

  implicit object DoubleIsBSONSerializable extends BSONSerializable[Double] {
    override def asBSONValue(v: Double): BSONValue = BSONDouble(v)

    override def fromBSONValue = {
      case BSONDouble(v) => v
      case BSONInteger(v) => v.toDouble
      case BSONLong(v) => v.toDouble
    }
  }

  implicit object PatternIsBSONSerializable extends BSONSerializable[Pattern] {
    val flagMap = Map(
      Pattern.CANON_EQ -> "c",
      Pattern.CASE_INSENSITIVE -> "i",
      Pattern.COMMENTS -> "x",
      Pattern.DOTALL -> "s",
      Pattern.LITERAL -> "t",
      Pattern.MULTILINE -> "m",
      Pattern.UNICODE_CASE -> "u",
      Pattern.UNIX_LINES -> "d"
    )

    def flagsToString(flags: Int) = {
      (for {
        (mask, char) <- flagMap
        if (flags & mask) != 0
      } yield char).mkString
    }

    override def asBSONValue(v: Pattern): BSONValue =
      BSONRegex(v.toString, flagsToString(v.flags))

    override def fromBSONValue = {
      case BSONRegex(v, flags) => Pattern.compile(v) //TODO: consider flags
    }
  }

  implicit object DateIsBSONSerializable extends BSONSerializable[Date] {

    override def asBSONValue(v: Date): BSONValue =
      BSONDateTime(v.getTime)

    override def fromBSONValue = {
      case BSONDateTime(v) => new java.util.Date(v)
    }
  }

  implicit object ObjectIdIsBSONSerializable extends BSONSerializable[BSONObjectID] {
    override def asBSONValue(v: BSONObjectID): BSONValue = v

    override def fromBSONValue = {
      case v: BSONObjectID => v
    }
  }

  implicit object BSONDocumentIsBSONSerializable extends BSONSerializable[BSONDocument] {
    override def asBSONValue(v: BSONDocument): BSONValue = v

    override def fromBSONValue = {
      case v: BSONDocument => v
    }
  }

  class StringMapBSONTypesAreBSONTypes[T: BSONSerializable]
      extends BSONSerializable[Map[String, T]] {
    override def asBSONValue(v: Map[String, T]): BSONValue = {
      BSONDocument(v.mapValues(BSONSerializable[T].asBSONValue).toSeq)
    }

    override def fromBSONValue = {
      case doc: BSONDocument =>
        val ev = implicitly[BSONSerializable[T]]
        val seq = doc.elements.collect {
          case BSONElement(key, v) if ev.fromBSONValue.isDefinedAt(v) =>
            key -> ev.fromBSONValue(v)
        }
        seq.toMap
    }
  }

  implicit def StringMapBSONTypesAreBSONTypes[T: BSONSerializable]
    : BSONSerializable[Map[String, T]] =
    new StringMapBSONTypesAreBSONTypes[T]

  class ListsOfBSONTypesAreBSONTypes[T: BSONSerializable] extends BSONSerializable[List[T]] {
    override def asBSONValue(v: List[T]): BSONValue = {
      BSONArray(v.map(BSONSerializable[T].asBSONValue).toSeq)
    }

    override def fromBSONValue = {
      case array: BSONArray =>
        val ev = implicitly[BSONSerializable[T]]
        array.values.toList.collect {
          case v if ev.fromBSONValue.isDefinedAt(v) =>
            ev.fromBSONValue(v)
        }
    }
  }

  implicit def ListsOfBSONTypesAreBSONTypes[T: BSONSerializable]: BSONSerializable[List[T]] =
    new ListsOfBSONTypesAreBSONTypes[T]
}
