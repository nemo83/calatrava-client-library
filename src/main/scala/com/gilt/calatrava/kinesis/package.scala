package com.gilt.calatrava

import java.io.InputStream

import com.codahale.jerkson.Json
import com.fasterxml.jackson.datatype.joda.JodaModule
import com.gilt.calatrava.v0.models.{ChangeEvent, SinkEvent}

package object kinesis {

  trait JodaJson extends Json {
    mapper.registerModule(new JodaModule)
  }
  object JodaJson extends JodaJson

  implicit val readChangeEvent: InputStream => ChangeEvent = { case is: InputStream => JodaJson.parse[ChangeEvent](is) }
  implicit val readSinkEvent: String => SinkEvent = { case s: String => JodaJson.parse[SinkEvent](s) }
}
