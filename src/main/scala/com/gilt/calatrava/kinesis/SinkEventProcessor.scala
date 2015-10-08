package com.gilt.calatrava.kinesis

import com.gilt.calatrava.v0.models.SinkEvent

private[kinesis] trait SinkEventProcessor {
  def processEvent(event: SinkEvent): Boolean
}
