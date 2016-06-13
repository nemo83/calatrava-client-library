package com.gilt.calatrava.kinesis

import java.net.InetAddress
import java.util.UUID
import javax.inject.{Inject, Singleton}

import com.amazonaws.auth.{DefaultAWSCredentialsProviderChain, STSAssumeRoleSessionCredentialsProvider}
import com.amazonaws.services.kinesis.clientlibrary.lib.worker.{InitialPositionInStream, KinesisClientLibConfiguration, Worker}
import com.amazonaws.services.kinesis.metrics.impl.{CWMetricsFactory, NullMetricsFactory}
import com.amazonaws.services.s3.AmazonS3Client

@Singleton
class BridgeWorkerFactory @Inject() (calatravaEventProcessor: CalatravaEventProcessor,
                                     bridgeConfiguration: BridgeConfiguration) extends WorkerFactory {

  private[this] def getCredentialsProvider(iamRoleArnOpt: Option[String]) = iamRoleArnOpt map { iamRoleArn =>
    new STSAssumeRoleSessionCredentialsProvider(iamRoleArn, bridgeConfiguration.clientAppName)
  } getOrElse new DefaultAWSCredentialsProviderChain

  private[this] val kinesisCredentialsProvider = getCredentialsProvider(bridgeConfiguration.kinesisIamRoleArnOpt)
  private[this] val dynamoCredentialsProvider = getCredentialsProvider(bridgeConfiguration.dynamoIamRoleArnOpt)
  private[this] val cloudWatchCredentialsProvider = getCredentialsProvider(bridgeConfiguration.metricsConfigOpt.flatMap(_.cloudwatchIamRoleArnOpt))

  private[this] val s3Client = new AmazonS3Client(kinesisCredentialsProvider)

  private[this] val workerId = s"${InetAddress.getLocalHost.getCanonicalHostName}:${UUID.randomUUID()}"

  override def instance() = new Worker.Builder()
    .recordProcessorFactory(createRecordProcessorFactory())
    .config(createKinesisClientLibConfiguration())
    .metricsFactory(createMetricsFactory())
    .build()

  private[this] def createRecordProcessorFactory() =
    new RecordProcessorFactory(calatravaEventProcessor, s3Client, bridgeConfiguration.bucketName)

  private[this] def createKinesisClientLibConfiguration() =
    new KinesisClientLibConfiguration(
      bridgeConfiguration.clientAppName,
      bridgeConfiguration.streamName,
      kinesisCredentialsProvider,
      dynamoCredentialsProvider,
      cloudWatchCredentialsProvider,
      workerId)
      .withInitialPositionInStream(InitialPositionInStream.TRIM_HORIZON)

  private[this] def createMetricsFactory() =
    bridgeConfiguration.metricsConfigOpt map { metricsConfig =>
      new CWMetricsFactory(
        cloudWatchCredentialsProvider,
        metricsConfig.metricsNamespace,
        metricsConfig.metricsBufferTimeMillis,
        metricsConfig.metricsBufferSize)
    } getOrElse new NullMetricsFactory

}
