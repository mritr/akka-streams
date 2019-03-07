package com.mritr.akka.streams

import java.net.URI

import akka.NotUsed
import akka.actor.ActorSystem
import akka.stream._
import akka.stream.alpakka.sqs.newdsl._
import akka.stream.scaladsl._
import software.amazon.awssdk.auth.credentials.{AwsBasicCredentials, StaticCredentialsProvider}
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.sqs.SqsAsyncClient

import concurrent.duration._
import scala.util.Random

object SourceToASource extends App {

  implicit val system = ActorSystem("SqsAttemptOne")
  implicit val materializer = ActorMaterializer()
  implicit val ec = system.dispatcher

  val sqsEndpoint = "http://localhost:4100"
//  val sqsRegion = "us-east-1"
  val creds = AwsBasicCredentials.create("","")

  implicit val awsSqsClientU: SqsAsyncClient = SqsAsyncClient.builder()
    .credentialsProvider(StaticCredentialsProvider.create(creds))
    .region(Region.US_EAST_1).build()

  // Initial source refreshes every n minutes to get list of applicable queues.

  /** Replace this with method that will
    *
    * @return
    */
  /*
  def getQueuesStub(): List[String] = {
    List(
      s"development-queue-${Math.abs(Random.nextInt())}",
      s"development-queue-${Math.abs(Random.nextInt())}",
      s"development-queue-${Math.abs(Random.nextInt())}",
      s"development-queue-${Math.abs(Random.nextInt())}",
      s"development-queue-${Math.abs(Random.nextInt())}"
    )
  }
  */

  def getQueuesStub(): List[String] = {
    List(
      "http://localhost/development-queue-1",
      "http://localhost/development-queue-2",
      "http://localhost/development-queue-3",
      "http://localhost/development-queue-4",
      "http://localhost/development-queue-5"
    )
  }


  val workerLoop =
    Source.tick(0.second, 5.second, NotUsed)
    .map(_ => getQueuesStub()).mapConcat(identity)
    .map(MessageRequest.Get(_, 1))
    .mapZipVia(SqsGetFlow(4))
    // will this provide backpressure to the top
    .map{ case (queueUrl, msg) =>
      if (msg.nonEmpty) {
        println(s"Got message $msg")
        Thread.sleep(5000)
        println(s"Done working on $msg")
        Some(MessageAction.Delete(msg.get, queueUrl.queueUrl))
      } else None
    }.runWith(SqsAckSink(50))

}

