package com.gilt.calatrava.kinesis

import java.io.InputStream

import com.amazonaws.services.kinesis.clientlibrary.interfaces.{IRecordProcessor, IRecordProcessorFactory}
import com.amazonaws.services.s3.AmazonS3Client
import com.amazonaws.services.s3.model.S3Object
import com.gilt.calatrava.v0.models.json._
import com.gilt.calatrava.v0.models.{ChangeEvent, SinkEvent}
import com.gilt.gfc.logging.Loggable
import com.gilt.gfc.util.Retry
import play.api.libs.json.Json

import scala.concurrent.duration._

private[kinesis] class RecordProcessorFactory (calatravaEventProcessor: CalatravaEventProcessor,
                              s3Client: AmazonS3Client,
                              bucketName: String) extends IRecordProcessorFactory with Loggable {

  private[kinesis] val MaxRetryTimes = 10
  private[kinesis] val InitialDelay = 1.second

  override def createProcessor(): IRecordProcessor = new RecordProcessor(createSinkEventProcessor())

  /**
   * Create a SinkEventProcessor for Calatrava events capable to fetch large events from S3, as needed
   */
  private[kinesis] def createSinkEventProcessor() = new SinkEventProcessor {
    override def processEvent(event: SinkEvent): Boolean = (event.event, event.eventObjectKey) match {
      case (Some(changeEvent), None) => calatravaEventProcessor.processEvent(changeEvent)
      case (None, Some(objectKey)) => fetchChangeEvent(objectKey) exists calatravaEventProcessor.processEvent
      case _ =>
        warn(s"Ignoring invalid event $event")
        true
    }
  }

  /**
   * Fetch an object from S3, retrying up to 10 times with exponential delay, and convert it to a ChangeEvent
   *
   * @param objectKey  the S3 object key
   * @return           the parsed ChangeEvent from the data in S3
   */
  private[kinesis] def fetchChangeEvent(objectKey: String): Option[ChangeEvent] =
    try {
      val s3Object = Retry.retryWithExponentialDelay(maxRetryTimes = MaxRetryTimes, initialDelay = InitialDelay) {
        s3Client.getObject(bucketName, objectKey)
      }(_ => ())

      Json.parse(getObjectContent(s3Object)).asOpt[ChangeEvent]
    } catch {
      case e: Exception =>
        warn(s"Failed to read and parse object with key $objectKey", e)
        None
    }

  private[kinesis] def getObjectContent(s3Object: S3Object): InputStream = s3Object.getObjectContent
}