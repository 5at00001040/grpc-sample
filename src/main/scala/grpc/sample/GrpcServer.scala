package grpc.sample

import grpc.sample.protos.model.player.Player
import grpc.sample.protos.model.position.tracking.PositionTracking
import grpc.sample.protos.service.game.tracking.{
  GameTrackingServiceGrpc,
  Result
}
import io.grpc.stub.StreamObserver
import io.grpc.{Server, ServerBuilder}

import scala.concurrent.{ExecutionContext, Future}

object GrpcServer extends App {
  val server = new GrpcServer(ExecutionContext.global)
  server.start()
  server.blockUntilShutdown()
}

class GrpcServer(executionContext: ExecutionContext) { self =>
  private[this] var server: Server = null

  private def start(): Unit = {
    server = ServerBuilder
      .forPort(50051)
      //      .useTransportSecurity(
      //        new File("/path/to/your/certificate"),
      //        new File("/path/to/your/private-key")
      //      )
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
