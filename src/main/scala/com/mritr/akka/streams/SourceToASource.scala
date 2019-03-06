package com.mritr.akka.streams

import akka.NotUsed
import akka.actor.ActorSystem
import akka.stream._
import akka.stream.alpakka.sqs.newdsl.{MessageAction, MessageRequest, SqsAckSink, SqsGetFlow}
import akka.stream.scaladsl._
import software.amazon.awssdk.services.sqs.SqsAsyncClient

import concurrent.duration._
import scala.util.Random

import com.mritr.akka.streams._

object SourceToASource extends App {

  implicit val system = ActorSystem("SqsAttemptOne")
  implicit val materializer = ActorMaterializer()
  implicit val ec = system.dispatcher

  val sqsEndpoint = "http://localhost:4100"
  val sqsRegion = "us-east-1"


  implicit val awsSqsClientU: SqsAsyncClient = ???

  // Initial source refreshes every n minutes to get list of applicable queues.

  /** Replace this with method that will
    *
    * @return
    */
  def getQueuesStub(): List[String] = {
    List(
      s"development-queue-${Math.abs(Random.nextInt())}",
      s"development-queue-${Math.abs(Random.nextInt())}",
      s"development-queue-${Math.abs(Random.nextInt())}",
      s"development-queue-${Math.abs(Random.nextInt())}",
      s"development-queue-${Math.abs(Random.nextInt())}"
    )
  }


  val workerLoop =
    Source.tick(1.second, 30.second, NotUsed)
    .map(_ => getQueuesStub()).mapConcat(identity)
    .map(MessageRequest.Get(_, 10))
    .mapZipVia(SqsGetFlow(4))
    // will this provide backpressure to the top
    .map{ case (queueUrl, msg) =>
      println(s"Got message $msg")
      Thread.sleep(5000)
      println(s"Done working on $msg")
      MessageAction.Delete(msg, queueUrl.queueUrl)
    }
    .runWith(SqsAckSink(50))

}

