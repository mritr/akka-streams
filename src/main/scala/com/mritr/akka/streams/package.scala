package com.mritr.akka

import akka.NotUsed
import akka.actor.Cancellable
import akka.stream.SourceShape
import akka.stream.scaladsl.{Broadcast, Flow, GraphDSL, Source, Zip}

package object streams {

  implicit class SourceOp[In](source: Source[In, Cancellable]) {
    def mapZipVia[Out](mapFn: Flow[In, Out, NotUsed]) = {
      Source.fromGraph(GraphDSL.create() { implicit builder: GraphDSL.Builder[NotUsed] =>
        import GraphDSL.Implicits._
        val broadcast = builder.add(Broadcast[In](2))
        val zip = builder.add(Zip[In, Out])
        source ~> broadcast ~> zip.in0
        broadcast ~> mapFn ~> zip.in1
        SourceShape(zip.out)
      })
    }
  }

  implicit class SourceFromSeq[T](seq: Seq[T]) {
    def toSource: Source[T, NotUsed] =
      Source.fromIterator{() => seq.toIterator}
  }

}
