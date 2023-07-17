package org.apache.spark.graphx.lib

import org.apache.spark.graphx._
import org.apache.spark.internal.Logging
import org.apache.spark.storage.StorageLevel
import org.jgi.spark.localcluster.tools.GraphLPA2.logInfo

import scala.reflect.ClassTag
object MyLabelPropagation {
    def run[VD, ED: ClassTag](graph: Graph[VD, ED], maxSteps: Int): Graph[VertexId, ED] = {
        logInfo(s"LPA1")
      require(maxSteps > 0, s"Maximum of steps must be greater than 0, but got ${maxSteps}")

      val lpaGraph = graph.mapVertices { case (vid, _) => vid }
        logInfo(s"LPA2")

      def sendMessage(e: EdgeTriplet[VertexId, ED]): Iterator[(VertexId, Map[VertexId, Int])] = {
        Iterator((e.srcId, Map(e.dstAttr -> 1)), (e.dstId, Map(e.srcAttr -> 1)))
      }
        logInfo(s"LPA3")

        def mergeMessage(count1: Map[VertexId, Int], count2: Map[VertexId, Int]): Map[VertexId, Int] = {
        (count1.keySet ++ count2.keySet).map { i =>
          val count1Val = count1.getOrElse(i, 0)
          val count2Val = count2.getOrElse(i, 0)
          i -> (count1Val + count2Val)
        }.toMap
      }
        logInfo(s"LPA4")

        def vertexProgram(vid: VertexId, attr: Long, message: Map[VertexId, Int]): VertexId = {
        if (message.isEmpty) attr else message.toSeq.sortBy(u=>(-u._2,u._1)).head._1
      }
        logInfo(s"LPA5")

      val initialMessage = Map[VertexId, Int]()

        logInfo(s"LPA6")
      MyPregel(lpaGraph, initialMessage, maxIterations = maxSteps)(
        vprog = vertexProgram,
        sendMsg = sendMessage,
        mergeMsg = mergeMessage)
    }

}

object MyPregel extends Logging {
  def apply[VD: ClassTag, ED: ClassTag, A: ClassTag]
  (graph: Graph[VD, ED],
   initialMsg: A,
   maxIterations: Int = Int.MaxValue,
   activeDirection: EdgeDirection = EdgeDirection.Either)
  (vprog: (VertexId, VD, A) => VD,
   sendMsg: EdgeTriplet[VD, ED] => Iterator[(VertexId, A)],
   mergeMsg: (A, A) => A)
  : Graph[VD, ED] = {

    logInfo(s"pregel1")
    require(maxIterations > 0, s"Maximum number of iterations must be greater than 0," +
      s" but got ${maxIterations}")

    logInfo(s"pregel2")
    var g = graph.mapVertices((vid, vdata) => vprog(vid, vdata, initialMsg)).persist(StorageLevel.MEMORY_AND_DISK_SER)

    logInfo(s"pregel3")
    var messages = GraphXUtils.mapReduceTriplets(g, sendMsg, mergeMsg).persist(StorageLevel.MEMORY_AND_DISK_SER)

    logInfo(s"pregel4")
    var activeMessages = messages.count()

    logInfo(s"pregel5")
    var prevG: Graph[VD, ED] = null
    var i = 0

    while (activeMessages > 0 && i < maxIterations) {

      logInfo(s"pregel6")
      prevG = g

      g = g.joinVertices(messages)(vprog).persist(StorageLevel.MEMORY_AND_DISK_SER)

      val oldMessages = messages
      oldMessages.unpersist(blocking = false)

      messages = GraphXUtils.mapReduceTriplets(g, sendMsg, mergeMsg,None).persist(StorageLevel.MEMORY_AND_DISK_SER)

      activeMessages = messages.count()

      logInfo("Pregel finished iteration " + i)

      // Unpersist the RDDs hidden by newly-materialized RDDs
      prevG.unpersistVertices(blocking = false)
      prevG.edges.unpersist(blocking = false)
      // count the iteration
      i += 1
      System.gc()
    }
    messages.unpersist(blocking = false)
    g
  } // end of apply

} // end of class Pregel
