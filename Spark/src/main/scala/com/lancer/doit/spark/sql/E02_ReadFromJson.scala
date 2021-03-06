package com.lancer.doit.spark.sql

import org.apache.spark.sql.SparkSession
import org.apache.spark.sql.types.{DataTypes, StructField, StructType}
import utils.SparkEnvUtils

object E02_ReadFromJson {
  def main(args: Array[String]): Unit = {
    val spark = SparkEnvUtils.getEnv("session", "readFromJson", "local", "error").asInstanceOf[SparkSession]

    val df = spark.read.json("Spark-core/data/json/input/a.txt") // 解析json数据，将解析失败的数据放入名为_corrupt_record字段中，不用指定第一行为头，将解析出来的k作为字段名
    // val df = spark.sql("select * from json.`Spark-core/data/json/input/a.txt`")
    import spark.implicits._

    /**
     * 不能直接引用_corrupt_record字段，所以不能使用SQL去过滤脏数据，调用api去过滤
     * 过滤出解析成功的字段, 只有存在错误的数据, 才有_corrupt_record字段
     * 可以使用try catch，来判断是否有异常
     */
    // df.filter(column("_corrupt_record").isNull) // 直接传入column方法

    var df2 = df // 如果没有错误数据，直接返回原数据
    try {
      df2 = df.filter($"_corrupt_record".isNull) // $表示把字段作为Column对象传入，新版本中，只能引用数据中的解析出来的字段，不能引用_corrupt_record字段
    } catch {
      case _: Exception => println("没有错误数据，没有_corrupt_record列")
    }

    // df.filter('_corrupt_record.isNull) // '表示把字段作为Column对象传入

    /**
     * 复杂的json结构，{"name": "zhangsan", "age": 18, "family": [{"name": "aa", "relationship":"mother", "hobby": [{"playBasketball": true, "playFootball": false}]}, {"name": "bb", "relationship": ""}]}
     * 会按照最复杂的那条json来确定结构，也可以手动指定schema，嵌套的{}结构默认都是struct类型，会出现大量空值(老版本为空串，新版本中改成null);[]结构都是array类型
     *
     * root
     * |-- _corrupt_record: string (nullable = true) -- 行中包含错误数据
     * |-- age: long (nullable = true)
     * |-- family: array (nullable = true)
     * |    |-- element: struct (containsNull = true)
     * |    |    |-- age: long (nullable = true)
     * |    |    |-- hobby: array (nullable = true)
     * |    |    |    |-- element: struct (containsNull = true)
     * |    |    |    |    |-- playBasketball: boolean (nullable = true)
     * |    |    |    |    |-- playFootball: boolean (nullable = true)
     * |    |    |-- name: string (nullable = true)
     * |    |    |-- relationship: string (nullable = true)
     * |-- name: string (nullable = true)
     *
     * SQL中(array+struct)复杂数据获取：family[0].hobby[0].playBasketball --> 母亲是否打篮球
     * (array+Map) : family[0].hobby['playBasketball']
     */

    df2.show(100, truncate = false)
    df2.printSchema()


    /**
     * 手动指定嵌套类型，避免大量空值的产生
     */
    val schemas = StructType(Seq(
      StructField("name", DataTypes.StringType),
      StructField("info", DataTypes.createMapType(DataTypes.StringType, DataTypes.BooleanType)) // map中的某一个数据类型不匹配(String, Boolean)类型，则该map就为空值，获取map中的值时，获取不到就为null
    ))
    val df3 = spark.read.schema(schemas).json("Spark-core/data/json/input/b.txt")
    df3.show(100, truncate = false)
    df3.printSchema()

    spark.close()
  }
}
