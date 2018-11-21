package demo.mtproto

import akka.actor.Actor
import akka.util.ByteString
import scodec.Attempt.{Failure, Successful}
import scodec.codecs.CipherFactory
import scodec.{Codec, DecodeResult}
import scodec.codecs.implicits._

class ClientActor(implicit cf: CipherFactory) extends Actor {
  override def receive: Receive = {
    case bs: ByteString =>
      val result = Codec[ResponsePqMessage].decode(bs) match {
        case Successful(DecodeResult(msg, _)) =>
          Codec[ReqDHParams]
            .encode(RandomUtils.getRandomDHParams)
            .toEither
            .left
            .map(err => new MTProtoException(err.message))
            .map(_.toByteString)
        case Failure(err) =>
          Left(new MTProtoException(err.message))
      }
      context.become(waitingForSuccess)
      sender() ! result
  }

  private def waitingForSuccess: Receive = {
    case bs: ByteString =>
      val result = Codec[MtprotoSuccessMessage]
        .decode(bs)
        .toEither
        .left
        .map(err => new MTProtoException(err.message))
        .flatMap(_ => Left(MtProtoSuccessSignal))

      sender() ! result
  }

}
