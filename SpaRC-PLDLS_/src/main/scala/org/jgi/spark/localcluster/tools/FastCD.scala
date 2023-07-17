package org.jgi.spark.localcluster.tools

import com.typesafe.scalalogging.LazyLogging
import org.apache.spark.SparkContext
import org.apache.spark.graphx._
import org.apache.spark.rdd.RDD
import org.apache.spark.broadcast.Broadcast
import org.apache.spark.rdd.RDD.rddToPairRDDFunctions
import org.apache.spark.sql.SparkSession
import org.jgi.spark.localcluster.{DNASeq, Utils}
import org.apache.spark.SparkConf
import org.apache.spark.graphx.{Edge, Graph, PartitionStrategy, lib => graphxlib}
import sext._

import scala.collection.mutable.ListBuffer
import scala.reflect.ClassTag
object FastCD extends App with LazyLogging{

  case class Config(edgesFile: String = "",verticeFile: String="",output_edge:String= "",output_vertice:String ="",output_CCaddSeq:String="",numPartitions:Int=0,min_reads_per_cluster:Int=2)

  def parse_command_line(args: Array[String]): Option[Config] = {
    val parser = new scopt.OptionParser[Config]("FastCD") {
      head("FastCD", Utils.VERSION)

      opt[String]('E', "edgesFile").required().valueName("<file>").action((x, c) =>
        c.copy( edgesFile = x)).text("files of graph edges. e.g. output from GraphGen")

      opt[String]('V', "verticeFile").required().valueName("<file>").action((x, c) =>
        c.copy(verticeFile = x)).text("files of graph vertices. e.g. output from GraphGen")

      opt[String]('o', "output_edge").required().valueName("<dir>").action((x, c) =>
        c.copy(output_edge = x)).text("output file")

      opt[String]('o', "output_vertice").required().valueName("<dir>").action((x, c) =>
        c.copy(output_vertice = x)).text("output file")

      opt[String]('o', "output_CCaddSeq").required().valueName("<dir>").action((x, c) =>
        c.copy(output_CCaddSeq = x)).text("output file")

      opt[Int]('n', "numPartitions").action((x, c) =>
        c.copy( numPartitions = x))
        .text("paritions for the input")


      opt[Int]("min_reads_per_cluster").action((x, c) =>
        c.copy(min_reads_per_cluster = x))
        .text("minimum reads per cluster")

      help("help").text("prints this usage text")
    }
    parser.parse(args, Config())
  }
 
 // update the information of nodes
 def updateNodeData(graph: Graph[NodeData, Long]):RDD[(VertexId, NodeData)]={
      graph.aggregateMessages[(Long,Long,Long,Long,NodeData)](
       //edges attribute,  the communityId of sender, Ic ,Sc,the information of receiver
         triplet =>
              {   triplet.sendToDst(triplet.attr,triplet.srcAttr.communityID,0,0,triplet.dstAttr)
                triplet.sendToSrc(triplet.attr,triplet.dstAttr.communityID,0,0,triplet.srcAttr)
               } ,

         (a,b)=> // reduce message if receiver receive more than one message
           {
             var li=b._3+a._3
               li+=a._1
               li+=b._1
              var si=0L
              if(a._2!=a._5.communityID)
              {
                si+=a._1
              }
              if(b._2!=b._5.communityID)
              {
                si+=b._1
              }
             (0L,-1L,li,si,a._5)
           }

        ).map{// create new vertice
     x=>
         var node=new NodeData(x._2._5.nodeid,x._2._5.communityID)
         var li=x._2._3
         var si=x._2._4
         if(li==0 )li=x._2._1
         if( si==0 && x._2._2!=x._2._5.communityID) si=x._2._1
         
          node.communityDegreeSum=li
          node.neighCommunityDegreeSum=si
          node.communityAllId=x._2._5.communityAllId
         Tuple2(x._1,node)
        }
 } 
 
