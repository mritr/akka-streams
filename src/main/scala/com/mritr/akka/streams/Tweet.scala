package com.mritr.akka.streams

import akka.NotUsed
import akka.actor.ActorSystem
import akka.stream.{ActorMaterializer, ClosedShape, IOResult}
import akka.stream.scaladsl.{Broadcast, Flow, GraphDSL, RunnableGraph, Sink, Source}

import scala.concurrent.Future

final case class Author(handle: String)

final case class Hashtag(name: String)

final case class Tweet(author: Author, timestamp: Long, body: String) {

  def hashtags: Set[Hashtag] = body.split(" ").collect {
    case t if t.startsWith("#") => Hashtag(t.replaceAll("[^#\\w]", ""))
  }.toSet

}

object TweetMain extends App {
  val akkaTag = Hashtag("#akka")

  implicit val system = ActorSystem("reactive-tweets")
  implicit val materializer = ActorMaterializer()

  val tweets: Source[Tweet, NotUsed] = Source(List(
    Tweet(Author("mritr"), System.currentTimeMillis(), "Hey #akka #hashtag #woot"),
    Tweet(Author("jsUser"), System.currentTimeMillis(), "another tweet #ok")
  ))

  val authors: Source[Author, NotUsed] = tweets
    .filter(_.hashtags.contains(akkaTag))
    .map(_.author)

  //authors.runWith(Sink.foreach(println))
  authors.runForeach(println)

  // mapConcat is similar to flatMap
  val hashtags: Source[Hashtag, NotUsed] = tweets.mapConcat(_.hashtags.toList)

  hashtags.runForeach(println)

  // Graphs allow for more complex flows
  val writeAuthors: Sink[String, Future[IOResult]] = QuickStart.lineSink("authors.txt")
  val writeHashtags: Sink[String, Future[IOResult]] = QuickStart.lineSink("hashtags.txt")

  val g = RunnableGraph.fromGraph(GraphDSL.create() { implicit b =>
    import GraphDSL.Implicits._

    val bcast = b.add(Broadcast[Tweet](2))
    tweets ~> bcast.in
    bcast.out(0) ~> Flow[Tweet].map(_.author.toString) ~> writeAuthors
    bcast.out(1) ~> Flow[Tweet].mapConcat(_.hashtags.toList).map(_.toString) ~> writeHashtags
    ClosedShape
  })
  // This writes out the 2 files.
  g.run()
}
