package com.gilt.calatrava.kinesis

trait MetricsConfiguration {
  def metricsNamespace: String
  def metricsBufferTimeMillis: Long
  def metricsBufferSize: Int
}

trait BridgeConfiguration {
  def clientAppName: String
  def streamName: String
  def bucketName: String
  def iamRoleArnOpt: Option[String]

  def metricsConfigOpt: Option[MetricsConfiguration]
}
