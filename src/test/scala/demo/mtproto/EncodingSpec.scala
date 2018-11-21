package demo.mtproto

import RandomUtils._
import org.scalatest.{FlatSpec, Matchers}
import scodec.codecs.CipherFactory
import scodec.{Codec, Err}

import scala.util.Right

class EncodingSpec extends FlatSpec with Matchers {
  implicit val cf: CipherFactory = createCipherFactory()

  "pqRequest codec" should "work correctly" in {
    val req = getRandomRequestMessage
    assert(serAndDeser(req) == Right(req))
  }

  "pqResponse codec" should "work correctly" in {
    val resp = getRandomResponseMessage
    assert(serAndDeser(resp) == Right(resp))
  }

  "pqInnerData codec" should "work correctly" in {
    val innerData = getRandomPQInnerData
    assert(serAndDeser(innerData) == Right(innerData))
  }

  "dhParams codec" should "work correctly" in {
    val dhParams = getRandomDHParams
    assert(serAndDeser(dhParams) == Right(dhParams))
  }

  private def serAndDeser[T](value: T)(implicit ev: Codec[T]): Either[Err, T] = {
    ev.encode(value).toEither.flatMap(bv => ev.decode(bv).toEither.map(_.value))
  }

}
