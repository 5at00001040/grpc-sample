package grpc.sample

import grpc.sample.protos.model.player.{Permission, Player}

object ProtobufSample extends App {

  val playerBinary = Player("1", "player1", List(Permission("position", true, false))).toByteArray
  val playerObject = Player.parseFrom(playerBinary)

  println(playerObject)

}
