package com.mritr.akka.streams

import akka.NotUsed
import akka.actor.ActorSystem
import akka.stream.{ActorMaterializer, ClosedShape, SourceShape, UniformFanInShape}
import akka.stream.scaladsl._

import scala.collection.immutable
import scala.concurrent.Future

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

  //scratchTask.run()

  // Could be useful...
  val sinks = immutable.Seq("a", "b", "c").map(prefix =>
    Flow[String].filter(str => str.startsWith(prefix)).toMat(Sink.head[String])(Keep.right)
  )

  val dynamicTask: RunnableGraph[Seq[Future[String]]] = RunnableGraph.fromGraph(GraphDSL.create(sinks) { implicit b => sinkList =>
    import GraphDSL.Implicits._

    val broadcast = b.add(Broadcast[String](sinkList.size))

    Source(List("ax", "bx", "cx")) ~> broadcast
    sinkList.foreach(sink => broadcast ~> sink)

    ClosedShape
  })

  val matList: Seq[Future[String]] = dynamicTask.run()
  matList.map(x => x.onComplete(f => println(f)))

  // Partial graphs
  val pickMaxOfThree = GraphDSL.create() { implicit b =>
    import GraphDSL.Implicits._

    // zip1 takes 2 ints and outputs an int.
    val zip1 = b.add(ZipWith[Int,Int,Int](math.max _))
    // zip2 takes result of zip1 and int and outputs an int.
    val zip2 = b.add(ZipWith[Int,Int,Int](math.max _))
    zip1.out ~> zip2.in0

    UniformFanInShape(zip2.out, zip1.in0, zip1.in1, zip2.in1)
  }

  val resultSink = Sink.head[Int]

  val partialConstruction = RunnableGraph.fromGraph(GraphDSL.create(resultSink) { implicit b => sink =>
    import GraphDSL.Implicits._

    // Importing the partial graph will return its shape (inlets & outlets)
    val pm3 = b.add(pickMaxOfThree)

    Source.single(1) ~> pm3.in(0)
    Source.single(2) ~> pm3.in(1)
    Source.single(3) ~> pm3.in(2)
    pm3.out ~> sink.in
    ClosedShape
  })

  val max: Future[Int] = partialConstruction.run()
  max.onComplete(x => println(x))

  // constructing sources, sinks and flows from partial graphs.
  val pairs = Source.fromGraph(GraphDSL.create() { implicit b =>
    import GraphDSL.Implicits._

    val zip = b.add(Zip[Int, Int]())
    def ints = Source.fromIterator(() => Iterator.from(1))

    // takes odds
    ints.filter(_ % 2 != 0) ~> zip.in0
    // takes evens
    ints.filter(_ % 2 == 0) ~> zip.in1

    SourceShape(zip.out)
  })

  val firstPair: Future[(Int, Int)] = pairs.runWith(Sink.head)

  // Expect this to be (1,2)
  firstPair.onComplete(x => println(x))
}
