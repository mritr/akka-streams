package com.mritr.akka.streams

import akka.Done
import akka.actor.ActorSystem
import akka.stream._
import akka.stream.alpakka.sqs.{MessageAction, SqsSourceSettings}
import akka.stream.alpakka.sqs.scaladsl.{SqsAckSink, SqsPublishSink, SqsSource}
import akka.stream.scaladsl._
import com.amazonaws.client.builder.AwsClientBuilder.EndpointConfiguration
import com.amazonaws.services.sqs.model.Message
import com.amazonaws.services.sqs.{AmazonSQSAsync, AmazonSQSAsyncClientBuilder}

import scala.concurrent.{Await, Future}
import scala.util.Random
import scala.concurrent.duration._

/** Be running: ./goaws
  *
  * Then execute the bash script in the bin directory (run from there, it needs a json file in that directory)
  * ./create-queues.sh
  */
object SqsAttemptOne extends App {
  implicit val system = ActorSystem("SqsAttemptOne")
  implicit val materializer = ActorMaterializer()
  implicit val ec = system.dispatcher

  val sqsEndpoint = "http://localhost:4100"
  val sqsRegion = "us-east-1"

  val queueUrls = IndexedSeq(
    "http://localhost:4100/development-queue1",
    "http://localhost:4100/development-queue2",
    "http://localhost:4100/development-queue3",
    "http://localhost:4100/development-queue4",
    "http://localhost:4100/development-queue5"
  )

  implicit val awsSqsClient: AmazonSQSAsync = AmazonSQSAsyncClientBuilder
    .standard()
    .withEndpointConfiguration(new EndpointConfiguration(
      sqsEndpoint, sqsRegion
    )).build()

  private def loadMessages(urls: Seq[String], messages: Seq[String]): Seq[Future[Done]] = {
    messages.map(msg => {
      val queueUrl = getQueueUrl(urls)
      println(s"Loading message: $msg into queue: $queueUrl")
      Source.single(msg).runWith(SqsPublishSink(queueUrl))
    })
  }

  private def getQueueUrl(queueUrls: Seq[String]): String = {
    val rand = new Random(System.currentTimeMillis())
    val idx = rand.nextInt(queueUrls.length)
    queueUrls(idx)
  }

  private def generateMessages(n: Int): Seq[String] = {
    Range.inclusive(0, n).map(idx => s"This is a test message: $idx")
  }

  val workToDo = Flow[Message].map(msg => {
    println(s"Got message $msg")
    Thread.sleep(5000)
    println(s"Done working on $msg")
    msg
  }).map(msg => MessageAction.Delete(msg)).async
    .addAttributes(Attributes.inputBuffer(1, 4))

  // Load test messages
  val loadMessageJob = loadMessages(queueUrls, generateMessages(queueUrls.length * 3))

  println("Loading messages")
  loadMessageJob.map(Await.ready(_, 20.seconds))
  println("Done loading messages")

  val sourcesAndSinks = queueUrls.map(queueUrl =>
    (SqsSource(queueUrl, SqsSourceSettings().withCloseOnEmptyReceive(false).withWaitTime(1.second)),
    SqsAckSink(queueUrl))
  )

  val graph = RunnableGraph.fromGraph(GraphDSL.create() { implicit b =>
    import GraphDSL.Implicits._

    // Hardcoded
    sourcesAndSinks.foreach(pair => {
      val source = pair._1
      val sink = pair._2
        source ~> workToDo ~> sink
    })

    ClosedShape
  })

  val job = graph.run()

  // Just forcefully kill this will not stop due to the closeOnEmptyReceive value.

}
