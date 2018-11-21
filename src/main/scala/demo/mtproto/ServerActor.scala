package demo.mtproto

import akka.actor.{Actor, ActorLogging}
import akka.util.ByteString
import scodec.Attempt.{Failure, Successful}
import scodec.{Codec, DecodeResult}
import scodec.codecs.CipherFactory
import scodec.codecs._

/**
  * Actor maintaining protocol state during a connection
  * @param cf
  * @param messageProcessor
  */
class ServerActor(implicit cf: CipherFactory, messageProcessor: MessageProcessor) extends Actor with ActorLogging {

  override def receive: Receive = processRequest

  private def processRequest: Receive = {
    case bs: ByteString =>
      val result = Codec[RequestPqMessage].decode(bs) match {
        case Successful(DecodeResult(msg, _)) =>
          messageProcessor
            .processRequest(msg)
            .map(Codec[ResponsePqMessage].encode)
            .flatMap(att => att.toEither.left.map(err => new MTProtoException(err.message)))
            .map(_.toByteString)
        case Failure(err) =>
          Left(new MTProtoException(err.message))
      }

      result match {
        case Right(_) =>
          context.become(processParams)
          sender() ! result
        case Left(_) =>
          context.stop(self)
          sender() ! result
      }
    case unexpected =>
      log.warning(s"Unexpected message $unexpected in actor ${getClass} mailbox")

  }

  private def processParams: Receive = {
    case bs: ByteString =>
      val result = Codec[ReqDHParams].decode(bs) match {
        case Successful(DecodeResult(msg, _)) =>
          messageProcessor.processParams(msg).flatMap(_ => Left(MtProtoSuccessSignal))
        case Failure(err) =>
          Left(new MTProtoException(err.message))
      }
      context.stop(self)
      sender() ! result

    case unexpected =>
      log.warning(s"Unexpected message $unexpected in actor ${getClass} mailbox")
  }

}
