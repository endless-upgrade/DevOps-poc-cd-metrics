
package it.reply.data.devops

import it.reply.data.pasquali.Storage
import org.apache.spark.sql.functions.udf

case class KuduStorage(storage : Storage) {

  val UDFtoLong = udf { u: String => u.toLong }
  val UDFtoDouble = udf { u: String => u.toDouble }


  def storeRatingsToKudu(filename : String) : Unit = {

    val spark = storage.spark

    import spark.implicits._

    val table = spark.read
      .format("csv")
      .option("header", "true")
      .option("mode", "DROPMALFORMED")
      .load(filename)
      .withColumn("u", UDFtoLong('userid))
      .withColumn("m", UDFtoLong('movieid))
      .withColumn("r", UDFtoDouble('rating))
      .drop("userid")
      .drop("movieid")
      .drop("rating")
      .select('u, 'm, 'r, 'time)
      .toDF("userid", "movieid", "rating", "time")

    table.printSchema()

    storage.upsertKuduRows(table, "default.kudu_ratings")
  }
}
