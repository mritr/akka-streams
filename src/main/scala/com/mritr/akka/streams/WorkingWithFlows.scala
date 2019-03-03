package com.mritr.akka.streams

import akka.NotUsed
import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.stream.scaladsl._
import com.mritr.akka.streams.QuickStart.system

import scala.concurrent.Future

object WorkingWithFlows extends App {
  implicit val system = ActorSystem("WorkingWithFlows")
  implicit val materializer = ActorMaterializer()

  val source = Source(1 to 10)
  val sink: Sink[Int, Future[Int]] = Sink.fold[Int, Int](0)(_ + _)

//  val runnable: RunnableGraph[Future[Int]] = source.toMat(sink)(Keep.right)
//  val sum: Future[Int] = runnable.run()
  //Alternate:
  val sum: Future[Int] = source.runWith(sink)

  implicit val ec = system.dispatcher

  sum.onComplete(s => {
    println(s)
    system.terminate()
  })

  val explicit = Source(1 to 6).via(Flow[Int].map(_ * 2)).to(Sink.foreach(println(_))).run()


}
