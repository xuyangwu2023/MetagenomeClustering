package org.jgi.spark.localcluster.tools

import com.typesafe.scalalogging.LazyLogging
import org.apache.log4j._
import org.apache.spark.graphx._
import org.apache.spark.rdd.RDD
import org.apache.spark.rdd.RDD.rddToPairRDDFunctions
import org.apache.spark._
import org.apache.spark.sql.SparkSession
import org.jgi.spark.localcluster.{DNASeq, Utils}
import org.jgi.spark.localcluster.tools.GraphLPA2.{Config, logInfo, parse_command_line, run}
import spire.compat.fractional

import scala.collection.{Map, mutable}
import scala.math.Fractional.Implicits.infixFractionalOps
import scala.reflect.ClassTag

object PLDLS extends App with LazyLogging with Serializable {
  case class Config(edge_file: String = "",output: String = "",iteration_number: Int = 5, pregel_iteration: Int = 5,
                    merge_flag: Int = 1, modularity_flag: Int = 1, write_to_disk: Int = 1, min_reads_per_cluster:Int=0 ,sleep: Int = 0)

  def parse_command_line(args: Array[String]): Option[Config] = {
    val parser = new scopt.OptionParser[Config]("PLDLS") {
      head("PLDLS", Utils.VERSION)

      opt[String]('i', "edge_file").required().valueName("<file>").action((x, c) =>
        c.copy(edge_file = x)).text("files of graph edges. e.g. output from GraphGen")

      opt[String]('o', "output").required().valueName("<dir>").action((x, c) =>
        c.copy(output = x)).text("output file")

      opt[Int]("iteration_number").action((x, c) =>
        c.copy(iteration_number = x))
        .text("iteration_number for PLDLS")


      opt[Int]("pregel_iteration").action((x, c) =>
        c.copy(pregel_iteration = x)).
        validate(x =>
          if (x >= 1) success
          else failure("n_output_blocks should be greater than 0"))
        .text("output block number")

      opt[Int]("merge_flag").action((x, c) =>
        c.copy(merge_flag = x)).
        validate(x =>
          if (x >= 1) success
          else failure("min_shared_kmers should be greater than 2"))
        .text("minimum number of kmers that two reads share")

      opt[Int]("modularity_flag").action((x, c) =>
        c.copy(modularity_flag = x)).
        validate(x =>
          if (x >= 1) success
          else failure("max_shared_kmers should be greater than 1"))
        .text("max number of kmers that two reads share")


      opt[Int]("write_to_disk").action((x, c) =>
        c.copy(write_to_disk = x))
        .text("minimum reads per cluster")


      opt[Int]("min_reads_per_cluster").action((x, c) =>
        c.copy(min_reads_per_cluster = x))
        .text("minimum reads per cluster")

      opt[Int]("wait").action((x, c) =>
        c.copy(sleep = x))
        .text("wait $slep second before stop spark session. For debug purpose, default 0.")


      help("help").text("prints this usage text")
    }
    parser.parse(args, Config())
  }

