package it.reply.data.devops

import org.apache.spark.mllib.recommendation.{MatrixFactorizationModel, Rating}
import org.apache.spark.rdd.RDD

case class BinaryALSValidator(model : MatrixFactorizationModel) {

  var ratesAndPreds : RDD[((Int, Int), (Double, Double))] = null

  var tp : Double = 0
  var tn : Double = 0
  var fp : Double = 0
  var fn : Double = 0


  def init(testSet : RDD[Rating]) : BinaryALSValidator = {

    val usersProducts = testSet.map { case Rating(user, product, rate) =>
      (user, product)
    }

    val predictions = model.predict(usersProducts).map { case Rating(user, product, rate) =>
        ((user, product), if(rate >= 0.5) 1.0 else 0.0)
      }

    ratesAndPreds = testSet.map { case Rating(user, product, rate) =>
      ((user, product), if(rate >= 2.5) 1.0 else 0.0)
    }.join(predictions)

    tp = ratesAndPreds.map { case ((user, product), (rate, pred)) =>
      if(rate == 1.0 && pred == 1.0) 1 else 0
    }.sum()

    tn = ratesAndPreds.map { case ((user, product), (rate, pred)) =>
      if(rate == 0.0 && pred == 0.0) 1 else 0
    }.sum()

    fp = ratesAndPreds.map { case ((user, product), (rate, pred)) =>
      if(rate == 0.0 && pred == 1.0) 1 else 0
    }.sum()

    fn = ratesAndPreds.map { case ((user, product), (rate, pred)) =>
      if(rate == 1.0 && pred == 0.0) 1 else 0
    }.sum()

    this
  }

  def MSE() : Double = {

    val MSE = ratesAndPreds.map { case ((user, product), (r1, r2)) =>
      val err = r1 - r2
      err * err
    }.mean()

    MSE
  }

  def RMSE() : Double = {
    math.sqrt(MSE())
  }

  def accuracy() : Double = {

    val accuracy = ratesAndPreds.map { case ((user, product), (r1, r2)) =>
      if(r1 == r2) 1 else 0
    }.mean()

    accuracy
  }

  def precision() : Double = {
    tp / (tp + fp)
  }

  def recall() : Double = {
    tp / (tp + fn)
  }

}
