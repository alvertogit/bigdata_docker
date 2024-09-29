"""
example.py: pyspark application example.

Local execution command example:

~/usr/spark-3.5.3/work_dir/python_apps/example$ spark-submit \
--master local[2] \
--driver-memory 10g \
example.py \
1 49999 \
/tmp/data/test_log1.csv \
/tmp/data/test_log2.csv \
/tmp/data/result_local_log

Worker execution command example:

~/usr/spark-3.5.3/work_dir/python_apps/example$ spark-submit \
--master spark://master:7077 \
--driver-memory 10g \
example.py \
1 49999 \
/tmp/data/test_log1.csv \
/tmp/data/test_log2.csv \
/tmp/data/result_worker_log
"""

__author__ = "alvertogit"
__copyright__ = "Copyright 2018-2024"


import os
import sys

from pyspark.sql import Row, SparkSession, Window
from pyspark.sql.functions import countDistinct, desc, row_number
from pyspark.sql.types import BooleanType, IntegerType, StructField, StructType

if __name__ == "__main__":
    if len(sys.argv) < 5:
        print("""
            Usage error: example.py <min-range-Id> <max-range-Id> <path-input-log1>
              <path-input-log2> <path-output-log>
            """)
        sys.exit(1)

    minRangeId = int(sys.argv[1])
    maxRangeId = int(sys.argv[2])
    path_input_log1 = sys.argv[3]
    path_input_log2 = sys.argv[4]
    path_output_log = sys.argv[5]

    def rangeId(user_id):
        if user_id >= minRangeId and user_id <= maxRangeId:
            return True
        return False

    os.environ["PYSPARK_PYTHON"] = sys.executable
    os.environ["PYSPARK_DRIVER_PYTHON"] = sys.executable
    spark = SparkSession.builder.appName("pyspark example").getOrCreate()
    # spark.conf.set("spark.driver.memory", "10g")
    # spark.sparkContext.setLogLevel("WARN")
    sc = spark.sparkContext

    # test_log1.csv input data format example
    #
    # userId;region;type;paid
    # 27103;0;0;true
    # 74637;2;1;false
    # ...

    infoSchema = StructType(
        [
            StructField("userId", IntegerType(), True),
            StructField("region", IntegerType(), True),
            StructField("type", IntegerType(), True),
            StructField("paid", BooleanType(), True),
        ]
    )

    infoDF = (
        spark.read.schema(infoSchema)
        .option("header", "true")
        .option("delimiter", ";")
        .csv(path_input_log1)
    )

    # test_log2.csv input data format example
    #
    # hour;userId;songId;genderId;deviceId
    # 18-10-2017 00:00:25;27103;231990117;23;1_27103
    # 18-10-2017 00:02:00;74637;241781021;24;1_74637
    # ...

    songSchema = StructType(
        [
            StructField("userId", IntegerType(), True),
            StructField("hour", IntegerType(), True),
            StructField("songId", IntegerType(), True),
            StructField("genderId", IntegerType(), True),
            StructField("deviceId", IntegerType(), True),
        ]
    )

    songRDD = sc.textFile(path_input_log2)

    songHeader = songRDD.first()

    songRDDFiltered = (
        songRDD.filter(lambda record: record != songHeader)
        .map(lambda line: line.split(";"))
        .filter(lambda field: rangeId(int(field[1])))
        .map(
            lambda rec: Row(
                int(rec[1]),
                int(rec[0].split(" ")[1].split(":")[0]),
                int(rec[2]),
                int(rec[3]),
                int(rec[4].split("_")[0]),
            )
        )
    )

    songDF = spark.createDataFrame(songRDDFiltered, songSchema)

    distSongDF = (
        songDF.groupBy("userId")
        .agg(countDistinct("songId"))
        .withColumnRenamed("count(DISTINCT songId)", "distSongIds")
    )

    genderSongDF = (
        songDF.groupBy("userId", "genderId").count().withColumnRenamed("count", "topGenderIdSongs")
    )

    genderWindow = Window.partitionBy("userId").orderBy(desc("topGenderIdSongs"))

    windowGenderSongDF = (
        genderSongDF.withColumn("rank", row_number().over(genderWindow))
        .where("rank = 1")
        .drop("rank")
        .withColumnRenamed("genderId", "topGenderId")
    )

    globalSongDF = windowGenderSongDF.join(distSongDF, "userId")

    # globalSongDF format example
    #
    # userId;topGenderId;topGenderIdSongs;distSongIds
    # 27103;23;1;2
    # 4052;27;1;2
    # ...

    # joining global song and info DFs and ordering output
    resultDF = globalSongDF.join(infoDF, "userId")

    # resultDF format example
    #
    # userId;topGenderId;topGenderIdSongs;distSongIds;region;type;paid
    # 27103;23;1;2;0;0;true
    # 4052;27;1;2;1;2;false
    # ...

    resultDF.coalesce(1).write.option("header", "true").option("delimiter", ";").csv(
        path_output_log
    )

    spark.stop()
