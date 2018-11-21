package demo.mtproto

import scala.util.Random

trait MessageProcessor {
  def processRequest(reqPqMessage: RequestPqMessage): Either[MTProtoException, ResponsePqMessage]

  def processParams(reqDHParams: ReqDHParams): Either[MTProtoException, Unit]
}

object StubMessageProcessor extends MessageProcessor {

  override def processRequest(reqPqMessage: RequestPqMessage): Either[MTProtoException, ResponsePqMessage] = {
    Right(RandomUtils.getRandomResponseMessage)
  }

  override def processParams(reqDHParams: ReqDHParams): Either[MTProtoException, Unit] = {
    Right(())
  }
}
