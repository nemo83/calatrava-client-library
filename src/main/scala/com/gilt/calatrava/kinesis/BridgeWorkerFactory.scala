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

  private[this] val credentialsProvider = bridgeConfiguration.iamRoleArnOpt map { iamRoleArn =>
    new STSAssumeRoleSessionCredentialsProvider(iamRoleArn, bridgeConfiguration.clientAppName)
  } getOrElse new DefaultAWSCredentialsProviderChain

  private[this] val s3Client = new AmazonS3Client(credentialsProvider)

  private[this] val workerId = s"${InetAddress.getLocalHost.getCanonicalHostName}:${UUID.randomUUID()}"

  override def instance() = new Worker(
    createRecordProcessorFactory(),
    createKinesisClientLibConfiguration(),
    createMetricsFactory())

  private[this] def createRecordProcessorFactory() =
    new RecordProcessorFactory(calatravaEventProcessor, s3Client, bridgeConfiguration.bucketName)

  private[this] def createKinesisClientLibConfiguration() =
    new KinesisClientLibConfiguration(bridgeConfiguration.clientAppName, bridgeConfiguration.streamName, credentialsProvider, workerId)
      .withInitialPositionInStream(InitialPositionInStream.TRIM_HORIZON)

  private[this] def createMetricsFactory() =
    bridgeConfiguration.metricsConfigOpt map { metricsConfig =>
      new CWMetricsFactory(
        credentialsProvider,
        metricsConfig.metricsNamespace,
        metricsConfig.metricsBufferTimeMillis,
        metricsConfig.metricsBufferSize)
    } getOrElse new NullMetricsFactory

}
