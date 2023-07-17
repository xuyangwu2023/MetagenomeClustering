package org.apache.spark.graphx.lib

import org.apache.spark.graphx.Pregel.logInfo
import org.apache.spark.graphx.util.PeriodicGraphCheckpointer
import org.apache.spark.graphx.{EdgeDirection, EdgeTriplet, Graph, GraphXUtils, Pregel, VertexId}
import org.apache.spark.internal.Logging
import org.apache.spark.rdd.RDD
import org.apache.spark.rdd.util.PeriodicRDDCheckpointer
import org.apache.spark.storage.StorageLevel

import scala.collection.mutable
import scala.reflect.ClassTag

object OverlappingCommunityDetection {

  /**
   * Run Overlapping Community Detection for detecting overlapping communities in networks.----COPRA
   *
   * OLPA is an overlapping community detection algorithm.It is based on standarad Label propagation
   * but instead of single community per node , multiple communities can be assigned per node.
   *
   * @tparam ED the edge attribute type (not used in the computation)
   * @param graph           the graph for which to compute the community affiliation
   * @param maxSteps        the number of supersteps of OLPA to be performed. Because this is a static
   *                        implementation, the algorithm will run for exactly this many supersteps.
   * @param noOfCommunities the maximum number of communities to be assigned to each vertex
   * @return a graph with list of vertex attributes containing the labels of communities affiliation
   */

  def run[VD, ED: ClassTag](graph: Graph[VD, ED], maxSteps: Int, noOfCommunities: Int) = {
    require(maxSteps > 0, s"Maximum of steps must be greater than 0, but got ${maxSteps}")
    require(noOfCommunities > 0, s"Number of communities must be greater than 0, but got ${noOfCommunities}")

    val threshold: Double =  3.0/noOfCommunities

    val lpaGraph: Graph[mutable.Map[VertexId, Double], ED] = graph.mapVertices { case (vid, _) => mutable.Map[VertexId, Double](vid -> 1) }

    def sendMessage(e: EdgeTriplet[mutable.Map[VertexId, Double], ED]): Iterator[(VertexId, mutable.Map[VertexId, Double])] = {
      Iterator((e.srcId, e.dstAttr), (e.dstId, e.srcAttr))
    }

    def mergeMessage(count1: mutable.Map[VertexId, Double], count2: mutable.Map[VertexId, Double])
    : mutable.Map[VertexId, Double] = {
      val communityMap: mutable.Map[VertexId, Double] = new mutable.HashMap[VertexId, Double]
      (count1.keySet ++ count2.keySet).map(key => {

        val count1Val = count1.getOrElse(key, 0.0)
        val count2Val = count2.getOrElse(key, 0.0)
        communityMap += (key -> (count1Val + count2Val))
      })
      communityMap
    }

    def vertexProgram(vid: VertexId, attr: mutable.Map[VertexId, Double], message: mutable.Map[VertexId, Double]): mutable.Map[VertexId, Double] = {
      if (message.isEmpty)
        attr
      else {
        var coefficientSum = message.values.sum

        val normalizedMap: mutable.Map[VertexId, Double] = message.map(row => {
          (row._1 -> (row._2 / coefficientSum))
        })


        val resMap: mutable.Map[VertexId, Double] = new mutable.HashMap[VertexId, Double]
        var maxRow: VertexId = 0L
        var maxRowValue: Double = Double.MinValue

        normalizedMap.foreach(row => {
          if (row._2 >= threshold) {
            resMap += row
          } else if (row._2 > maxRowValue) {
            maxRow = row._1
            maxRowValue = row._2
          }
        })

        if (resMap.isEmpty) {   //Add maximum value node in result map if there is no node with sum greater then threshold
          resMap += (maxRow -> maxRowValue)
        }

        coefficientSum = resMap.values.sum
        resMap.map(row => {
          (row._1 -> (row._2 / coefficientSum))
        })
      }
    }

    val initialMessage = mutable.Map[VertexId, Double]()

    val overlapCommunitiesGraph = OPregel(lpaGraph, initialMessage, maxIterations = maxSteps)(
      vprog = vertexProgram,
      sendMsg = sendMessage,
      mergeMsg = mergeMessage)

    overlapCommunitiesGraph.mapVertices((vertexId, vertexProperties) => vertexProperties.keys)
  }
}


object OPregel extends Logging {

