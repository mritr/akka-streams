package akka.stream.alpakka.sqs.newdsl

import akka.NotUsed
import akka.stream.scaladsl.Flow
import software.amazon.awssdk.services.sqs.SqsAsyncClient
import software.amazon.awssdk.services.sqs.model.DeleteMessageRequest

import scala.compat.java8.FutureConverters._
import scala.concurrent.{ExecutionContext, Future}

/**
  * Author: Nicholas Connor
  * Date: 3/6/19
  * Package: akka.stream.alpakka.sqs.scaladsl
  */
object SqsAckFlow {

  def async(parallelism: Int)
           (implicit sqsAsyncClient: SqsAsyncClient): Flow[MessageAction, ActionResult, NotUsed] =
    Flow[MessageAction]
      .mapAsync(parallelism) {
        case delete: MessageAction.Delete =>
          sqsAsyncClient.deleteMessage(
            DeleteMessageRequest.builder()
              .queueUrl(delete.queueUrl)
              .receiptHandle(delete.message.receiptHandle)
              .build()
          ).thenApply[ActionResult](f => ActionResult.Receipt(f.sdkHttpResponse(), f.responseMetadata()))
          .toScala
      }

  def ignored(parallelism: Int)
           (implicit sqsAsyncClient: SqsAsyncClient, ec: ExecutionContext): Flow[Option[MessageAction], ActionResult, NotUsed] =
    Flow[Option[MessageAction]]
      .mapAsyncUnordered(parallelism) {
            case delete: Option[MessageAction.Delete] =>
              if (delete.nonEmpty) {
                sqsAsyncClient.deleteMessage(
                  DeleteMessageRequest.builder()
                    .queueUrl(delete.get.queueUrl)
                    .receiptHandle(delete.get.message.receiptHandle)
                    .build()
                ).thenApply[ActionResult](_ => ActionResult.Ignored)
                  .toScala
              } else {
                Future { ActionResult.Ignored }
              }
          }
}
