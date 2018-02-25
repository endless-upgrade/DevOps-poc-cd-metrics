package it.reply.data.devops

import org.apache.spark.SparkContext
import org.apache.spark.mllib.recommendation.{ALS, MatrixFactorizationModel, Rating}
import org.apache.spark.rdd.RDD
import org.apache.spark.sql.SparkSession

import scala.reflect.io.File
import scala.sys.process._

case class BinaryALS() {

  var spark : SparkSession = null
  var sc : SparkContext = null

  var model : MatrixFactorizationModel = null

  def initSpark(spark : SparkSession) : BinaryALS = {
    this.spark = spark
    sc = spark.sparkContext
    this
  }

  def initSpark(appName : String, master : String) : BinaryALS = {

    spark = SparkSession.builder()
      .appName(appName)
      .master(master)
      .getOrCreate()
    sc = spark.sparkContext
    sc.setCheckpointDir("checkpoint")
    this
  }

  def trainModelBinary(ratings : RDD[Rating], rank : Int, numIter : Int, regParam : Double) : BinaryALS = {
    val approx = ratings.map(r => Rating(r.user, r.product, if(r.rating >= 2.5) 1.0 else 0.0))
    model = ALS.train(approx, rank, numIter, regParam)
    this
  }

  def predict(ratings : RDD[(Int, Int)]) : RDD[((Int, Int), Double)] = {

    model.predict(ratings).map { case Rating(user, product, rate) =>
      ((user, product), rate)
    }
  }

  def storeModel(dirPath : String) : Unit = {

    if(File(dirPath).exists)
      s"rm -rd ${dirPath}" !

    model.save(sc, dirPath)
  }

  def loadModel(dirPath : String) : BinaryALS = {

    model = MatrixFactorizationModel.load(sc, dirPath)
    this
  }

  def closeSession() : Unit = {

    if(spark != null)
      spark.stop()

    spark = null
    sc = null
  }



}
