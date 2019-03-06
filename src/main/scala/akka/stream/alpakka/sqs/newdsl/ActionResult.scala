package akka.stream.alpakka.sqs.newdsl

import software.amazon.awssdk.http.SdkHttpResponse
import software.amazon.awssdk.services.sqs.model.SqsResponseMetadata

/**
  * Author: Nicholas Connor
  * Date: 3/6/19
  * Package: akka.stream.alpakka.sqs.scaladsl
  */
trait ActionResult

object ActionResult {

  case object Ignored extends ActionResult
  case class Receipt(
                      httpResponse: SdkHttpResponse,
                      metadata: SqsResponseMetadata
                    ) extends ActionResult
}
