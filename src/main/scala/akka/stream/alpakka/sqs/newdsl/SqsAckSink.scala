package akka.stream.alpakka.sqs.newdsl

import akka.Done
import akka.stream.scaladsl.{Keep, Sink}
import software.amazon.awssdk.services.sqs.SqsAsyncClient

import scala.concurrent.{ExecutionContext, Future}


/**
  * Author: Nicholas Connor
  * Date: 3/6/19
  * Package: akka.stream.alpakka.sqs.scaladsl
  */


object SqsAckSink {
  def apply(parallelism: Int)
           (implicit sqsAsyncClient: SqsAsyncClient): Sink[MessageAction, Future[Done]] =
    SqsAckFlow.ignored(parallelism).toMat(Sink.ignore)(Keep.right)
}
