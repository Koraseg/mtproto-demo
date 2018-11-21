package demo.mtproto

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import com.typesafe.config.ConfigFactory

object ServerApp {
  def main(args: Array[String]): Unit = {
    lazy val conf = ConfigFactory.defaultApplication()
    val host = conf.getString("demo.mtproto.server.host")
    val port = conf.getInt("demo.mtproto.server.port")
    implicit val system = ActorSystem("ServerSystem")
    implicit val materializer = ActorMaterializer()
    val server = new MtProtoServer(host, port)
    server.start()
  }

}