 def distributeCommunity(graph: Graph[NodeData, Long],w:Broadcast[Long]):Graph[NodeData,Long]={
      var old=graph.vertices

       //neighCommunityDegreeSum
   //
       var newvertexRDD:RDD[(VertexId, NodeData)]= graph.aggregateMessages[(Long,NodeData,NodeData,Double)](
           triplet=> if(triplet.srcAttr.communityID!=triplet.dstAttr.communityID){
             triplet.sendToDst(triplet.attr,triplet.srcAttr,triplet.dstAttr,0)
             triplet.sendToSrc(triplet.attr,triplet.dstAttr,triplet.srcAttr,0)
            },
          (a,b)=>
            {
             var avalue=0.0
             var bvalue=0.0
            if(a._1!=0) avalue=(w.value*a._1-a._2.neighCommunityDegreeSum*a._3.communityDegreeSum)/(2.0*w.value*w.value)
             if(b._1!=0) bvalue=(w.value*b._1-b._2.neighCommunityDegreeSum*b._3.communityDegreeSum)/(2.0*w.value*w.value)
             if(avalue<=0 && bvalue<=0) (0,null,null,0)
             else if(avalue>bvalue)(a._1,a._2,a._3,avalue)
             else (b._1,b._2,b._3,bvalue)
            }
        ).filter(x=>x._2._1!=0).map{
        data=>
          {  
            var node=new NodeData(data._1,data._2._2.communityID)
            node.communityDegreeSum=data._2._3.communityDegreeSum
            node.Q=data._2._4
           //node.sci=data._2._3.sci
            node.isUpdate=true
           Tuple2(data._1,node)
          }
        }

     newvertexRDD=old.union(newvertexRDD).reduceByKey{
         (x,y)=>if(x.isUpdate) {
           x.isUpdate=false
           x
         }
         else
          {
           y.isUpdate=false
           y
          }
       }
       var tgraph= Graph(newvertexRDD,graph.edges).cache() 
       var pregraph=tgraph


       var subRdd:RDD[(VertexId, NodeData)]=tgraph.aggregateMessages[(NodeData)](
           tri=>{
             var node:NodeData= null
             
             if(tri.dstAttr.communityID==tri.srcAttr.nodeid && tri.srcAttr.communityID!=tri.srcAttr.nodeid)
             {
                node=tri.dstAttr
                node.communityID=tri.srcAttr.communityID
                node.isUpdate=true
                tri.sendToDst(node)
             }
             else if(tri.srcAttr.communityID==tri.dstAttr.nodeid && tri.dstAttr.communityID!=tri.dstAttr.nodeid)
             {
                node=tri.srcAttr
                node.communityID=tri.dstAttr.communityID
                node.isUpdate=true
                tri.sendToSrc(node)
             }
           },
           (a,b)=>b
       )
     
      newvertexRDD=newvertexRDD.union(subRdd).reduceByKey{
         (x,y)=>if(x.isUpdate) {
           x.isUpdate=false
           x
         }
         else
          {
           y.isUpdate=false
           y
          }
       }

     println("add->AllId--------------")
   val newvertexRDD1: RDD[(VertexId, NodeData)] = newvertexRDD.groupBy(_._2.communityID).map(line => {
     val temp: ListBuffer[VertexId] = ListBuffer()
     val result: ListBuffer[(VertexId, NodeData)] = ListBuffer()
     line._2.foreach(e => temp += e._2.nodeid)
     for (elem <- line._2) {
       val tempNode: NodeData = elem._2
       temp.foreach(tempNode.communityAllId += _)
       result += ((elem._1, tempNode))
     }
     result.toList
   }).flatMap(x => x)
   newvertexRDD1.take(10).foreach(println(_))


  // tgraph= Graph(newvertexRDD,tgraph.edges).cache()//
       tgraph= Graph(newvertexRDD1,tgraph.edges).cache()
       pregraph.unpersist(false)
       pregraph=tgraph

       var edges=EdgesReduce(tgraph)
       tgraph= Graph(newvertexRDD1,edges).cache()
       pregraph.unpersist(false)
       pregraph=tgraph
       println("EdgesReduce(tgraph)-----------")
       tgraph.vertices.take(10).foreach(println(_))
       tgraph.edges.take(10).foreach(println(_))

       newvertexRDD=updateNodeData(tgraph)
       pregraph.unpersist(false)
       Graph(newvertexRDD,edges)
 }
  // update the information of edges
def EdgesReduce(graph: Graph[NodeData, Long]):RDD[Edge[Long]]={
            val temp1=graph.triplets.map{f=>
               Edge(f.srcAttr.communityID,f.dstAttr.communityID,(f.attr)) //(4,7,1)
             }

              val temp2=temp1.flatMap {
               e=>
                 {
                   var key=""
                   if(e.srcId>e.dstId) key=e.srcId+"-"+e.dstId
                   else key=e.dstId+"-"+e.srcId
                   List((key,e.attr))
                 }
             }
                val temp3=temp2.reduceByKey(_ + _).map
             {
               e=>
                 {
                   var src=e._1.split("-")
                   Edge(src(0).toLong,src(1).toLong,e._2)
                 }
             }
  temp3
} 

// execute fastCD Algorithm 
 def run(config: Config, sc: SparkContext)=
  {
    // sc = spark.sparkContext
    //val sqlContext = spark.sqlContext
    sc.setCheckpointDir("hdfs://hadoop50:9000/wxy/sparkCheckpoint")
    val start = System.currentTimeMillis
    println(new java.util.Date(start) + ": Program started ...")


        var run=true
         var edges:RDD[Edge[(Long)]]=sc.textFile(config.edgesFile, config.numPartitions).map{
          line=>
              var record=line.split(",")    //split(',')
              if(record.length!=2) record=line.split(",")
              Edge(record(0).toLong,record(1).toLong,record(2).toLong)
          }

       //count w.value
       val w = sc.broadcast(edges.reduce((x1,x2)=>new Edge(x1.srcId,x2.dstId,x1.attr+x2.attr)).attr)
       var vertices:RDD[(VertexId, NodeData)]= sc.textFile(config.verticeFile, config.numPartitions).map(line => {
                var lineLong=line.toLong
                var node=new NodeData(lineLong,lineLong)
                Tuple2(lineLong,node)
            })
       println("vertices..........initial")
       vertices.take(10).foreach(println(_))
       println("edges..........initial")
       edges.take(10).foreach(println(_))

        var graph= Graph(vertices,edges).cache()// the pregraph is used for release the cache of graph
        var pregraph=graph // count the number of vertices if the property named Q in NodeData is more than 0
        var newnum:Long=0L
        var newvertexRDD=updateNodeData(graph)

        graph= Graph(newvertexRDD,edges).cache() 
        pregraph.unpersist(blocking=false)
        pregraph=graph
      do{
        graph= distributeCommunity(graph,w)
        graph.cache()
        pregraph.unpersist(false)
        pregraph.edges.unpersist(false)
        newnum=graph.vertices.filter(x=>x._2.Q>0).count()
        if(newnum==0) run=false 
     }while(run)

    println("results-------------")
    graph.vertices.take(10).foreach(println(_))
    graph.edges.take(10).foreach(println(_))

    graph
    val totalTime1 = System.currentTimeMillis
    println("Processing time: %.2f minutes".format((totalTime1 - start).toFloat / 60000))

    var CCaddSeq= graph.vertices.groupBy(_._2.communityID).map{line =>
      line._2.head._2.communityAllId}.filter(x=>x.size>=config.min_reads_per_cluster)

    var CCaddSeq2=CCaddSeq.map{
      line=>line.mkString(",")
    }
    println("CCaddSeq2------------")
    CCaddSeq2.take(10).foreach(println(_))

    println("save..............")
    graph.vertices.coalesce(1).saveAsTextFile(config.output_vertice)
    graph.edges.coalesce(1).saveAsTextFile(config.output_edge)
    CCaddSeq2.coalesce(1).saveAsTextFile(config.output_CCaddSeq)
  }

