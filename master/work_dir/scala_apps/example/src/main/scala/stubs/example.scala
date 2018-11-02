/**

Execution command example:

spark-submit \
--master local[2] \
--driver-memory 10g \
--class stubs.Example \
target/example-1.0.jar \
1 49999 \
/tmp/data/test_log.csv \
/tmp/data/result_log

*/

package stubs

import org.apache.spark.SparkContext
import org.apache.spark.sql.SparkSession
import org.apache.spark.sql.expressions.Window
import org.apache.spark.sql.types._
import org.apache.spark.sql.Row
import org.apache.spark.sql.functions._

object Example {
def main(args: Array[String]) {

if (args.length < 4) {
	System.err.println("Usage: stubs.Example <min-range-Id> <max-range-Id> <path-input-log> <path-output-log>")
	System.exit(1)
}
 
val minRangeId = args(0).toInt
val maxRangeId = args(1).toInt
val path_input_log = args(2)
val path_output_log = args(3)

def rangeId (user_id: Int) = {
  if (user_id >= minRangeId & user_id <= maxRangeId) {
    true
  }
  else{
    false
  }
}

val sc = new SparkContext()
val spark = SparkSession.builder.appName("ExampleApp").getOrCreate()
import spark.implicits._	

// test_log.csv input data format example
//
// hour;userId;songId;genderId;deviceId
// 18-10-2017 00:00:25;27103;231990117;23;1_27103
// 18-10-2017 00:02:00;74637;241781021;24;1_74637
// ...

val songSchema = StructType(Array(
StructField("userId",IntegerType,true),
StructField("hour",IntegerType,true),
StructField("songId",IntegerType,true),
StructField("genderId",IntegerType,true),
StructField("deviceId",IntegerType,true)
))

val songRDD = sc.textFile(path_input_log)

val songHeader = songRDD.first

val songRDDFiltered = songRDD.filter(record => record != songHeader).map(line => line.split(";")).filter(
field => rangeId(field(1).toInt
)).map(
rec => Row(
rec(1).toInt,
rec(0).split(' ')(1).split(':')(0).toInt,
rec(2).toInt,
rec(3).toInt,
rec(4).split('_')(0).toInt
))

val songDF = spark.createDataFrame(songRDDFiltered,songSchema)

val distSongDF = songDF.groupBy($"userId").agg(countDistinct($"songId")).withColumnRenamed("count(DISTINCT songId)","distSongIds")

val genderSongDF = songDF.groupBy($"userId",$"genderId").count.withColumnRenamed("count","topGenderIdSongs")

val genderWindow = Window.partitionBy("userId").orderBy($"topGenderIdSongs".desc)

val windowGenderSongDF = genderSongDF.withColumn("rank", row_number().over(genderWindow)).where($"rank" === 1).drop($"rank"
).withColumnRenamed("genderId","topGenderId")

val resultDF = windowGenderSongDF.join(distSongDF,"userId")

// resultDF format example
//              
// userId;topGenderId;topGenderIdSongs;distSongIds
// 27103;23;1;2
// 4052;27;1;2
// ...

resultDF.coalesce(1).write.option("header","true").option("delimiter", ";").csv(path_output_log)

spark.stop

}
}
