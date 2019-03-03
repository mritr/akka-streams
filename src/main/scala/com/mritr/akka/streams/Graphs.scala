package com.mritr.akka.streams

import akka.NotUsed
import akka.actor.ActorSystem
import akka.stream.{ActorMaterializer, ClosedShape}
import akka.stream.scaladsl._

object Graphs extends App {

  implicit val system = ActorSystem("WorkingWithFlows")
  implicit val materializer = ActorMaterializer()
  implicit val ec = system.dispatcher

  // Working from exmple, this shows fan out and fan in.
  val g = RunnableGraph.fromGraph(GraphDSL.create() { implicit builder: GraphDSL.Builder[NotUsed] =>
    import GraphDSL.Implicits._
    val in = Source(1 to 10)
    val out = Sink.foreach(println)

    val bcast = builder.add(Broadcast[Int](2))
    val merge = builder.add(Merge[Int](2))

    val f1, f2, f3 = Flow[Int].map(_ + 10)
    val f4 = Flow[Int].map(_ + 100)
    //in ~> f1 ~> f2 ~> f3 ~> out
    //in ~> f1 ~> bcast ~> out
    //bcast ~> f4 ~> out
    // 11 + 10 + 10
    in ~> f1 ~> bcast ~> f2 ~> merge ~> f3 ~> out
    // 11 + 100 + 10
    bcast ~> f4 ~> merge

    ClosedShape
  })

  val task = g.run()

}
