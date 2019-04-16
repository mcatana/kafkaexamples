package com.examples.wordcount

import java.time.Duration
import java.util.Properties

import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.common.serialization.Serdes
import org.apache.kafka.streams.{KafkaStreams, StreamsConfig}
import org.apache.kafka.streams.scala.StreamsBuilder
import org.apache.kafka.streams.scala.kstream._

object Wordcount {

  def main(args: Array[String]): Unit = {
    import org.apache.kafka.streams.scala.Serdes._
    import org.apache.kafka.streams.scala.ImplicitConversions._
    import java.util.concurrent.CountDownLatch

    System.out.println("Hello World!")
    val config: Properties = {
      val p = new Properties()
      p.put(StreamsConfig.APPLICATION_ID_CONFIG, "wordcount-scala-app1")
      val bootstrapServers = "localhost:9092"
      p.put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers)
      p.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest")
      //p.put(StreamsConfig.CACHE_MAX_BYTES_BUFFERING_CONFIG, 0)
      p.put(StreamsConfig.DEFAULT_KEY_SERDE_CLASS_CONFIG, Serdes.String.getClass.getName)
      p.put(StreamsConfig.DEFAULT_VALUE_SERDE_CLASS_CONFIG, Serdes.String.getClass.getName)
      p
    }
    System.out.println(config)

    val builder = new StreamsBuilder()
    val textLines: KStream[String, String] = builder.stream[String, String]("words_in")
    val wordcounts: KTable[String, Long] = textLines
      .flatMapValues(textLine => textLine.toLowerCase().split("\\W+"))
      .groupBy((_,word) => word)
      .count()

    wordcounts.toStream.to("streams-scala-output")

    val streams: KafkaStreams = new KafkaStreams(builder.build(), config)
    val latch = new CountDownLatch(1)

    Runtime.getRuntime.addShutdownHook(new Thread("scala-wordcount-shutdown-hook") {
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