  def pldls(config: Config, spark: SparkSession): Unit = {




//    val dataset_name = "karate"   // write dataset name here
//    val path = "./datasets/" + dataset_name + ".txt"
//    val iteration_number = 5    // number of iterations for fast label selection step
//    val pregel_iteration = 5    // number of iterations for improved label propagation step
//    val merge_flag = 1        // merge_flag = 1 ==> perform merge step ||  merge_flag = 0 ==> do not perform merge step
//    val modularity_flag = 1  // modularity_flag = 1 ==> calculate modularity ||  modularity_flag = 0 ==> do not calculate modularity
//    val write_to_disk = 1   // write_to_disk = 0 ==> write results  || do not write results to disk

    //--------------------------------------------------------------------------
    val sc = spark.sparkContext
    val sqlContext = spark.sqlContext
    sc.setCheckpointDir("hdfs://hadoop50:9000/wxy/sparkCheckpoint")
    val start = System.currentTimeMillis
    logInfo(new java.util.Date(start) + ": Program started ...")
    Logger.getLogger("org").setLevel(Level.ERROR)
//    val conf = new SparkConf()
//    conf.setMaster("local[*]")
//    conf.setAppName("Spark Community Detection")
//    conf.set("spark.driver.maxResultSize", "2g")
//    conf.set("spark.driver.drivermemory", "2g")
//    conf.set("spark.executor.timeout", "1000000")
//    conf.set("spark.network.timeout", "1000000")
//    val sc = new SparkContext(conf)
    //--------------------------------------------------------------------------
    val graph = GraphLoader.edgeListFile(sc,config.edge_file)
    val vertex_degree = graph.degrees
    val node_number: Int = graph.numVertices.toInt //number of vertices
    //************************ nodes neighbors *****************************
    val total_execution_time = System.nanoTime()
    val nodes_neighbors: VertexRDD[List[VertexId]] = graph.aggregateMessages[List[VertexId]](
      sendMsg = triplet => {

        triplet.sendToDst(List(triplet.srcId))
        triplet.sendToSrc(List(triplet.dstId))
      },
      mergeMsg = (x, y) => x ++ y
    )
    //-----------------------------------------------------------------------
    val new_graph = Graph(nodes_neighbors, graph.edges) //make new Graph
    //----------------------- Similarity and node importance compute -----------------------------
    println("Similarity and node importance compute")
    val nodes_similarity_sum: RDD[(VertexId, Double)] = new_graph.aggregateMessages[Double](
      sendMsg = triplet => {
        val srcNeighbor = triplet.srcAttr
        val dstNeighbor = triplet.dstAttr
        //Jaccard coefficient
        val temp_intersect = srcNeighbor.intersect(dstNeighbor).length
        val temp_union = srcNeighbor.union(dstNeighbor).length
        val similarity = (((temp_intersect.toFloat) / (temp_union.toFloat)))

        //Ochiai coefficient:
//        val temp_intersect = srcNeighbor.intersect(dstNeighbor).length
//
//        val temp_union1 = srcNeighbor.length
//        val temp_union2=dstNeighbor.length
//        val sum=temp_union1*temp_union2
//        val sum1=scala.math.pow(sum,0.5)
//        val similarity = (((temp_intersect.toFloat) / (sum1.toFloat)))

        triplet.sendToDst(similarity)
        triplet.sendToSrc(similarity)
      },
      mergeMsg = (x, y) => x + y
    ).join(vertex_degree).mapValues(x => x._1 * (x._2 * x._2))
    
    val nodes_importance = nodes_similarity_sum
    val similaritySum_importance_degree_neighbors = nodes_similarity_sum.join(nodes_importance).join(nodes_neighbors).join(vertex_degree).collect().toMap
    val all_needed_measures = sc.broadcast(similaritySum_importance_degree_neighbors)

    //**************************************************************************************************
    val average_importance = nodes_importance.values.sum() / node_number
    //mode of importance
    val grouped = nodes_importance.filter(x => x._2 >= average_importance).map(x => (x._2.round, 1)).reduceByKey((x, y) => x + y)

    val modeValue = grouped.max()._2
    val modes = grouped.filter(_._2 == modeValue).map(_._1)
    var final_mode = 0.toFloat
    if (modes.count > 1) final_mode = modes.min().toFloat
    else final_mode = modes.max().toFloat

    //-----------------------------------core node label diffusion-----------------------------------------
    println("core node label diffusion")
    val seed_nodes = nodes_importance.filter(x => x._2 >= final_mode)
    case class nodes_properties(label: VertexId, isCoreNode: Boolean = false)
    var work_graph = graph.mapVertices { case (node: Long, property) => nodes_properties(node.toInt, false) }

    val core_and_max_neighbor = seed_nodes.keys.map { i =>
      (i, all_needed_measures.value(i)._1._1._2, all_needed_measures.value(i)._1._2.map(x => (x, all_needed_measures.value(x)._1._1._2)).maxBy(x => x._2))
    }

    val FLIN=core_and_max_neighbor.map(x=>(x._3._1)).distinct().map(x=>(x,all_needed_measures.value(x)._1._2))
    val core_label_select = core_and_max_neighbor.map { i =>
      if (i._2 > i._3._2) (i._1, (i._1, true))
      else (i._1, (i._3._1, true))
    }.map(elem => (elem._1, nodes_properties(elem._2._1.toInt, elem._2._2)))

    work_graph.unpersistVertices(blocking = false)

    work_graph = work_graph.outerJoinVertices(core_label_select) { (id, oldAttr, newAttr) =>
      newAttr match {
        case Some(newAttr) => newAttr
        case None => oldAttr
      }
    }

    val commmon_neighbors = (core_and_max_neighbor.map { i =>
      (i._1, (i._3._1, all_needed_measures.value(i._1)._1._2.intersect(all_needed_measures.value(i._3._1)._1._2)))
    }).join(core_label_select).map(x => (x._2._2.label, x._2._1._2)).flatMapValues(x => x).map(x => x.swap).distinct().reduceByKey { (x, y) =>

      if (all_needed_measures.value(x)._2 >= all_needed_measures.value(y)._2) x
      else y
    }.map(elem => (elem._1, nodes_properties(elem._2, true)))

    work_graph.unpersistVertices(blocking = false)
    work_graph = work_graph.outerJoinVertices(commmon_neighbors) { (id, oldAttr, newAttr) =>
      newAttr match {
        case Some(newAttr) => newAttr
        case None => oldAttr
      }
    }
    //*******************************************************************************
    val FLIN_neighbors =FLIN.join(work_graph.vertices).map(x=>(x._1,(x._2._1,x._2._2.label)))
      .map{ x=>
        (x._2._2, x._2._1.map{y=>
          val intrsct=all_needed_measures.value(x._1)._1._2.intersect(all_needed_measures.value(y)._1._2).length.toFloat
          val unionn=all_needed_measures.value(x._1)._2 + all_needed_measures.value(y)._2.toFloat
          val ratio=intrsct/unionn
          if( (all_needed_measures.value(x._1)._1._1._2 >=  all_needed_measures.value(y)._1._1._2    )  && ratio>=0.5 ) y//   >=  0.5
          else -1 })

      }.mapValues(x=> x.filter(i=> i!= -1)).flatMapValues(x => x).map(x => x.swap).distinct()
      .reduceByKey { (x, y) =>

        if (all_needed_measures.value(x)._2 >= all_needed_measures.value(y)._2) x
        else y
      }.map(elem => (elem._1.asInstanceOf[VertexId], nodes_properties(elem._2, true)))

    work_graph.unpersistVertices(blocking = false)
    work_graph = work_graph.outerJoinVertices(FLIN_neighbors) { (id, oldAttr, newAttr) =>
      newAttr match {
        case Some(newAttr) => newAttr
        case None => oldAttr
      }
    }
    //********************** Second phase - Unlabeled nodes ***************************************
    println("Second phase - Unlabeled nodes")
    val not_updated_nodes = work_graph.vertices.filter(x => x._2.isCoreNode == false).map { case (x, y) => (x, y.label) }
    for (i <- 1 to config.iteration_number.toInt) {

      val not_update_with_neighbors = (not_updated_nodes.map { i =>
        (i._1, all_needed_measures.value(i._1)._1._2)
      }).map { i =>
        (i._1, (i._2.map(j => (j, all_needed_measures.value(j)._1._1._2))))
      }.map { i =>
        (i._1, i._2.maxBy(x => x._2))
      }.map(x => (x._1, x._2._1).swap).join(work_graph.vertices).map(x => (x._2._1, x._2._2.label)).map(elem => (elem._1, nodes_properties(elem._2, true)))

      work_graph.unpersistVertices(blocking = false)

      work_graph = work_graph.outerJoinVertices(not_update_with_neighbors) { (id, oldAttr, newAttr) =>
        newAttr match {
          case Some(newAttr) => newAttr
          case None => oldAttr
        }
      }
    }
    // ********************************** improved label propagation ******************************
    println("improved label propagation")
    def run[VD, ED: ClassTag](graph: Graph[VD, ED], maxSteps: Int): Graph[VertexId, ED] = {

      val lpaGraph = graph.mapVertices { case vid => vid._2.asInstanceOf[VertexId] }
      def sendMessage(e: EdgeTriplet[VertexId, ED]): Iterator[(VertexId, Map[VertexId, Long])] = {
        Iterator((e.srcId, Map(e.dstAttr -> 1L)), (e.dstId, Map(e.srcAttr -> 1L)))
      }

      def mergeMessage(count1: Map[VertexId, Long], count2: Map[VertexId, Long])
      : Map[VertexId, Long] = {
        val map = mutable.Map[VertexId, Long]()
        (count1.keySet ++ count2.keySet).foreach { i =>
          val count1Val = count1.getOrElse(i, 0L)
          val count2Val = count2.getOrElse(i, 0L)
          map.put(i, count1Val + count2Val)
        }
        map
      }

      def vertexProgram(vid: VertexId, attr: Long, message: Map[VertexId, Long]): VertexId = {
        if (message.isEmpty) attr else {
          if (all_needed_measures.value(attr)._1._1._2 >= all_needed_measures.value(message.maxBy(_._2)._1)._1._1._2) attr
          else message.maxBy(_._2)._1
        }
      }

      val initialMessage = Map[VertexId, Long]()
      Pregel(lpaGraph, initialMessage, maxIterations = maxSteps)(
        vprog = vertexProgram,
        sendMsg = sendMessage,
        mergeMsg = mergeMessage)
    }

      val lpaGraph = work_graph.mapVertices { case (id, node) => node.label }
      val new_updated_graph = run(lpaGraph, config.pregel_iteration.toInt)
      work_graph.unpersist(blocking = false)
      work_graph = new_updated_graph.mapVertices { case (node: Long, property) => nodes_properties(property) }

    //************************** Merging Step***********************
    println("Merging Step")
      if (config.merge_flag == 1) {
        val nodes_labels = work_graph.vertices.map(x => (x._1, x._2.label)).join(vertex_degree).map(x => (x._2._1, (x._1, x._2._2)))
        val createCombiner = (v: (VertexId, Int)) => List(v)
        val mergeValue = (a: List[(VertexId, Int)], b: (VertexId, Int)) => a.::(b)
        val mergeCombiners = (a: List[(VertexId, Int)], b: List[(VertexId, Int)]) => a.++(b)
        val communities: RDD[(VertexId, List[(VertexId, PartitionID)])] = nodes_labels.combineByKey(createCombiner, mergeValue, mergeCombiners)

        val community_with_size = communities.map(x => (x._1, x._2.length))
        val max_size_community = community_with_size.map(x => x.swap).max()

        val average_size = (community_with_size.values.sum() - max_size_community._1) / (communities.count() - 1)
        //val less_than_average_communities = communities.filter(x => x._2.length <= average_size) //filter(x => x._2.length <= average_size)

        //if (!less_than_average_communities.isEmpty()) {

        val candidate_neighbor_degree = communities.map(x => (x._1, x._2.maxBy(i => i._2)))
          .map(x => (x._1, x._2, all_needed_measures.value(x._1)._1._2))
          .map(x => (x._1, x._2, x._3.map(y => (y, all_needed_measures.value(y)._2))))
          .map(x => (x._1, x._2, x._3.maxBy(i => i._2)))
          .map { i =>

            if (i._2._2 <= i._3._2) (i._1, (i._1, i._3._1))
            else (i._1, (i._1, i._2._1))
          }.map(x => (x._1, x._2._2).swap).join(work_graph.vertices).map(x => (x._2._1, x._2._2.label))

        val new_labels = communities.fullOuterJoin(candidate_neighbor_degree)
          .map(x => if (x._2._2.isDefined) (x._2._2, x._2._1) else (x._1, x._2._1))
          .flatMapValues(x => x.map(y => y.map(z => z._1)))
          .flatMapValues(x => x).map(x => (x._2, x._1))
          .mapValues {
            case Some(x) => x
            case x => x
          }.map(elem => (elem._1, elem._2.asInstanceOf[VertexId]))
          .map(elem => (elem._1, nodes_properties(elem._2, true)))

        work_graph.unpersistVertices(blocking = false)

        work_graph = work_graph.outerJoinVertices(new_labels) { (id, oldAttr, newAttr) =>
          newAttr match {
            case Some(newAttr) => newAttr
            case None => oldAttr
          }
        }
    }

    println("Merging Step")
    if (config.merge_flag == 1) {
      val nodes_labels = work_graph.vertices.map(x => (x._1, x._2.label)).join(vertex_degree).map(x => (x._2._1, (x._1, x._2._2)))
      val createCombiner = (v: (VertexId, Int)) => List(v)
      val mergeValue = (a: List[(VertexId, Int)], b: (VertexId, Int)) => a.::(b)
      val mergeCombiners = (a: List[(VertexId, Int)], b: List[(VertexId, Int)]) => a.++(b)
      val communities: RDD[(VertexId, List[(VertexId, PartitionID)])] = nodes_labels.combineByKey(createCombiner, mergeValue, mergeCombiners)

      val community_with_size = communities.map(x => (x._1, x._2.length))
      val max_size_community = community_with_size.map(x => x.swap).max()

      val average_size = (community_with_size.values.sum() - max_size_community._1) / (communities.count() - 1)
      val less_than_average_communities = communities.filter(x => x._2.length <= average_size) //filter(x => x._2.length <= average_size)

      if (!less_than_average_communities.isEmpty()) {

        val candidate_neighbor_degree = less_than_average_communities.map(x => (x._1, x._2.maxBy(i => i._2))) //改进：   原算法：less_than_average_communities.map(x => (x._1, x._2.maxBy(i => i._2)))
          .map(x => (x._1, x._2, all_needed_measures.value(x._1)._1._2))
          .map(x => (x._1, x._2, x._3.map(y => (y, all_needed_measures.value(y)._2))))
          .map(x => (x._1, x._2, x._3.maxBy(i => i._2)))
          .map { i =>

            if (i._2._2 <= i._3._2) (i._1, (i._1, i._3._1))
            else (i._1, (i._1, i._2._1))
          }.map(x => (x._1, x._2._2).swap).join(work_graph.vertices).map(x => (x._2._1, x._2._2.label))

        val new_labels = communities.fullOuterJoin(candidate_neighbor_degree)
          .map(x => if (x._2._2.isDefined) (x._2._2, x._2._1) else (x._1, x._2._1))
          .flatMapValues(x => x.map(y => y.map(z => z._1)))
          .flatMapValues(x => x).map(x => (x._2, x._1))
          .mapValues {
            case Some(x) => x
            case x => x
          }.map(elem => (elem._1, elem._2.asInstanceOf[VertexId]))
          .map(elem => (elem._1, nodes_properties(elem._2, true)))

        work_graph.unpersistVertices(blocking = false)

        work_graph = work_graph.outerJoinVertices(new_labels) { (id, oldAttr, newAttr) =>
          newAttr match {
            case Some(newAttr) => newAttr
            case None => oldAttr
          }
        }
      }
    }




    println("time (total_execution_time): "+(System.nanoTime-total_execution_time)/1e9)

    // ----------------------------- Modularity ------------------------------------
//    println("Modularity")
//    if(config.modularity_flag==1) {
//
//      val modularity_time = System.nanoTime()
//
//      val edge_number = work_graph.edges.count().toFloat
//      val nodes_labels = work_graph.vertices.map(x => (x._1, x._2.label)).join(vertex_degree).map(x => (x._2._1, (x._1, x._2._2)))
//      val createCombiner = (v:(VertexId,Int)) => List(v)
//      val mergeValue = (a:List[(VertexId,Int)], b:(VertexId, Int)) => a.::(b)
//      val mergeCombiners = (a:List[(VertexId, Int)],b:List[(VertexId, Int)]) => a.++(b)
//      val communities = nodes_labels.combineByKey(createCombiner,mergeValue, mergeCombiners)
//      communities.repartition(8)
//
//      val modu: Double = communities.map {
//        x =>
//          var local_modularity = 0F
//          for (elem <- x._2) {
//
//            for (elem2 <- x._2) {
//
//              var are_neighbor = 0F
//              if (all_needed_measures.value(elem._1)._1._2.contains(elem2._1)) are_neighbor = 1F
//              else are_neighbor = 0F
//              local_modularity = local_modularity + (are_neighbor - ((elem._2 * elem2._2) / (2 * edge_number)))
//
//            }
//          }
//
//          (x._1, local_modularity / (2 * edge_number))
//      }.values.sum()
//
//      val community_number = communities.count()
//      println("Community number:" + community_number)
//      println("Modularity: " + modu)
//      println("time (modularity_time): "+(System.nanoTime-modularity_time)/1e9)
//    }

    //********************** Write RDD to Text file *********************************
    println(" Write RDD to Text file")
    if(config.write_to_disk==1) {
    val final_result=work_graph.vertices.map{x=>(x._1,x._2.label)}//.groupBy(_._2)//.map(x=>x._2)
    val CCAddSeq=final_result.groupBy(_._2).mapValues(_.map({case(a,b)=>a})).filter(_._2.size>=config.min_reads_per_cluster)

    println("CCaddSeq2------------")
    CCAddSeq.take(10).foreach(println(_))

      var CCaddSeq2=CCAddSeq.map{
        line=>line._2.mkString(",")
     }

     println("CCaddSeq2------------")
     CCaddSeq2.take(10).foreach(println(_))
     CCaddSeq2.coalesce(1).saveAsTextFile(config.output)
      //final_result.saveAsTextFile(config.output)
      //val sorted_result=final_result.sortByKey(numPartitions = 1).map(x=>x._2)
      //val dataset_folder="./results/"+config.output
      //sorted_result.saveAsTextFile(config.output)
    }
  }


  override def main(args: Array[String]) {
    val APPNAME = "PLDLS"

    val options = parse_command_line(args)

    options match {
      case Some(_) =>
        val config = options.get

        val conf = new SparkConf().setAppName(APPNAME)
        conf.registerKryoClasses(Array(classOf[DNASeq]))

        val spark = SparkSession
          .builder().config(conf)
          .appName(APPNAME)
          .getOrCreate()

        pldls(config, spark)
        if (config.sleep > 0) Thread.sleep(config.sleep * 1000)
        spark.stop()
      case None =>
        println("bad arguments")
        sys.exit(-1)
    }
  } //main


}
