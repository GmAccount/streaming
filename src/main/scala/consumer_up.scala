import org.apache.kafka.clients.consumer.{ConsumerRecord, ConsumerRecords, KafkaConsumer}
import org.apache.log4j.{Level, Logger}
import java.lang
import java.util.Properties

import org.apache.spark._
import org.apache.spark.SparkContext._
import org.apache.spark.sql.{Row, SaveMode, SparkSession}
import org.apache.spark.sql.SQLContext
import org.apache.spark.streaming._
import org.apache.spark.streaming.twitter._
import org.apache.spark.streaming.StreamingContext._

import org.apache.log4j.{Level, Logger}
import java.text.SimpleDateFormat

object consumer_up {
  def main(args: Array[String]): Unit = {
    Logger.getLogger("org").setLevel(Level.OFF)
    val props = new Properties()
    props.put("bootstrap.servers", "localhost:9092")
    props.put("group.id", "test")
    props.put("key.deserializer","org.apache.kafka.common.serialization.StringDeserializer")
    props.put("value.deserializer","org.apache.kafka.common.serialization.StringDeserializer")
    props.put("enable.auto.commit", "true")
    props.put("auto.commit.interval.ms", "1000")
    import scala.collection.JavaConversions._
    val kafkaConsumerUp = new KafkaConsumer[String,String](props)
    val kafkaConsumerDown = new KafkaConsumer[String,String](props)

    kafkaConsumerUp.subscribe(java.util.Arrays.asList("Up"))
    kafkaConsumerDown.subscribe(java.util.Arrays.asList("Down"))

    val spark = SparkSession
      .builder()
      .config("spark.master", "local[2]")
      .appName("interfacing spark sql to hive metastore through thrift url below")
      .enableHiveSupport() // to enable hive support
      .getOrCreate()
    import spark.implicits._
    spark.sql("CREATE TABLE IF NOT EXISTS tweets (datetime STRING, text STRING) STORED AS PARQUET")

    val sc=spark.sparkContext

    val ssc = new StreamingContext(sc, Seconds(1))

    while(true) {
      val record: ConsumerRecords[String, String] = kafkaConsumerUp.poll(200)
      for (r <- record.iterator()) {
        println("+++++++++++")
        println(s"Here's your $r")

        println(r.value())
      }
    }
    /*
    while(true) {
      val record: ConsumerRecords[String, String] = kafkaConsumerDown.poll(200)
      for (r <- record.iterator()) {
        println("------")
        println(r)
      }
    }
    */
  }
}