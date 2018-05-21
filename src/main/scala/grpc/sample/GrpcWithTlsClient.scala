package grpc.sample

import java.io.File
import java.util.concurrent.TimeUnit

import grpc.sample.protos.model.player.{Permission, Player}
import grpc.sample.protos.model.position.tracking.{PositionTracking, Vector3}
import grpc.sample.protos.service.game.tracking.GameTrackingServiceGrpc.{GameTrackingServiceBlockingStub, GameTrackingServiceStub}
import grpc.sample.protos.service.game.tracking.{GameTrackingServiceGrpc, Result}
import io.grpc.netty.shaded.io.grpc.netty.{GrpcSslContexts, NettyChannelBuilder}
import io.grpc.stub.StreamObserver
import io.grpc.{ManagedChannel, StatusRuntimeException}

object GrpcWithTlsClient extends App {

  val channel =
    NettyChannelBuilder
      .forAddress("grpc.test.google.fr", 50061)
      .sslContext(
        GrpcSslContexts.forClient().trustManager(new File("/tmp/any/path/certs/ca.pem")).build()
      )
      .build
  val blockingStub = GameTrackingServiceGrpc.blockingStub(channel)
  val nonBlockingStub = GameTrackingServiceGrpc.stub(channel)
  val client = new GrpcWithTlsClient(channel, blockingStub, nonBlockingStub)

  try {
    client.postPlayer()
    client.postPositionTracking()
  } finally {
    client.shutdown()
  }
}

class GrpcWithTlsClient private (
    private val channel: ManagedChannel,
    private val blockingStub: GameTrackingServiceBlockingStub,
    private val nonBlockingStub: GameTrackingServiceStub
) {

  def shutdown() = {
    channel.shutdown.awaitTermination(5, TimeUnit.SECONDS)
  }

  def postPlayer() = {
    val request =
      Player("1", "player1", List(Permission("position", true, false)))
    try {
      println("request message: " + request)
      val response = blockingStub.postPlayer(request)
      println("response message: " + response)
    } catch {
      case e: StatusRuntimeException => println("RPC status: " + e.getStatus)
    }
  }

  def postPositionTracking() = {
    val request = PositionTracking("player1",
                                   Some(Vector3(1.0d, 2.0d, 3.0d)),
                                   Some(Vector3(4.0d, 5.0d, 6.0d)))

    val resObs = new StreamObserver[Result] {
      override def onError(t: Throwable) = println("post failed")
      override def onCompleted() = println("post complete")
      override def onNext(value: Result) = println("next post")
    }

    val reqObs: StreamObserver[PositionTracking] =
      nonBlockingStub.postPositionTracking(resObs)

    reqObs.onNext(request)
    Thread.sleep(10000)
    reqObs.onNext(request)
    Thread.sleep(10000)
    reqObs.onNext(request)

    resObs.onCompleted()
    reqObs.onCompleted()
  }

}
