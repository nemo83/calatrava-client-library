package com.gilt.calatrava.kinesis

import com.amazonaws.services.kinesis.clientlibrary.interfaces.{IRecordProcessor, IRecordProcessorCheckpointer}
import com.amazonaws.services.kinesis.clientlibrary.types.ShutdownReason
import com.amazonaws.services.kinesis.model.Record
import com.fasterxml.jackson.core.JsonParseException
import com.gilt.calatrava.v0.models.SinkEvent
import com.gilt.calatrava.v0.models.json._
import com.gilt.gfc.logging.Loggable
import com.gilt.gfc.util.Retry
import play.api.libs.json.Json

import scala.collection.JavaConverters._
import scala.concurrent.duration._
import scala.util.Try

private[kinesis] class RecordProcessor(sinkEventProcessor: SinkEventProcessor) extends IRecordProcessor with Loggable {

  private[this] var shardId: String = _
  private[this] var nextCheckpointTimeMillis: Long = -1
  private[kinesis] val CheckpointIntervalInMillis = 1000L

  override def initialize(shardId: String): Unit = {
    this.shardId = shardId
    info(s"Record Processor created for shard $shardId.")
  }

  override def shutdown(iRecordProcessorCheckpointer: IRecordProcessorCheckpointer, shutdownReason: ShutdownReason): Unit = {
    if (shutdownReason == ShutdownReason.TERMINATE) {
      checkpoint(iRecordProcessorCheckpointer)
    }
    info(s"Record Processor stopped for shard $shardId.")
  }

  override def processRecords(records: java.util.List[Record], iRecordProcessorCheckpointer: IRecordProcessorCheckpointer): Unit = {
    val events = records.asScala flatMap convertRecord
    if (events.size == records.size()) {

      events foreach sinkEventProcessor.processEvent

      if (System.currentTimeMillis() > nextCheckpointTimeMillis) {
        checkpoint(iRecordProcessorCheckpointer)
        nextCheckpointTimeMillis = System.currentTimeMillis() + CheckpointIntervalInMillis
      }
    } else {
      warn("Failed to parse records into SinkEvent objects.")
    }
  }


  private[this] def checkpoint(checkpointer: IRecordProcessorCheckpointer): Unit = {
    Retry.retryWithExponentialDelay(maxRetryTimes = 10, initialDelay = 1.second) {
      checkpointer.checkpoint()
    }(_ => ())
  }

  private[this] def convertRecord(record: Record): Option[SinkEvent] =
    try {
      Json.parse(new String(record.getData.array())).asOpt[SinkEvent]
    } catch {
      case e: JsonParseException =>
        None
    }
}
