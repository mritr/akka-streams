package com.mritr.akka.streams

import akka.stream._
import akka.stream.scaladsl._

import akka.{ NotUsed, Done }
import akka.actor.ActorSystem
import akka.util.ByteString
import scala.concurrent._
import scala.concurrent.duration._
import java.nio.file.Paths

object QuickStart extends App {

  implicit val system = ActorSystem("QuickStart")
  implicit val materializer = ActorMaterializer()

  def factorialExample(source: Source[Int, NotUsed]): Future[IOResult] = {
    val factorials = source.scan(BigInt(1))((acc, next) => acc * next)
    factorials
      .map(num => ByteString(s"$num\n"))
      .runWith(FileIO.toPath(Paths.get("factorials.txt")))
  }

  def lineSink(filename: String): Sink[String, Future[IOResult]] =
    Flow[String]
      .map(s => ByteString(s + "\n"))
    .toMat(FileIO.toPath(Paths.get(filename)))(Keep.right)

  // First type is the element type the source emits Int.
  // Second type is if the source produces an auxillary value, info about bound port or address, NotUsed here.
  val source: Source[Int, NotUsed] = Source(1 to 100)

  val done = factorialExample(source)
  //val done: Future[Done] = source.runForeach(i => println(i))(materializer)

  implicit val ec = system.dispatcher
  done.onComplete(_ => system.terminate())
}
