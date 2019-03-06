package com.mritr.akka.streams

import akka.NotUsed
import akka.actor.{ActorSystem, Cancellable}
import akka.stream.{ActorMaterializer, Attributes, ClosedShape}
import akka.stream.alpakka.sqs.{MessageAction, SqsSourceSettings}
import akka.stream.alpakka.sqs.scaladsl.SqsSource
import akka.stream.scaladsl._
import com.amazonaws.client.builder.AwsClientBuilder.EndpointConfiguration
import com.amazonaws.services.sqs.model.Message
import com.amazonaws.services.sqs.{AmazonSQSAsync, AmazonSQSAsyncClientBuilder}

import concurrent.duration._
import scala.util.Random

object SourceToASource extends App {

  implicit val system = ActorSystem("SqsAttemptOne")
  implicit val materializer = ActorMaterializer()
  implicit val ec = system.dispatcher

  val sqsEndpoint = "http://localhost:4100"
  val sqsRegion = "us-east-1"

  implicit val awsSqsClient: AmazonSQSAsync = AmazonSQSAsyncClientBuilder
    .standard()
    .withEndpointConfiguration(new EndpointConfiguration(
      sqsEndpoint, sqsRegion
    )).build()

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

  val queueSource: Source[String, Cancellable] = Source.tick(1.second, 30.second, NotUsed).map(_ => getQueuesStub()).mapConcat(identity)
  // This refreshes every 30 seconds
  queueSource.runForeach(println)

  // Construct sqs sources
  val sqsSources: Source[Message, Cancellable] = queueSource.map(queueUrl => SqsSource(queueUrl, SqsSourceSettings().withCloseOnEmptyReceive(false).withWaitTime(1.second))).flatMapConcat(identity)

  val workToDo: Flow[Message, MessageAction, NotUsed] = Flow[Message].map(msg => {
    println(s"Got message $msg")
    Thread.sleep(5000)
    println(s"Done working on $msg")
    msg
  }).map(msg => MessageAction.Delete(msg)).async
    .addAttributes(Attributes.inputBuffer(1, 4))

  val runnableGraph = RunnableGraph.fromGraph(GraphDSL.create() { implicit builder: GraphDSL.Builder[NotUsed] =>
    import GraphDSL.Implicits._

    // Need to link corresponding Sink
    sqsSources ~> workToDo ~> Sink.ignore

    ClosedShape
  })

}
