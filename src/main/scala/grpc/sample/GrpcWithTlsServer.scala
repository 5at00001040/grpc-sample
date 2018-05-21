package grpc.sample

import java.io.File

import grpc.sample.protos.model.player.Player
import grpc.sample.protos.model.position.tracking.PositionTracking
import grpc.sample.protos.service.game.tracking.{GameTrackingServiceGrpc, Result}
import io.grpc.netty.shaded.io.grpc.netty.NettyServerBuilder
import io.grpc.stub.StreamObserver
import io.grpc.Server

import scala.concurrent.{ExecutionContext, Future}

object GrpcWithTlsServer extends App {
  val server = new GrpcWithTlsServer(ExecutionContext.global)
  server.start()
  server.blockUntilShutdown()
}

class GrpcWithTlsServer(executionContext: ExecutionContext) { self =>
  private[this] var server: Server = null

  private def start(): Unit = {
    server = NettyServerBuilder
      .forPort(50061)
      .useTransportSecurity(
        new File("/tmp/any/path/certs/server1.pem"),
        new File("/tmp/any/path/certs/server1.key")
      )
      .addService(
        GameTrackingServiceGrpc.bindService(new GameTrackingServiceImpl,
                                            executionContext))
      .build
      .start
    sys.addShutdownHook {
      self.stop()
      println("* server shut down *")
    }
  }

  private def stop(): Unit = {
    if (server != null) {
      server.shutdown()
    }
  }

  private def blockUntilShutdown(): Unit = {
    if (server != null) {
      server.awaitTermination()
    }
  }

  private class GameTrackingServiceImpl
      extends GameTrackingServiceGrpc.GameTrackingService {
    override def postPlayer(req: Player) = {
      val reply = Result(message = "register player: " + req.playerName)
      println("register player" + req.playerName)
      Future.successful(reply)
    }

    override def postPositionTracking(resObs: StreamObserver[Result]) = {

      new StreamObserver[PositionTracking] {
        override def onError(t: Throwable) = {
          println("postPositionTracking error")
          resObs.onError(t)
        }

        override def onCompleted() = {
          resObs.onNext(Result("completed"))
          resObs.onCompleted()
          println("postPositionTracking completed")
        }

        override def onNext(req: PositionTracking) = {
          println(s"arrived pos: ${req.position}, dir: ${req.direction}")
        }
      }

    }
  }

}
