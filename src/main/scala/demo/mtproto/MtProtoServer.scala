package demo.mtproto

import akka.actor.{ActorSystem, Props}
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{Flow, Framing, Source, Tcp}
import akka.stream.scaladsl.Tcp.{IncomingConnection, ServerBinding}
import akka.pattern._

import scala.concurrent.duration._
import akka.util.{ByteString, Timeout}
import scodec.Codec
import scodec.codecs.CipherFactory
import scodec.codecs.implicits._
import scala.concurrent.Future

class MtProtoServer(host: String, port: Int)(implicit system: ActorSystem,
                                             actorMaterializer: ActorMaterializer,
                                             messageProcessor: MessageProcessor = StubMessageProcessor) {

  private implicit lazy val askTimeout = Timeout(5 seconds)
  private implicit lazy val executionContext = system.dispatcher
  private implicit lazy val cf = createCipherFactory()
  private val FinalMessage = Codec[MtprotoSuccessMessage].encode(MtprotoSuccessMessage(0, 0, 0)).require.toByteString

  lazy val connections: Source[IncomingConnection, Future[ServerBinding]] =
    Tcp().bind(host, port)

  def start(): Unit = {
    connections runForeach { connection =>
      println(s"New connection from: ${connection.remoteAddress}")
      val stateActor = system.actorOf(Props(new ServerActor))
      val handler = Flow[ByteString]
        .via(mtprotoFraming)
        .mapAsync(1)(bs => stateActor.ask(bs).mapTo[Either[CloseConnectionEvent, ByteString]])
        .takeWhile(_.isRight)
        .map(_.right.get)
        .concat(Source.single(FinalMessage))

      connection.handleWith(handler)
    }
  }

  def stop(): Unit = system.terminate()

  // for testing purposes only
  private[mtproto] def getCipherFactory: CipherFactory = cf

}
