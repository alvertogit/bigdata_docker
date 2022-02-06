/**

@author alvertogit
Copyright 2018-2022

Local execution command example:

~/usr/spark-3.2.0/work_dir/scala_apps/example$ spark-submit \
--master local[2] \
--driver-memory 10g \
--class stubs.Example \
target/example-1.0.jar \
1 49999 \
/tmp/data/test_log1.csv \
/tmp/data/test_log2.csv \
/tmp/data/result_local_log

Worker execution command example:

~/usr/spark-3.2.0/work_dir/scala_apps/example$ spark-submit \
--master spark://master:7077 \
--driver-memory 10g \
--class stubs.Example \
target/example-1.0.jar \
1 49999 \
/tmp/data/test_log1.csv \
/tmp/data/test_log2.csv \
/tmp/data/result_worker_log

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
	System.err.println("Usage: stubs.Example <min-range-Id> <max-range-Id> <path-input-log1> <path-input-log2> <path-output-log>")
	System.exit(1)
}
 
val minRangeId = args(0).toInt
val maxRangeId = args(1).toInt
val path_input_log1 = args(2)
val path_input_log2 = args(3)
val path_output_log = args(4)

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

// test_log1.csv input data format example
//
// userId;region;type;paid
// 27103;0;0;true
// 74637;2;1;false
// ...

val infoSchema = StructType(Array(
StructField("userId",IntegerType,true),
StructField("region",IntegerType,true),
StructField("type",IntegerType,true),
StructField("paid",BooleanType,true)
))

val infoDF = spark.read.schema(infoSchema).option("header","true").option("delimiter", ";").csv(path_input_log1)

// test_log2.csv input data format example
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

val songRDD = sc.textFile(path_input_log2)

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

val globalSongDF = windowGenderSongDF.join(distSongDF,"userId")

// globalSongDF format example
//              
// userId;topGenderId;topGenderIdSongs;distSongIds
// 27103;23;1;2
// 4052;27;1;2
// ...

// joining global song and info DFs and ordering output
val resultDF = globalSongDF.join(infoDF ,"userId")

// resultDF format example
//              
// userId;topGenderId;topGenderIdSongs;distSongIds;region;type;paid
// 27103;23;1;2;0;0;true
// 4052;27;1;2;1;2;false
// ...

resultDF.coalesce(1).write.option("header","true").option("delimiter", ";").csv(path_output_log)

spark.stop

}
}
