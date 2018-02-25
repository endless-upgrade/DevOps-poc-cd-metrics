import java.io.File

import com.typesafe.config._
import io.prometheus.client.{CollectorRegistry, Gauge}
import io.prometheus.client.exporter.PushGateway
import it.reply.data.devops.{BinaryALS, BinaryALSValidator}
import it.reply.data.pasquali.Storage
import org.apache.spark.mllib.recommendation.Rating
import org.apache.spark.sql.SparkSession

import scala.reflect.io.Path
import scala.util.Try

object OfflineBinRec {

  var mr : BinaryALS = null
  var spark : SparkSession = null

  def main(args: Array[String]): Unit = {

    var toggles : Config = null

    if(args.length != 0)
      toggles = ConfigFactory.parseFile(new File(args(0)))
    else
      toggles = ConfigFactory.load

    // ***************************************************************
    val GATEWAY_ADDR = toggles.getString("metrics.pushgateway.addr")
    val GATEWAY_PORT = toggles.getString("metrics.pushgateway.port")
    val pushGateway : PushGateway = new PushGateway(s"$GATEWAY_ADDR:$GATEWAY_PORT")
    val registry = new CollectorRegistry


    val gTrainDur = Gauge.build()
      .name("model_training_duration")
      .help("Binary ALS model training duration")
      .register(registry)

    val gLoadRatingsDur = Gauge.build()
      .name("model_load_data_duration")
      .help("Ratings CSV loading duration")
      .register(registry)

    mr = BinaryALS().initSpark("test", "local")

    spark = mr.spark

    val tl = gLoadRatingsDur.startTimer() // *******************
    val ratings = spark.read
      .format("csv")
      .option("header", "true")
      .option("mode", "DROPMALFORMED")
      .load("data/ratings.csv")
      .drop("time")
      .rdd.map { rate =>
      Rating(rate(0).toString.toInt, rate(1).toString.toInt, rate(2).toString.toDouble)
    }.cache()

    val Array(train, test) = ratings.randomSplit(Array(0.8, 0.2))
    tl.setDuration() // ***************************************

    val tt = gTrainDur.startTimer() // *******************
    mr.trainModelBinary(train, 10, 10, 0.1)
    tt.setDuration() // **********************************

    print("Model Trained")

    if(toggles.getBoolean("toggle.evaluate.evaluate")){

      val validator = BinaryALSValidator(mr.model).init(test)

      if(toggles.getBoolean("toggle.evaluate.accuracy")){
        println(s"accuracy = ${(validator.accuracy*100).toInt}%")

        Gauge.build()
          .name("model_accuracy")
          .help("Binary ALS model accuracy, single split 20-80")
          .register(registry)
          .set(validator.accuracy())
      }


      if(toggles.getBoolean("toggle.evaluate.precision")){
        println(s"precision = ${(validator.precision*100).toInt}%")

        Gauge.build()
          .name("model_precision")
          .help("Binary ALS model precision, single split 20-80")
          .register(registry)
          .set(validator.accuracy())
      }

      if(toggles.getBoolean("toggle.evaluate.recall")){
        println(s"recall = ${(validator.recall*100).toInt}%")

        Gauge.build()
          .name("model_recall")
          .help("Binary ALS model recall, single split 20-80")
          .register(registry)
          .set(validator.accuracy())
      }

    }


    if(toggles.getBoolean("toggle.store.store")){

      val path = toggles.getString("toggle.store.path")
      val zipIt = toggles.getBoolean("toggle.store.zip")

      storeModel(path, zipIt)

      Gauge.build()
        .name("model_stored")
        .help("1 if model stored else 0")
        .register(registry)
        .set(1)
    }
    else
      Gauge.build()
        .name("model_stored")
        .help("1 if model stored else 0")
        .register(registry)
        .set(0)

    pushGateway.push(registry, "dev_ops_poc")
  }

  def storeModel(path : String, zip : Boolean = false) : Unit = {

    mr.storeModel(path+"/poc")

    if(zip){
      val storage = Storage()
      storage.zipModel(path+"/poc", path+"/poc.zip")

      Try(Path(path+"/poc").deleteRecursively())
    }
  }

}
