package com.mritr.akka.streams

import akka.NotUsed
import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{Sink, Source}

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
    Tweet(Author("mritr"), System.currentTimeMillis(), "Hey #akka"),
    Tweet(Author("jsUser"), System.currentTimeMillis(), "another tweet")
  ))

  val authors: Source[Author, NotUsed] = tweets
    .filter(_.hashtags.contains(akkaTag))
    .map(_.author)

  //authors.runWith(Sink.foreach(println))
  authors.runForeach(println)
}
