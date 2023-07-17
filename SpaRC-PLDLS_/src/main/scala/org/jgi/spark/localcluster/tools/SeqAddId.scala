/**
  * Created by Lizhen Shi on 6/8/17.
  */
package org.jgi.spark.localcluster.tools

import com.typesafe.scalalogging.LazyLogging
import org.apache.spark.{SparkConf, SparkContext}
import org.jgi.spark.localcluster.{DNASeq, Utils}
import sext._


object SeqAddId extends App with LazyLogging {

  case class Config(in: String = "", output: String = "",
                    n_partition: Int = -1, shuffle: Boolean = false)

  def parse_command_line(args: Array[String]): Option[Config] = {
    val parser = new scopt.OptionParser[Config]("Repartition") {
      head("GraphCC", Utils.VERSION)

      opt[String]('i', "in").required().valueName("<file>").action((x, c) =>
        c.copy(in = x)).text("input file")

      opt[String]('o', "output").required().valueName("<dir>").action((x, c) =>
        c.copy(output = x)).text("output file")

      opt[Int]('n', "n_partition").action((x, c) =>
        c.copy(n_partition = x))
        .text("paritions of output")

      help("help").text("prints this usage text")

    }
    parser.parse(args, Config())
  }

  def run(config: Config, sc: SparkContext): Unit = {

    val start = System.currentTimeMillis
    logger.info(new java.util.Date(start) + ": Program started ...")
    val rdd = if (config.n_partition<1) sc.textFile(config.in) else sc.textFile(config.in).coalesce(config.n_partition, shuffle = config.shuffle)
    println("Addseq:Noindex")
    rdd.take(10).foreach(println(_))

    rdd.zipWithIndex().map(u=>u._2.toInt.toString+"\t"+u._1).coalesce(1).saveAsTextFile(config.output)
    println("Addseq:index")
    rdd.take(10).foreach(println(_))
    logger.info(s"save results to ${config.output}")

    val totalTime1 = System.currentTimeMillis
    logger.info("Processing time: %.2f minutes".format((totalTime1 - start).toFloat / 60000))
  }

  override def main(args: Array[String]) {

    val options = parse_command_line(args)

    options match {
      case Some(_) =>
        val config = options.get

        logger.info(s"called with arguments\n${options.valueTreeString}")
        val conf = new SparkConf().setAppName("Spark Repartition")
        conf.registerKryoClasses(Array(classOf[DNASeq]))

        val sc = new SparkContext(conf)
        run(config, sc)

        sc.stop()
      case None =>
        println("bad arguments")
        sys.exit(-1)
    }
  } //main
}
