
import it.reply.data.devops.BinaryALSValidator
import it.reply.data.devops.{BinaryALS, BinaryALSValidator}
import it.reply.data.pasquali.Storage
import org.apache.spark.mllib.recommendation.Rating
import org.apache.spark.rdd.RDD
import org.apache.spark.sql.{DataFrame, SparkSession}
import org.scalatest._

import scala.reflect.io.Path
import scala.util.Try

class DevOpsPOCSpec extends FlatSpec
  with Matchers
  with OptionValues
  with Inside
  with Inspectors
  with BeforeAndAfterAll{

  var spark : SparkSession = null
  var movieRec : BinaryALS = null
  var ratings : DataFrame = null
  var ratingsRDD : RDD[Rating] = null
  var train : RDD[Rating] = null
  var test : RDD[Rating] = null

  override def beforeAll(): Unit = {
    super.beforeAll()

    movieRec = BinaryALS().initSpark("DevOpsPOC", "local[*]")
    spark = movieRec.spark

    ratings = spark.read
      .format("csv")
      .option("header", "true")
      .option("mode", "DROPMALFORMED")
      .load("data/ratings.csv")
  }

  "Spark Session" must "be initialized" in {
    assert(spark != null)
  }

  "Ratings Dataframe" must "be loaded from csv local file" in {
    assert(ratings != null)
    ratings.printSchema()
  }
  it must "contains at least 10K elements" in {
    assert(ratings.count() >= 10000)
  }
  it must "be transformed, removing timestamp column" in {
    ratings = ratings
      .drop("time")

    assert(!ratings.columns.contains("time"))
  }
  it must "be transformed in a RDD of Spark MLlib Ratings" in {
    ratingsRDD = ratings.rdd.map { rate =>
      Rating(rate(0).toString.toInt, rate(1).toString.toInt, rate(2).toString.toDouble)
    }.cache()

    assert(ratingsRDD != null)
    assert(ratingsRDD.count() >= 10000)
  }

  "Rating RDD" must "be split in train and test set" in {
    val Array(tr, te) = ratingsRDD.randomSplit(Array(0.8, 0.2))

    assert(tr != null)
    assert(te != null)
    assert(tr.count() >= 10000*0.8)
    assert(te.count() >= 10000*0.2)

    train = tr
    test = te
  }

  "Binary ALS recommender" must "be initialized" in {
    assert(movieRec != null)
  }
  it must "train a recommendation model" in {

    movieRec.trainModelBinary(train, 10, 10, 0.1)
    assert(movieRec.model != null)

  }

  "The model" must "have a good accuracy" in {

    val validator = BinaryALSValidator(movieRec.model).init(test)

    val acc = validator.accuracy()

    println(s"Accuracy is ${acc}")
    assert(acc >= 0.7)
  }

  it must "have a good precision" in {
    val validator = BinaryALSValidator(movieRec.model).init(test)

    val prec = validator.precision()

    println(s"Precision is ${prec}")
    assert(prec >= 0.8)
  }



  it must "have a good recall" in {

    val validator = BinaryALSValidator(movieRec.model).init(test)

    val rec = validator.recall()

    println(s"Recall is ${rec}")
    assert(rec >= 0.8)

  }

  it must "be stored as is" in {

    movieRec.storeModel("model/poc")

    assert(Path("model/poc").exists)
    Try(Path("model/poc").deleteRecursively())

  }

  it must "be stored as zip" in {

    movieRec.storeModel("model/poc")
    Storage().zipModel("model/poc", "model/poc.zip")
    Try(Path("model/poc").deleteRecursively())

    assert(Path("model/poc.zip").exists)

    Try(Path("model/poc.zip").delete())
  }





  override def afterAll(): Unit = {
    super.afterAll()

    if(spark != null)
      spark.close()
  }

}

