package demo

import java.nio.ByteOrder
import java.security.KeyPairGenerator
import javax.crypto.Cipher

import akka.stream.scaladsl.Framing
import akka.util.ByteString
import scodec.Codec
import scodec.bits.{BitVector, ByteVector}
import scodec.codecs._
import shapeless.Generic

package object mtproto {

  val EncryptDataSizeInBytes = 256
  val MaximumFrameLength = 2048

  type MessageType = Either[CloseConnectionEvent, ByteString]

  implicit class BitVectorExt(val bitVector: BitVector) extends AnyVal {
    def toByteString = ByteString.fromArray(bitVector.toByteArray)
  }

  implicit def hlist2CaseClassCodec[T, R](hlistCodec: Codec[R])(implicit gen: Generic.Aux[T, R]): Codec[T] = {
    hlistCodec.map(gen.from).contramap(gen.to).fuse
  }

  implicit def byteString2BitVector(bs: ByteString): BitVector = BitVector(bs.toByteBuffer)

  sealed trait MtprotoMessage {
    def authKeyId: Long
    def messageId: Long
    def messageLength: Int
  }

  object RequestPqMessage {
    implicit val codec: Codec[RequestPqMessage] =
      int64 :: int64 :: int32 :: int32 :: fixedSizeByteVector(16)

  }
  final case class RequestPqMessage(
    authKeyId: Long,
    messageId: Long,
    messageLength: Int,
    reqPQConstructorNumber: Int,
    nonce: Vector[Byte]
  ) extends MtprotoMessage

  final case class MtprotoSuccessMessage(authKeyId: Long, messageId: Long, messageLength: Int) extends MtprotoMessage

  object ResponsePqMessage {
    implicit val codec: Codec[ResponsePqMessage] =
      int64 :: int64 :: int32 :: int32 ::
        fixedSizeByteVector(16) ::
        fixedSizeByteVector(16) ::
        sizedByteVectorWithPadding(12) ::
        int32 :: vectorOfN(int32, int64)
  }

  final case class ResponsePqMessage(
    authKeyId: Long,
    messageId: Long,
    messageLength: Int,
    resPQConstructorNumber: Int,
    nonce: Vector[Byte],
    serverNonce: Vector[Byte],
    pq: Vector[Byte],
    vecLongConstructorNumber: Int,
    fingerprints: Vector[Long]
  ) extends MtprotoMessage

  object ReqDHParams {
    implicit def codec(implicit cf: CipherFactory): Codec[ReqDHParams] =
      int64 :: int64 :: int32 :: int32 :: fixedSizeByteVector(16) ::
        fixedSizeByteVector(16) :: encrypted(PQInnerData.codec)

  }

  final case class ReqDHParams(
    authKeyId: Long,
    messageId: Long,
    messageLength: Int,
    reqDHparamsConstructorNumber: Int,
    nonce: Vector[Byte],
    serverNonce: Vector[Byte],
    encryptedData: PQInnerData
  ) extends MtprotoMessage

  object PQInnerData {
    implicit val codec: Codec[PQInnerData] = {
      val nonPaddedCodec: Codec[PQInnerData] = int32 :: sizedByteVectorWithPadding(12) ::
        sizedByteVectorWithPadding(8) :: sizedByteVectorWithPadding(8) ::
        fixedSizeByteVector(16) :: fixedSizeByteVector(16) :: fixedSizeByteVector(32)

      paddedFixedSizeBytes(EncryptDataSizeInBytes, nonPaddedCodec, constant(ByteVector.fromByte(0)))
    }
  }

  final case class PQInnerData(
    pqInnerDataConstructorNumber: Int,
    pq: Vector[Byte],
    p: Vector[Byte],
    q: Vector[Byte],
    nonce: Vector[Byte],
    serverNonce: Vector[Byte],
    newNonce: Vector[Byte]
  )

  sealed trait CloseConnectionEvent extends Serializable

  case object MtProtoSuccessSignal extends CloseConnectionEvent
  class MTProtoException(msg: String, cause: Throwable) extends Exception with CloseConnectionEvent {
    def this(msg: String) = this(msg, null)
  }

  private[mtproto] def createCipherFactory(): CipherFactory = {
    val kpg = KeyPairGenerator.getInstance("RSA")
    kpg.initialize(EncryptDataSizeInBytes * 8)

    val pair = kpg.generateKeyPair()

    new CipherFactory {
      override def newEncryptCipher: Cipher = {
        val cf = Cipher.getInstance("RSA/ECB/NoPadding")
        cf.init(Cipher.ENCRYPT_MODE, pair.getPublic)
        cf
      }

      override def newDecryptCipher: Cipher = {
        val cf = Cipher.getInstance("RSA/ECB/NoPadding")
        cf.init(Cipher.DECRYPT_MODE, pair.getPrivate)
        cf
      }
    }
  }

  private[mtproto] def mtprotoFraming =
    Framing.lengthField(4, 16, MaximumFrameLength, ByteOrder.BIG_ENDIAN, (_, sz) => sz + 20)

  private def fixedSizeByteVector(size: Int): Codec[Vector[Byte]] = vectorOfN(provide(size), byte)
  private def sizedByteVectorWithPadding(size: Int): Codec[Vector[Byte]] =
    paddedFixedSizeBytes(size, vectorOfN(int8, byte), constant(ByteVector.fromByte(0)))
}
