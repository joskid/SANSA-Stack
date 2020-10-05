package net.sansa_stack.rdf.spark.io.turtle

import java.io.ByteArrayInputStream

import net.sansa_stack.rdf.common.annotation.Experimental
import net.sansa_stack.rdf.spark.io._
import org.apache.hadoop.io.{LongWritable, Text}
import org.apache.hadoop.mapreduce.lib.input.TextInputFormat
import org.apache.jena.riot.{Lang, RDFDataMgr}
import org.apache.spark.rdd.RDD
import org.apache.spark.sql.{Row, SQLContext}
import org.apache.spark.sql.sources.{BaseRelation, PrunedScan, TableScan}
import org.apache.spark.sql.types.{StringType, StructField, StructType}
import scala.util.{Failure, Success, Try}

import org.apache.jena.graph.Node


/**
 * A custom relation that represents RDF triples loaded from files in Turtle syntax.
 *
 * @param location
 * @param userSchema
 * @param sqlContext
 */
@Experimental
private[turtle] class TurtleRelation(location: String, userSchema: StructType)
                                    (@transient val sqlContext: SQLContext)
  extends BaseRelation
    with TableScan
    with PrunedScan
    with Serializable {

  override def schema: StructType = {
    if (this.userSchema != null) {
      this.userSchema
    }
    else {
      StructType(
        Seq(
          StructField("s", StringType, true),
          StructField("p", StringType, true),
          StructField("o", StringType, true)
        ))
    }
  }

  override def buildScan(): RDD[Row] = {
    // 1. parse the Turtle file into an RDD[Triple]
    val turtleRDD = sqlContext.sparkSession.rdf(Lang.TURTLE)(location)

    // map to Row
    val rows = turtleRDD.map(toRow)

    rows
  }

  override def buildScan(requiredColumns: Array[String]): RDD[Row] = {
    // parse the Turtle file into an RDD[Triple]
    val turtleRDD = sqlContext.sparkSession.rdf(Lang.TURTLE)(location)

    // map to Row
    val rows = turtleRDD.map { t =>
      val nodes = for (col <- requiredColumns) yield {
          col match {
            case "s" => t.getSubject
            case "p" => t.getPredicate
            case "o" => t.getObject
            case other => throw new RuntimeException(s"unsupported column name '$other''")
          }
      }

      toRow(nodes)
//
//      requiredColumns.foreach { col =>
//        val n = col match {
//          case "s" => t.getSubject
//          case "p" => t.getPredicate
//          case "o" => t.getObject
//          case other  => throw new RuntimeException(s"unsupported column name '$other''")
//        }
//
//      }

    }

    rows
  }


  def cleanly[A, B](resource: A)(cleanup: A => Unit)(doWork: A => B): Try[B] = {
    try {
      Success(doWork(resource))
    } catch {
      case e: Exception => Failure(e)
    }
    finally {
      try {
        if (resource != null) {
          cleanup(resource)
        }
      } catch {
        case e: Exception => println(e) // should be logged
      }
    }
  }
}
