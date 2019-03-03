package com.mritr.akka.streams

import akka.NotUsed
import akka.actor.ActorSystem
import akka.stream.{ActorMaterializer, ClosedShape}
import akka.stream.scaladsl._

// https://doc.akka.io/docs/akka/2.5/stream/stream-graphs.html
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
  //val task = g.run()

  // 2nd example
  val topHeadSink = Sink.head[Int]
  val bottomHeadSink = Sink.head[Int]
  val sharedDoubler = Flow[Int].map(_ * 2)
  val printer = Flow[Int].map(x => {
    println(x)
    x
  })

  val task2 = RunnableGraph.fromGraph(GraphDSL.create(topHeadSink, bottomHeadSink)((_, _)) { implicit builder =>
    (topHS, bottomHS) =>
      import GraphDSL.Implicits._
      val broadcast = builder.add(Broadcast[Int](2))
      Source.single(1) ~> broadcast.in

      broadcast ~> sharedDoubler ~> printer ~> topHS.in
      broadcast ~> sharedDoubler ~> printer ~> bottomHS.in
      ClosedShape
  })

  //task2.run()

  // Similar to what we're trying to do, multiple sources tied to a specific sink passing through a single flow.
  val scratchTask =  RunnableGraph.fromGraph(GraphDSL.create() { implicit builder: GraphDSL.Builder[NotUsed] =>
    import GraphDSL.Implicits._
    val source1 = Source(1 to 10)
    val source2 = Source(11 to 20)
    val sink1 = Sink.ignore
    val sink2 = Sink.ignore

    source1 ~> sharedDoubler ~> printer ~> sink1
    source2 ~> sharedDoubler ~> printer ~> sink2
    ClosedShape
  })

  scratchTask.run()

}
