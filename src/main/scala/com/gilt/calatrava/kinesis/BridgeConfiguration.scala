package com.gilt.calatrava.kinesis

/**
 * Configuration for the Calatrava Bridge that we want to consume
 *
 * @param clientAppName             a name for this application, must be unique for this Bridge
 * @param streamName                the name of the Kinesis stream associated with the Bridge
 * @param bucketName                the name of the S3 bucket where the Bridge stores large events
 * @param iamRoleArnOpt             the name of an IAM role that this application can assume to get access to AWS
 * @param metricsConfigOpt          optional configuration for CloudWatch metrics factory
 */
case class BridgeConfiguration(clientAppName: String,
                               streamName: String,
                               bucketName: String,
                               iamRoleArnOpt: Option[String],
                               metricsConfigOpt: Option[MetricsConfiguration])

/**
 * If provided, this configuration enables KCL sending its own metrics to CloudWatch
 *
 * For detailed information on KCL metrics, see
 * http://docs.aws.amazon.com/kinesis/latest/dev/monitoring-with-kcl.html
 *
 * @param metricsNamespace          the namespace under which the metrics will appear in the CloudWatch console
 * @param metricsBufferTimeMillis   time to buffer metrics before publishing to CloudWatch
 * @param metricsBufferSize         maximum number of metrics that we can have in a queue
 */
case class MetricsConfiguration(metricsNamespace: String,
                                metricsBufferTimeMillis: Long,
                                metricsBufferSize: Int)

