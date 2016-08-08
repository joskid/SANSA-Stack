package org.sansa.inference.spark

import org.apache.spark.SparkConf
import org.apache.spark.sql.SparkSession
import org.sansa.inference.data.RDFTriple
import org.sansa.inference.spark.data.{RDFGraph, RDFGraphDataFrame, RDFGraphLoader, RDFGraphWriter}
import org.sansa.inference.spark.forwardchaining.{ForwardRuleReasonerOptimizedSQL, ForwardRuleReasonerRDFS, ForwardRuleReasonerRDFSDataframe}
import org.sansa.inference.utils.{Profiler, RuleUtils}

/**
  * @author Lorenz Buehmann
  */
object RDDVsDataframeExperiments extends Profiler{

  val conf = new SparkConf()
  conf.registerKryoClasses(Array(classOf[RDFTriple]))

  // the SPARK config
  val session = SparkSession.builder
    .appName("SPARK Reasoning")
    .master("local[4]")
    .config("spark.eventLog.enabled", "true")
    .config("spark.hadoop.validateOutputSpecs", "false") //override output files
    .config("spark.serializer", "org.apache.spark.serializer.KryoSerializer")
    .config("spark.default.parallelism", "4")
    .config("spark.sql.shuffle.partitions", "4")
    .config(conf)
    .appName("RDD-vs-Dataframe")
    .getOrCreate()

  def main(args: Array[String]): Unit = {
    if (args.length < 2) {
      System.err.println("Usage: RDDVsDataframeExperiments <sourceFile> <targetDirectory>")
      System.exit(1)
    }

    val sourcePath = args(0)

//    profile{
//      val infGraphRDD = computeRDD(sourcePath)
//    }

    profile {
      val infGraphDataframe = computeDataframe(sourcePath)
//      infGraphDataframe.toDataFrame().explain()
    }

//    println("Dataframe-based: " + infGraphDataframe.size())
//    println("RDD-based: " + infGraphRDD.size())
//
//
//    val targetDir = args(1)
//
//    // write triples to disk
//    RDFGraphWriter.writeToFile(infGraphRDD, targetDir + "/rdd")
//    RDFGraphWriter.writeToFile(infGraphDataframe.toDataFrame(), targetDir + "/dataframe")

    session.stop()

  }

  def computeRDD(sourcePath: String): RDFGraph = {
    // load triples from disk
    val graph = RDFGraphLoader.loadFromFile(sourcePath, session.sparkContext, 4)

    // create reasoner
    val reasoner = new ForwardRuleReasonerRDFS(session.sparkContext)

    // compute inferred graph
    val inferredGraph = reasoner.apply(graph)

    inferredGraph
  }

  def computeDataframe(sourcePath: String): RDFGraphDataFrame = {
    // load triples from disk
    val graph = RDFGraphLoader.loadGraphDataFrameFromFile(sourcePath, session, 4)

    // create reasoner
    val reasoner = new ForwardRuleReasonerRDFSDataframe(session)

    // compute inferred graph
    val inferredGraph = reasoner.apply(graph)

    inferredGraph
  }
}