  /**
   * Execute a Pregel-like iterative vertex-parallel abstraction.  The
   * user-defined vertex-program `vprog` is executed in parallel on
   * each vertex receiving any inbound messages and computing a new
   * value for the vertex.  The `sendMsg` function is then invoked on
   * all out-edges and is used to compute an optional message to the
   * destination vertex. The `mergeMsg` function is a commutative
   * associative function used to combine messages destined to the
   * same vertex.
   *
   * On the first iteration all vertices receive the `initialMsg` and
   * on subsequent iterations if a vertex does not receive a message
   * then the vertex-program is not invoked.
   *
   * This function iterates until there are no remaining messages, or
   * for `maxIterations` iterations.
   *
   * @tparam VD the vertex data type
   * @tparam ED the edge data type
   * @tparam A the Pregel message type
   *
   * @param graph the input graph.
   *
   * @param initialMsg the message each vertex will receive at the first
   * iteration
   *
   * @param maxIterations the maximum number of iterations to run for
   *
   * @param activeDirection the direction of edges incident to a vertex that received a message in
   * the previous round on which to run `sendMsg`. For example, if this is `EdgeDirection.Out`, only
   * out-edges of vertices that received a message in the previous round will run. The default is
   * `EdgeDirection.Either`, which will run `sendMsg` on edges where either side received a message
   * in the previous round. If this is `EdgeDirection.Both`, `sendMsg` will only run on edges where
   * *both* vertices received a message.
   *
   * @param vprog the user-defined vertex program which runs on each
   * vertex and receives the inbound message and computes a new vertex
   * value.  On the first iteration the vertex program is invoked on
   * all vertices and is passed the default message.  On subsequent
   * iterations the vertex program is only invoked on those vertices
   * that receive messages.
   *
   * @param sendMsg a user supplied function that is applied to out
   * edges of vertices that received messages in the current
   * iteration
   *
   * @param mergeMsg a user supplied function that takes two incoming
   * messages of type A and merges them into a single message of type
   * A.  ''This function must be commutative and associative and
   * ideally the size of A should not increase.''
   *
   * @return the resulting graph at the end of the computation
   *
   */
  def apply[VD: ClassTag, ED: ClassTag, A: ClassTag]
  (graph: Graph[VD, ED],
   initialMsg: A,
   maxIterations: Int = Int.MaxValue,
   activeDirection: EdgeDirection = EdgeDirection.Either)
  (vprog: (VertexId, VD, A) => VD,
   sendMsg: EdgeTriplet[VD, ED] => Iterator[(VertexId, A)],
   mergeMsg: (A, A) => A)
  : Graph[VD, ED] =
  {
    require(maxIterations > 0, s"Maximum number of iterations must be greater than 0," +
      s" but got ${maxIterations}")

    val checkpointInterval = graph.vertices.sparkContext.getConf
      .getInt("spark.graphx.pregel.checkpointInterval", -1)
    var g = graph.mapVertices((vid, vdata) => vprog(vid, vdata, initialMsg)).persist(StorageLevel.MEMORY_AND_DISK_SER)
    val graphCheckpointer = new PeriodicGraphCheckpointer[VD, ED](
      checkpointInterval, graph.vertices.sparkContext)
    graphCheckpointer.update(g)

    // compute the messages
    var messages = GraphXUtils.mapReduceTriplets(g, sendMsg, mergeMsg).persist(StorageLevel.MEMORY_AND_DISK_SER)
    val messageCheckpointer = new PeriodicRDDCheckpointer[(VertexId, A)](
      checkpointInterval, graph.vertices.sparkContext)
    messageCheckpointer.update(messages.asInstanceOf[RDD[(VertexId, A)]])
    var activeMessages = messages.count()

    // Loop
    var prevG: Graph[VD, ED] = null
    var i = 0
    while (activeMessages > 0 && i < maxIterations) {
      // Receive the messages and update the vertices.
      prevG = g
      g = g.joinVertices(messages)(vprog).persist(StorageLevel.MEMORY_AND_DISK_SER)
      graphCheckpointer.update(g)

      val oldMessages = messages
      // Send new messages, skipping edges where neither side received a message. We must cache
      // messages so it can be materialized on the next line, allowing us to uncache the previous
      // iteration.
      messages = GraphXUtils.mapReduceTriplets(
        g, sendMsg, mergeMsg, Some((oldMessages, activeDirection))).persist(StorageLevel.MEMORY_AND_DISK_SER)
      // The call to count() materializes `messages` and the vertices of `g`. This hides oldMessages
      // (depended on by the vertices of g) and the vertices of prevG (depended on by oldMessages
      // and the vertices of g).
      messageCheckpointer.update(messages.asInstanceOf[RDD[(VertexId, A)]])
      activeMessages = messages.count()

      logInfo("Pregel finished iteration " + i)

      // Unpersist the RDDs hidden by newly-materialized RDDs
      oldMessages.unpersist()
      prevG.unpersistVertices()
      prevG.edges.unpersist()
      // count the iteration
      i += 1
    }
    messageCheckpointer.unpersistDataSet()
    graphCheckpointer.deleteAllCheckpoints()
    messageCheckpointer.deleteAllCheckpoints()
    g
  } // end of apply

} // end of class Pregel

