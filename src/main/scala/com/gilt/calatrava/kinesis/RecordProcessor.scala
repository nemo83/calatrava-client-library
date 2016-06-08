package com.gilt.calatrava.kinesis

import com.amazonaws.services.kinesis.clientlibrary.interfaces.IRecordProcessorCheckpointer
import com.amazonaws.services.kinesis.clientlibrary.interfaces.v2.IRecordProcessor
import com.amazonaws.services.kinesis.clientlibrary.types.{InitializationInput, ProcessRecordsInput, ShutdownInput, ShutdownReason}
import com.amazonaws.services.kinesis.model.Record
import com.codahale.jerkson.ParsingException
import com.gilt.calatrava.v0.models.SinkEvent
import com.gilt.gfc.logging.Loggable
import com.gilt.gfc.util.Retry

import scala.collection.JavaConverters._
import scala.concurrent.duration._

private[kinesis] class RecordProcessor(sinkEventProcessor: SinkEventProcessor) extends IRecordProcessor with Loggable {

  private[this] var shardId: String = _
  private[this] var nextCheckpointTimeMillis: Long = -1
  private[kinesis] val CheckpointIntervalInMillis = 60000L

  override def initialize (initializationInput: InitializationInput): Unit = {
    shardId = initializationInput.getShardId
    info(s"Record Processor created for shard $shardId.")
  }

  override def shutdown(shutdownInput: ShutdownInput): Unit = {
    if (shutdownInput.getShutdownReason == ShutdownReason.TERMINATE) {
      checkpoint(shutdownInput.getCheckpointer)
    }
    info(s"Record Processor stopped for shard $shardId.")
  }

  override def processRecords(processRecordsInput: ProcessRecordsInput): Unit = {
    val events = processRecordsInput.getRecords.asScala flatMap convertRecord
    if (events.size == processRecordsInput.getRecords.size()) {

      try {
        val allGood = events forall sinkEventProcessor.processEvent

        if (allGood && System.currentTimeMillis() > nextCheckpointTimeMillis) {
          checkpoint(processRecordsInput.getCheckpointer)
          nextCheckpointTimeMillis = System.currentTimeMillis() + CheckpointIntervalInMillis
        }
      } catch {
        case e: Exception =>
          warn(s"Failed to process events $events", e)
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
      Some(readSinkEvent(new String(record.getData.array(), "UTF-8")))
    } catch {
      case e: ParsingException =>
        None
    }
}
