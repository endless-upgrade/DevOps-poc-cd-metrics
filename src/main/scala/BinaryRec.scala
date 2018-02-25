import it.reply.data.devops.BinaryALSValidator
import it.reply.data.devops.{BinaryALS, BinaryALSValidator}
import it.reply.data.pasquali.Storage
import org.apache.spark.mllib.recommendation.Rating
import org.apache.spark.sql.Row

object BinaryRec {

  def main(args: Array[String]): Unit = {

    var addr = ""
    var port = ""

    if(args.length != 2){
      addr = "clouder"
      port = "7051"
    }
    else{
      addr = args(0)
      port = args(1)
    }

    val storage = Storage()
      .init("local", "binaryRec", false)
      .initKudu(addr, port, "impala::")

    var ratings = storage.readKuduTable("default.kudu_ratings").rdd
      .map{ case Row(userID, movieID, rating, time) =>
      Rating(userID.asInstanceOf[Long].toInt,
        movieID.asInstanceOf[Long].toInt,
        rating.asInstanceOf[Double])}

    val Array(train, test) = ratings.randomSplit(Array(0.8, 0.2))

    val mr = BinaryALS().initSpark("test", "local")
        .trainModelBinary(train, 10, 10, 0.1)

    val validator = BinaryALSValidator(mr.model).init(test)

    println(s"MSE = ${validator.MSE}")
    println(s"RMSE = ${validator.RMSE}")
    println(s"accuracy = ${(validator.accuracy*100).toInt}%")
    println(s"precision = ${(validator.precision*100).toInt}%")
    println(s"recall = ${(validator.recall*100).toInt}%")
  }
}
