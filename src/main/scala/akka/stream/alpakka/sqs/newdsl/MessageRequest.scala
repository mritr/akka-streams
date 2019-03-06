package akka.stream.alpakka.sqs.newdsl

/**
  * Author: Nicholas Connor
  * Date: 3/6/19
  * Package: akka.stream.alpakka.sqs.newdsl
  */
trait MessageRequest

object MessageRequest {
  case class Get(queueUrl: String, limit: Int)
}
