
import org.apache.spark._
import org.apache.spark.SparkContext._
import org.apache.spark.sql.{Row, SaveMode, SparkSession}
import org.apache.spark.streaming._
import org.apache.spark.streaming.twitter._
import org.apache.spark.streaming.StreamingContext._

import org.apache.log4j.{Level, Logger}
import java.text.SimpleDateFormat
import java.util.{Calendar, Date}

import org.apache.kafka.clients.producer.{KafkaProducer, ProducerRecord,Callback, RecordMetadata}
import java.util.Properties

object twiter_receiver {

  def main(args: Array[String]): Unit = {
    Logger.getLogger("org").setLevel(Level.ERROR)

    val props = new Properties()
    props.put("bootstrap.servers", "localhost:9092")
    props.put("key.serializer", "org.apache.kafka.common.serialization.StringSerializer")
    props.put("value.serializer", "org.apache.kafka.common.serialization.StringSerializer")
    val kafkaProducer: KafkaProducer[String, String] = new KafkaProducer[String, String](props)

    var onetime=true
    import java.text.SimpleDateFormat
    val sdf = new SimpleDateFormat("YYYY-MM-DD hh:mm:ss")

    val sparkConf = new SparkConf().setAppName("getTweets").setMaster("local[3]")


    val spark = SparkSession
      .builder()
      .config("spark.master", "local[2]")
      .appName("adl-test")
      .getOrCreate()

    import spark.implicits._

    spark.sql("CREATE TABLE IF NOT EXISTS digital_currency (datetime STRING, text STRING) USING CSV")
    val sc=spark.sparkContext

    val ssc = new StreamingContext(sc, Seconds(1))

    // specify twitter consumerKey, consumerSecret, accessToken, accessTokenSecret


    System.setProperty("twitter4j.oauth.consumerKey","[consumerKey]")
    System.setProperty("twitter4j.oauth.consumerSecret", "[consumerSecret]")
    System.setProperty("twitter4j.oauth.accessToken", "[accessToken]")
    System.setProperty("twitter4j.oauth.accessTokenSecret", "[accessTokenSecret]")


    // Connect to Twitter and get the tweets object
    val tweets = TwitterUtils.createStream(ssc, None)
    // Extract the text from the tweets object
    val tweets_collection = tweets.map(each_tweet => each_tweet.getText())
    val focus_tweets_collection=tweets_collection.filter(text=>text.toLowerCase.contains("up") | text.toLowerCase.contains("down"))

    // streaming data from Twitter
    focus_tweets_collection.foreachRDD{ rdd =>
      if(!rdd.isEmpty) {
        val tweet_str = rdd.toString()
        var topic_name = "unknow"
        if(tweet_str.contains("up")){
          println("up case")
          topic_name = "Up"
        }else if(tweet_str.contains("down")){
          topic_name = "Down"
          println("down case")
        }else{
          println("unknow received teweet...")
        }

        val record = new ProducerRecord[String, String](topic_name,
          tweet_str)
        println("Sending record: " + record.value())

        kafkaProducer.send(record, new Callback {
          override def onCompletion(metadata: RecordMetadata, exception: Exception): Unit = {
            if(exception == null) {
              println("message sent successfully")
            } else {
              println(exception.getMessage)
            }
          }
        })

        println("record sent")
        kafkaProducer.close()
        println("closing producer")

      }
    }

    ssc.checkpoint("/tmp/digital_currency_dir")
    ssc.start()
    ssc.awaitTermination()
  }
}