package com.gilt.calatrava.kinesis

import org.scalatest.{BeforeAndAfterEach, Matchers, WordSpec}
import org.scalatest.mock.MockitoSugar
import org.mockito.Mockito._

class BridgeWorkerFactorySpec extends WordSpec with MockitoSugar with Matchers with BeforeAndAfterEach {

  val mockEventProcessor = mock[CalatravaEventProcessor]
  val mockConfiguration = mock[BridgeConfiguration]
  val mockMetricsConfig = mock[MetricsConfiguration]

  val appName = "test-application"
  val bucketName = "test-bucket"
  val streamName = "test-stream"

  val metricsNamespace = "test-metrics-ns"
  val metricsBufferTimeMillis = 1000L
  val metricsBufferSize = 1

  override def beforeEach() = {
    reset(mockEventProcessor, mockConfiguration)
    when(mockConfiguration.clientAppName).thenReturn(appName)
    when(mockConfiguration.bucketName).thenReturn(bucketName)
    when(mockConfiguration.streamName).thenReturn(streamName)

    when(mockMetricsConfig.metricsNamespace).thenReturn(metricsNamespace)
    when(mockMetricsConfig.metricsBufferTimeMillis).thenReturn(metricsBufferTimeMillis)
    when(mockMetricsConfig.metricsBufferSize).thenReturn(metricsBufferSize)
  }

  "BridgeWorkerFactory" must {

    "create a Worker with DefaultAWSCredentialsProviderChain if no IAM Role Arn provided" in {

      when(mockConfiguration.iamRoleArnOpt).thenReturn(None)
      when(mockConfiguration.metricsConfigOpt).thenReturn(None)

      val worker = new BridgeWorkerFactory(mockEventProcessor, mockConfiguration).instance()

      worker.getApplicationName should be(appName)
    }

    "create a Worker with STS credentials provider if an IAM Role Arn is provided" in {
      when(mockConfiguration.iamRoleArnOpt).thenReturn(Some("arn:fake:value"))
      when(mockConfiguration.metricsConfigOpt).thenReturn(None)

      val worker = new BridgeWorkerFactory(mockEventProcessor, mockConfiguration).instance()

      worker.getApplicationName should be(appName)
    }

    "create a Worker reporting metrics to CloudWatch, if so configured" in {
      when(mockConfiguration.iamRoleArnOpt).thenReturn(None)
      when(mockConfiguration.metricsConfigOpt).thenReturn(Some(mockMetricsConfig))

      val worker = new BridgeWorkerFactory(mockEventProcessor, mockConfiguration).instance()

      worker.getApplicationName should be(appName)
    }
  }
}
