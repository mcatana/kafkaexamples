package com.examples.movielens

import java.io.InputStream
import java.time.Duration
import java.util.Properties
import java.util.concurrent.CountDownLatch

import io.confluent.kafka.serializers.AbstractKafkaAvroSerDeConfig
import io.confluent.kafka.streams.serdes.avro.GenericAvroSerde
import org.apache.avro.generic.GenericRecord
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.common.serialization.Serdes
import org.apache.kafka.streams.{KafkaStreams, StreamsConfig}
import org.apache.kafka.streams.scala.StreamsBuilder
import org.apache.kafka.streams.scala.kstream.{Consumed, KStream, KTable}
import org.apache.kafka.streams.scala.Serdes._
import org.apache.kafka.streams.scala.ImplicitConversions._
import io.confluent.kafka.streams.serdes.avro.GenericAvroSerde
import org.apache.avro.generic.GenericRecord
import org.apache.kafka.common.serialization.Serde
import java.util.Collections
import java.util

import org.apache.avro.Schema
import org.apache.kafka.streams.KeyValue
import org.apache.kafka.streams.errors.LogAndContinueExceptionHandler
import org.apache.kafka.streams.kstream.KeyValueMapper
import org.apache.kafka.streams.scala.kstream._


object MovieFilter {

  def main(args: Array[String]): Unit = {

    val config: Properties = {
      val p = new Properties()
      p.put(StreamsConfig.APPLICATION_ID_CONFIG, "moviefilter-app")
      p.put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092")
      p.put(AbstractKafkaAvroSerDeConfig.SCHEMA_REGISTRY_URL_CONFIG, "http://localhost:8081")
      p.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest")
      //p.put(StreamsConfig.CACHE_MAX_BYTES_BUFFERING_CONFIG, 0)
      p.put(StreamsConfig.DEFAULT_KEY_SERDE_CLASS_CONFIG, Serdes.String.getClass.getName)
      p.put(StreamsConfig.DEFAULT_VALUE_SERDE_CLASS_CONFIG, Serdes.String.getClass.getName)
      //p.put(StreamsConfig.DEFAULT_DESERIALIZATION_EXCEPTION_HANDLER_CLASS_CONFIG, classOf[LogAndContinueExceptionHandler]);
      p
    }

    val streamsBuilder = new StreamsBuilder()
    //create a stream from the movies3_key topic
    val movieStream: KStream[String, String] = streamsBuilder.stream[String, String]("movies3_key")

    movieStream.foreach((key, value) => System.out.println(key + " => " + value))

    val streams: KafkaStreams = new KafkaStreams(streamsBuilder.build(), config)
    val latch = new CountDownLatch(1)

    Runtime.getRuntime.addShutdownHook(new Thread("scala-movie-shutdown-hook") {
      override def run(): Unit = {
        streams.close
        latch.countDown
      }
    })

    try {
      streams.start
      latch.await
    } catch {
      case e: Throwable =>
        e.printStackTrace()
        System.exit(1)
    }

    System.exit(0)
  }
}
