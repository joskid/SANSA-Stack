package net.sansa_stack.query.spark.sparqlify

import java.util.Collections

import scala.collection.JavaConverters._

import net.sansa_stack.rdf.spark.utils.SchemaUtils
import org.aksw.sparqlify.config.v0_2.bridge.BasicTableInfo
import org.aksw.sparqlify.config.v0_2.bridge.BasicTableInfoProvider
import org.apache.spark.sql.SparkSession

class BasicTableInfoProviderSpark(val sparkSession: SparkSession)
    extends BasicTableInfoProvider {
  def getBasicTableInfo(queryStr: String): BasicTableInfo = {
    val dataFrame = sparkSession.sql(queryStr)
    val flatSchema = SchemaUtils.flattenSchema(dataFrame.schema)

    // TODO Handle nullable columns

    val rawTypeMap = flatSchema.asJava
    val nullableColumns = Collections.emptySet[String] // Set[String]().asJava

    new BasicTableInfo(rawTypeMap, nullableColumns)
  }
}
