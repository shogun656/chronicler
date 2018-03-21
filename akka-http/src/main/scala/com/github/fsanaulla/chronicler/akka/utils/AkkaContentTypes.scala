package com.github.fsanaulla.chronicler.akka.utils

import _root_.akka.http.scaladsl.model.{MediaType, MediaTypes}

/**
  * Created by fayaz on 12.07.17.
  */
private[fsanaulla] object AkkaContentTypes {

  final val AppJson: MediaType.WithFixedCharset = MediaTypes.`application/json`

  final val OctetStream: MediaType.Binary = MediaTypes.`application/octet-stream`
}