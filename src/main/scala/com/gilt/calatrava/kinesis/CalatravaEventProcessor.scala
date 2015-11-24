package com.gilt.calatrava.kinesis

import com.gilt.calatrava.v0.models.ChangeEvent

trait CalatravaEventProcessor {
  /**
   * Process an event and return true if successful, false otherwise
   *
   * @param event   the event to process
   * @return        true, if successfully processed; false, otherwise
   */
  def processEvent(event: ChangeEvent): Boolean

  /**
    * Notification that a heart beat event was received
    */
  def processHeartBeat(): Unit
}
