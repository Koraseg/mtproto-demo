package demo.mtproto

import java.math.BigInteger
import java.util.concurrent.ThreadLocalRandom

object RandomUtils {
  // here we assume fingerprints array contains only one element
  private val ResponseBodyLength = 64

  private val RequestBodyLength = 20

  private val ParamsBodyLength = 292

  def getRandomResponseMessage: ResponsePqMessage = {
    val rnd = ThreadLocalRandom.current()
    val bytesArray = new Array[Byte](16)
    rnd.nextBytes(bytesArray)
    val nonce = bytesArray.toVector
    rnd.nextBytes(bytesArray)
    val serverNonce = bytesArray.toVector
    rnd.nextBytes(bytesArray)
    val pq = bytesArray.take(8).toVector

    ResponsePqMessage(
      authKeyId = rnd.nextLong(),
      messageId = rnd.nextLong(),
      messageLength = ResponseBodyLength,
      resPQConstructorNumber = rnd.nextInt(),
      nonce = nonce,
      serverNonce = serverNonce,
      pq = pq,
      vecLongConstructorNumber = rnd.nextInt(),
      fingerprints = Vector(rnd.nextLong())
    )
  }

  def getRandomRequestMessage: RequestPqMessage = {
    val rnd = ThreadLocalRandom.current()
    val bytesArray = new Array[Byte](16)
    rnd.nextBytes(bytesArray)
    val nonce = bytesArray.toVector

    RequestPqMessage(
      authKeyId = rnd.nextLong(),
      messageId = rnd.nextLong(),
      messageLength = RequestBodyLength,
      reqPQConstructorNumber = rnd.nextInt(),
      nonce = nonce
    )
  }

  def getRandomDHParams: ReqDHParams = {
    val rnd = ThreadLocalRandom.current()
    val bytesArray = new Array[Byte](16)
    rnd.nextBytes(bytesArray)
    val nonce = bytesArray.toVector
    rnd.nextBytes(bytesArray)
    val serverNonce = bytesArray.toVector

    ReqDHParams(
      authKeyId = rnd.nextLong(),
      messageId = rnd.nextLong(),
      messageLength = ParamsBodyLength,
      reqDHparamsConstructorNumber = rnd.nextInt(),
      nonce = nonce,
      serverNonce = serverNonce,
      encryptedData = getRandomPQInnerData
    )

  }
  def getRandomPQInnerData: PQInnerData = {
    val rnd = ThreadLocalRandom.current()
    val bytesArray = new Array[Byte](32)
    val p = new BigInteger(rnd.nextInt().toString)
    val q = new BigInteger(rnd.nextInt().toString)
    val pq = p.multiply(q)
    val nonce = bytesArray.take(16).toVector
    val serverNonce = bytesArray.drop(16).toVector
    rnd.nextBytes(bytesArray)
    val newNonce = bytesArray.toVector

    PQInnerData(
      pqInnerDataConstructorNumber = 42,
      pq = pq.toByteArray.toVector,
      p = p.toByteArray.toVector,
      q = q.toByteArray.toVector,
      nonce = nonce,
      serverNonce = serverNonce,
      newNonce = newNonce
    )
  }

}
