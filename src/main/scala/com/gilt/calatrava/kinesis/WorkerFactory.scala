package com.gilt.calatrava.kinesis

import com.amazonaws.services.kinesis.clientlibrary.lib.worker.Worker

trait WorkerFactory {
  def instance(): Worker
}
