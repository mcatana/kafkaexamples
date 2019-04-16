package com.examples.movielens

import java.util.{Collections, Properties}
import java.util.concurrent.CountDownLatch

import io.confluent.kafka.serializers.AbstractKafkaAvroSerDeConfig
import io.confluent.kafka.streams.serdes.avro.GenericAvroSerde
import org.apache.avro.Schema
import org.apache.avro.generic.GenericRecord
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.common.serialization.{Serde, Serdes}
import org.apache.kafka.streams.errors.LogAndContinueExceptionHandler
import org.apache.kafka.streams.kstream.KeyValueMapper
import org.apache.kafka.streams.scala.ImplicitConversions._
import org.apache.kafka.streams.scala.Serdes._
import org.apache.kafka.streams.scala.StreamsBuilder
import org.apache.kafka.streams.scala.kstream.{KStream, KTable}
import org.apache.kafka.streams.{KafkaStreams, StreamsConfig}


object MovieFilterAvro {

  def main(args: Array[String]): Unit = {

    val config: Properties = {
      val p = new Properties()
      p.put(StreamsConfig.APPLICATION_ID_CONFIG, "moviefilter-avro-app")
      p.put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092")
      p.put(AbstractKafkaAvroSerDeConfig.SCHEMA_REGISTRY_URL_CONFIG, "http://localhost:8081")
      p.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "latest")
      //p.put(StreamsConfig.CACHE_MAX_BYTES_BUFFERING_CONFIG, 0)
      p.put(StreamsConfig.DEFAULT_KEY_SERDE_CLASS_CONFIG, Serdes.String.getClass.getName)
      p.put(StreamsConfig.DEFAULT_VALUE_SERDE_CLASS_CONFIG, classOf[GenericAvroSerde])
      //p.put(StreamsConfig.DEFAULT_DESERIALIZATION_EXCEPTION_HANDLER_CLASS_CONFIG, classOf[LogAndContinueExceptionHandler]);
      p
    }

    implicit val genericAvroSerde: Serde[GenericRecord] = {
      val gas = new GenericAvroSerde
      val isKeySerde: Boolean = false
      gas.configure(Collections.singletonMap(AbstractKafkaAvroSerDeConfig.SCHEMA_REGISTRY_URL_CONFIG, "http://localhost:8081"), isKeySerde)
      gas
    }
    //override serdes for values
    val serdeConfig = Collections.singletonMap("schema.registry.url", "http://localhost:8081")
    val valueGenericAvroSerde = new GenericAvroSerde()
    valueGenericAvroSerde.configure(serdeConfig, false)

//    val moviesSchema = MovieFilter.getClass.getClassLoader.getResourceAsStream("movies.avsc")
//    val schema = new Schema.Parser().parse(moviesSchema)

    val streamsBuilder = new StreamsBuilder()
    //create a stream from the movies3_key topic
    val movieStream: KStream[String, GenericRecord] = streamsBuilder.stream[String, GenericRecord]("movies_avro3")

    //map key and year
    //filter movies with year > 2050
    val futureMovies: KStream[String, String] =  movieStream
        .map((key, t) =>(key, t.get("YEAR").toString))
        .filter((_,year) => year.toInt>2050)

    futureMovies.foreach((key, value) => System.out.println(key + " => " + value))

    val countTable: KTable[String,Long] = futureMovies.groupBy((movieid, year) => year).count()

    countTable.toStream.to("movieavro-count-output-year")


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
