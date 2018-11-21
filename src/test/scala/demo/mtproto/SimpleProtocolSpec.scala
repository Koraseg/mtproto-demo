package demo.mtproto

import akka.actor.{ActorSystem, Props}
import akka.stream._
import akka.stream.scaladsl.{Broadcast, Flow, GraphDSL, Keep, Merge, Partition, RunnableGraph, Sink, Source, Tcp}
import akka.util.{ByteString, Timeout}
import org.scalatest._
import org.scalatest.{BeforeAndAfterAll, FlatSpec}
import akka.pattern.ask
import scala.concurrent.duration._
import scodec.Codec
import scala.concurrent.{Await}

class SimpleProtocolSpec extends FlatSpec with BeforeAndAfterAll {
  val host = "127.0.0.1"
  val port = 8888

  implicit val system = ActorSystem("TestSystem")
  implicit val materializer = ActorMaterializer()
  implicit val timeout = Timeout(3 seconds)
  implicit val ec = system.dispatcher

  val server = new MtProtoServer(host, port)
  implicit val cipherFactory = server.getCipherFactory

  override def beforeAll(): Unit = {
    server.start()
  }

  "client" should "successfully proceed via protocol" in {
    val request = Codec[RequestPqMessage].encode(RandomUtils.getRandomRequestMessage).require

    val resultSink = Flow[CloseConnectionEvent].toMat(Sink.head)(Keep.right)

    val mprotoGraph = RunnableGraph.fromGraph(
      GraphDSL.create(resultSink) { implicit builder => sink =>
        import akka.stream.scaladsl.GraphDSL.Implicits._
        val initialRequest = Source.single(request.toByteString)
        val actor = system.actorOf(Props(new ClientActor()))

        val processingFlow = Tcp()
          .outgoingConnection(host, port)
          .via(mtprotoFraming)
          .mapAsync(1)(bs => actor.ask(bs).mapTo[MessageType])

        val partition = builder.add(Partition[MessageType](2, x => if (x.isLeft) 0 else 1))

        val out = Flow[MessageType].map(_.left.get)

        val merge = builder.add(Merge[ByteString](2))
        initialRequest ~> merge ~> processingFlow ~> partition

        partition.out(0) ~> out ~> sink
        partition.out(1).map(_.right.get) ~> merge

        ClosedShape

      }
    )

    assert(Await.result(mprotoGraph.run(), 5 seconds) == MtProtoSuccessSignal)

  }

  override def afterAll(): Unit = {
    server.stop()
  }

}
