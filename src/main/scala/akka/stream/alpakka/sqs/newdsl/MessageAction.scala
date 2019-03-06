package akka.stream.alpakka.sqs.newdsl

import software.amazon.awssdk.services.sqs.model.Message


/**
  * Thin Scala wrapper over SDK messages.
  */
trait MessageAction {
  val message: Message
  val queueUrl: String
}

object MessageAction {
  case class Delete(message: Message, queueUrl: String) extends MessageAction
}
