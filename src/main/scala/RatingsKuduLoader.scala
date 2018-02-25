import it.reply.data.devops.KuduStorage
import it.reply.data.pasquali.Storage

object RatingsKuduLoader {

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
      .init("local", "kudu", false)
      .initKudu(addr, port, "impala::")

    KuduStorage(storage).storeRatingsToKudu("data/ratings.csv")
  }

}
