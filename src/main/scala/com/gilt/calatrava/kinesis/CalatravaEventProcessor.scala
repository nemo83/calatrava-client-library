package com.gilt.calatrava.kinesis

import com.gilt.calatrava.v0.models.ChangeEvent

trait CalatravaEventProcessor {
  def processEvent(event: ChangeEvent): Unit
}
