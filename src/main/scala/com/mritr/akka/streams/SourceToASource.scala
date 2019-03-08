package com.mritr.akka.streams

import akka.NotUsed
import akka.actor.ActorSystem
import akka.stream._
import akka.stream.alpakka.sqs.newdsl._
import akka.stream.scaladsl._
import software.amazon.awssdk.auth.credentials.{AwsBasicCredentials, StaticCredentialsProvider}
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.sqs.SqsAsyncClient

import concurrent.duration._

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
    println("getting queues")
    List(
    )
  }

  val workerLoop =
    Source.tick(0.second, 5.second, NotUsed)
    .map(_ => getQueuesStub()).mapConcat(identity)
    .map(MessageRequest.Get(_, 10))
    .via(SqsGetFlow(4))
    .map{ case (msgWithRequest) =>
        println(s"Got message ${msgWithRequest.message}")
        Thread.sleep(5000)
        println(s"Done working on ${msgWithRequest.message}")
        MessageAction.Delete(msgWithRequest.message, msgWithRequest.messageRequest.queueUrl)
    }.runWith(SqsAckSink(50))

}

