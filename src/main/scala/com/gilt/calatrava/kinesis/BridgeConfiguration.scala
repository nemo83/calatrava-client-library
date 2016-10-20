package com.gilt.calatrava.kinesis

import com.amazonaws.auth.{AWSCredentialsProvider, DefaultAWSCredentialsProviderChain, STSAssumeRoleSessionCredentialsProvider}

/**
  * Configuration for the Calatrava Bridge that we want to consume
  *
  * @param clientAppName              a name for this application, must be unique for this Bridge
  * @param streamName                 the name of the Kinesis stream associated with the Bridge
  * @param bucketName                 the name of the S3 bucket where the Bridge stores large events
  * @param kinesisCredentialsProvider Credentials Provider to get access to AWS Kinesis
  * @param dynamoCredentialsProvider  Credentials Provider to get access to AWS Dynamo
  * @param metricsConfigOpt           optional configuration for CloudWatch metrics factory
  */
case class BridgeConfiguration(clientAppName: String,
                               streamName: String,
                               bucketName: String,
                               kinesisCredentialsProvider: AWSCredentialsProvider,
                               dynamoCredentialsProvider: AWSCredentialsProvider,
                               metricsConfigOpt: Option[MetricsConfiguration] = None)

case class BridgeConfigurationBuilder(clientAppName: String,
                                      streamName: String,
                                      bucketName: String,
                                      kinesisCredentialsProviderOpt: Option[AWSCredentialsProvider] = None,
                                      dynamoCredentialsProviderOpt: Option[AWSCredentialsProvider] = None,
                                      metricsConfigOpt: Option[MetricsConfiguration] = None) {

  def withKinesisCredentialsProvider(kinesisCredentialsProvider: AWSCredentialsProvider) = this.copy(kinesisCredentialsProviderOpt = Some(kinesisCredentialsProvider))

  def withKinesisRoleIamArn(kinesisRoleIamArn: String) = this.copy(kinesisCredentialsProviderOpt = Some(new STSAssumeRoleSessionCredentialsProvider(kinesisRoleIamArn, clientAppName)))

  def withDynamoCredentialsProvider(dynamoCredentialsProvider: AWSCredentialsProvider) = this.copy(dynamoCredentialsProviderOpt = Some(dynamoCredentialsProvider))

  def withDynamoRoleIamArn(dynamoRoleIamArn: String) = this.copy(dynamoCredentialsProviderOpt = Some(new STSAssumeRoleSessionCredentialsProvider(dynamoRoleIamArn, clientAppName)))

  def withMetricsConfig(metricsConfiguration: MetricsConfiguration) = this.copy(metricsConfigOpt = Some(metricsConfiguration))

  def toBridgeConfiguration =
    BridgeConfiguration(
      clientAppName,
      streamName,
      bucketName,
      kinesisCredentialsProviderOpt.getOrElse(new DefaultAWSCredentialsProviderChain),
      dynamoCredentialsProviderOpt.getOrElse(new DefaultAWSCredentialsProviderChain),
      metricsConfigOpt
    )
}

object BridgeConfigurationBuilder {

  def aBridgeConfiguration(clientAppName: String, streamName: String, bucketName: String) =
    BridgeConfigurationBuilder(clientAppName, streamName, bucketName)

}


/**
 * If provided, this configuration enables KCL sending its own metrics to CloudWatch
 *
 * For detailed information on KCL metrics, see
 * http://docs.aws.amazon.com/kinesis/latest/dev/monitoring-with-kcl.html
 *
 * @param cloudwatchIamRoleArnOpt   the name of an IAM role that this application can assume to get access to AWS CloudWatch
 * @param metricsNamespace          the namespace under which the metrics will appear in the CloudWatch console
 * @param metricsBufferTimeMillis   time to buffer metrics before publishing to CloudWatch
 * @param metricsBufferSize         maximum number of metrics that we can have in a queue
 */
case class MetricsConfiguration(cloudwatchIamRoleArnOpt: Option[String],
                                metricsNamespace: String,
                                metricsBufferTimeMillis: Long,
                                metricsBufferSize: Int)

