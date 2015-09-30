package com.gilt.calatrava.kinesis

import java.io.{ByteArrayInputStream, InputStream}

import com.amazonaws.AmazonServiceException
import com.amazonaws.services.s3.AmazonS3Client
import com.amazonaws.services.s3.model.S3Object
import com.gilt.calatrava.v0.models.{SinkEvent, ChangeEvent}
import com.gilt.calatrava.v0.models.json._
import org.joda.time.DateTime
import org.mockito.Mockito._
import org.mockito.Matchers.{any, anyString}
import org.scalatest.mock.MockitoSugar
import org.scalatest.{BeforeAndAfterEach, Matchers, WordSpec}
import play.api.libs.json.Json

import scala.concurrent.duration._

class RecordProcessorFactorySpec extends WordSpec with MockitoSugar with Matchers with BeforeAndAfterEach {

  val mockEventProcessor = mock[CalatravaEventProcessor]
  val mockS3Client = mock[AmazonS3Client]
  val mockS3Object = mock[S3Object]
  val mockFetcher = mock[String => Option[ChangeEvent]]
  val bucketName = "SomeBucket"

  override def beforeEach() = {
    reset(mockEventProcessor, mockS3Client, mockS3Object, mockFetcher)
  }

  class RecordProcessorFactoryStub(s3result: String) extends RecordProcessorFactory(mockEventProcessor, mockS3Client, bucketName) {
    override def getObjectContent(s3Object: S3Object): InputStream = new ByteArrayInputStream(s3result.getBytes)
    override val MaxRetryTimes = 3
    override val InitialDelay = 1.millisecond
  }

  val objectKey = "object-key"
  val changeEvent = ChangeEvent("123", Some("12345"), Some(""), Some(""), new DateTime())
  val changeEventJson = Json.toJson(changeEvent).toString()

  "fetchChangeEvent" must {

    "convert the S3 data into a ChangeEvent" in {
      val processorFactoryStub = new RecordProcessorFactoryStub(changeEventJson)
      when(mockS3Client.getObject(bucketName, objectKey)).thenReturn(mockS3Object)
      processorFactoryStub.fetchChangeEvent(objectKey) should be(Some(changeEvent))
    }

    "retry exactly 3 times, with exponential delay, if S3 errors out" in {
      when(mockS3Client.getObject(bucketName, objectKey)).thenThrow(new AmazonServiceException("Boom!"))

      val processorFactoryStub = new RecordProcessorFactoryStub(changeEventJson)
      processorFactoryStub.fetchChangeEvent(objectKey) should be(None)
      verify(mockS3Client, times(4)).getObject(bucketName, objectKey)
    }
  }


  class RecordProcessorFactoryShunt(fetcher: String => Option[ChangeEvent]) extends RecordProcessorFactory(mockEventProcessor, mockS3Client, bucketName) {
    override def fetchChangeEvent(objectKey: String): Option[ChangeEvent] = fetcher(objectKey)
  }

  "createSinkEventProcessor" must {

    "process a ChangeEvent, if present" in {
      val processor = new RecordProcessorFactoryShunt(mockFetcher).createSinkEventProcessor()

      processor.processEvent(SinkEvent(Some(changeEvent), None))
      verify(mockEventProcessor, times(1)).processEvent(changeEvent)
      verify(mockFetcher, never()).apply(anyString)
    }

    "fetch a ChangeEvent from S3, if the key is present" in {
      when(mockFetcher.apply(objectKey)).thenReturn(Some(changeEvent))
      val processor = new RecordProcessorFactoryShunt(mockFetcher).createSinkEventProcessor()

      processor.processEvent(SinkEvent(None, Some(objectKey)))
      verify(mockEventProcessor, times(1)).processEvent(changeEvent)
      verify(mockFetcher, times(1)).apply(objectKey)
    }

    "skip ill formatted SinkEvent" in {
      val processor = new RecordProcessorFactoryShunt(mockFetcher).createSinkEventProcessor()
      processor.processEvent(SinkEvent(None, None))
      verify(mockFetcher, never()).apply(anyString)
      verify(mockEventProcessor, never()).processEvent(any[ChangeEvent])
    }
  }

  "RecordProcessorFactory" must {
    "create a processor" in {
      val factory = new RecordProcessorFactory(mockEventProcessor, mockS3Client, bucketName)
      factory.createProcessor() should not be null
    }
  }
}
