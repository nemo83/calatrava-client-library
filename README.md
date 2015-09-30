# calatrava-client-library


A library that allows consumption of events from a Calatrava stream.

## Contents and Example Usage

### Interfaces

There are two interfaces that must be implemented. One allows you to configure the Calatrava Bridge Worker, and the other is the event processor that consumes the events crossing the Bridge.

#### kinesis.BridgeConfiguration

An implementation of this interface must be constructed to configure the Calatrava Bridge that we need to consume events for. The following parameters must be provided:

* _clientAppName_: mandatory String parameter to specify the name of this Kinesis client application.

* _streamName_: mandatory String parameter, specifies the name of the Kinesis stream to read events from.

* _bucketName_: mandatory String parameter, specifies the name of the S3 bucket where events are to be read from, when the size exceeds the Kinesis maximum event size.

* _iamRoleArnOpt_: optional String parameter, specifies an IAM role that this application must assume prior to making calls to Kinesis, or S3. If not specified, the application will use the default credentials provider chain.
 
* _metricsConfigOpt_: optional `MetricsConfiguration` parameter, specifies CloudWatch metrics configuration parameters. If not provided, the Kinesis Client library will not send metrics to CloudWatch.

#### kinesis.CalatravaEventProcessor

An implementation of this interface must be constructed and passed to the Worker factory, so that events are being sent here for processing. Has only one method, `processEvent`, which takes a parameter of type `ChangeEvent`, as defined in the [Calatrava API](https://github.com/gilt/calatrava/blob/master/api/api.json).

### Worker Factory

A singleton instance of this class should be created, and then used to produce KCL workers as needed.

Example:

```
val factory = new BridgeWorkerFactory(eventProcessor, bridgeConfiguration)
val worker = factory.instance()
val workerThread = new Thread(worker)

def start(): Unit = {
  workerThread.start()
}

def shutdown(): Unit = {
  worker.shutdown()
  workerThread.join(WORKER_THREAD_TIMEOUT_MILLIS)
}
```

The BridgeWorkerFactory can be instantiated with Guice, and have its configuration and processor injected automatically.

## License
Copyright 2015 Gilt Groupe, Inc.

Licensed under the Apache License, Version 2.0: http://www.apache.org/licenses/LICENSE-2.0