 def getQAndCommunityNumbers(sc:SparkContext,graph: Graph[NodeData, Long])={
 val w = sc.broadcast(graph.edges.reduce((x1,x2)=>new Edge(x1.srcId,x2.dstId,x1.attr+x2.attr)).attr)
  val Qs=graph.aggregateMessages[(NodeData)](
       {tri=>
         if(tri.srcAttr.communityID==tri.dstAttr.communityID ) 
         {
         if(tri.srcAttr.nodeid==tri.srcAttr.communityID)tri.sendToSrc(tri.srcAttr) 
         else if(tri.dstAttr.nodeid==tri.dstAttr.communityID)
           tri.sendToDst(tri.dstAttr) 
         }
       },
       //  find the nodes that can represent different communities.If the number of message is more than one ,a must equal b
         (a,b)=> 
           {
            a
           }
     ).map{
       x=>
         {
           val Sc=x._2.neighCommunityDegreeSum
           val Ic=x._2.communityDegreeSum
           val ID=x._2.communityID
           val q=(Ic-Sc*Sc*1.0/(2*w.value))/(2*w.value)
           Tuple2(ID,q)
         }    
}.collect()
     val iter=Qs.iterator
     var sum=0.0
     var number=0L
     while(iter.hasNext)
     {
       var line=iter.next()
       sum+=line._2
       number+=1
     }
     Tuple2(sum,number)
 }

  override def main(args: Array[String]) {
    val APPNAME = "FastCD"

    val options = parse_command_line(args)

    options match {
      case Some(_) =>
        val config = options.get

        println(s"called with arguments\n${options.valueTreeString}")

        val conf = new SparkConf().setAppName("FastCD")
        conf.registerKryoClasses(Array(classOf[DNASeq]))

        val sc = new SparkContext(conf)
//        val spark = SparkSession
//          .builder().config(conf)
//          .appName(APPNAME)
//          .getOrCreate()

        run(config, sc)
        sc.stop()
      case None =>
        println("bad arguments")
        sys.exit(-1)
    }
  } //main

}
