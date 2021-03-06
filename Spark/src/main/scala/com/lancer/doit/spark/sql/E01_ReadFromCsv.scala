package com.lancer.doit.spark.sql

import org.apache.spark.sql.{DataFrame, SparkSession}
import org.apache.spark.sql.types.{DataTypes, StructField, StructType}
import utils.SparkEnvUtils

/**
 * 从csv文件读数据
 */
object E01_ReadFromCsv {
  def main(args: Array[String]): Unit = {
    val spark = SparkEnvUtils.getEnv("session", "readFromCsv", "local", "error").asInstanceOf[SparkSession]

    /**
     * 手动指定schema
     */
    // 使用apply方法创建schema
    val schemas = StructType(Seq(
      StructField("id", DataTypes.LongType),
      StructField("tall", DataTypes.IntegerType),
      StructField("weight", DataTypes.IntegerType),
      StructField("face", DataTypes.IntegerType),
      StructField("gender", DataTypes.StringType)
    ))

    // 调用api创建schema
    val schema = new StructType()
      .add("id", DataTypes.LongType)
      .add("tall", DataTypes.IntegerType)
      .add("weight", DataTypes.IntegerType)
      .add("face", DataTypes.IntegerType)
      .add("gender", DataTypes.StringType)

    // 使用构造方法创建schema
    val schema1 = new StructType((StructField("id", DataTypes.LongType) :: StructField("tall", DataTypes.IntegerType) :: Nil).toArray)
    val schema2 = new StructType(Array(StructField("id", DataTypes.LongType), StructField("tall", DataTypes.IntegerType)))
    val schema3 = StructType(StructField("id", DataTypes.LongType) :: StructField("tall", DataTypes.IntegerType) :: Nil)

    // DataFrame是Dataset[Row]的别名，其上还可以调用RDD算子
    val df: DataFrame = spark
      .read
      .option("header", "true") // 告诉它数据中包含头
      .option("inferSchema", "false") // 自动推断类型，不要使用，会额外触发join
      .option("seq", ",") // 指定分割字段
      .schema(schemas) // 手动指定Schema（字段名 + 字段类型），如果指定了头，那么按照手动 指定的schema来
      .csv("Spark-core/data/stu/input/a.txt")

    spark.readStream

    // 多行数据，列数太多，是否进行截断显示
    df.show(100, false)

    spark.sql(
      """
        |select * from csv.`Spark-core/data/stu/input/a.txt`
        |""".stripMargin).show(100, truncate = false)

    spark.close()
  }
}
