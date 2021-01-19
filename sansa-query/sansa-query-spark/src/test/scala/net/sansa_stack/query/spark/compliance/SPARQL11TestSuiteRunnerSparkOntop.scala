package net.sansa_stack.query.spark.compliance

import java.util.Properties

import scala.collection.JavaConverters._

import org.apache.jena.graph.Triple
import org.apache.jena.query._
import org.apache.jena.rdf.model.Model
import org.apache.jena.sparql.resultset.SPARQLResult
import org.apache.spark.rdd.RDD
import org.scalatest.tags.Slow
import org.scalatest.{ConfigMap, DoNotDiscover}

import net.sansa_stack.query.spark.api.domain.QueryExecutionFactorySpark
import net.sansa_stack.query.spark.ontop.QueryEngineFactoryOntop


/**
 * SPARQL 1.1 test suite runner for Ontop-based SPARQL-to-SQL implementation on Apache Spark.
 *
 *
 * @author Lorenz Buehmann
 */
@DoNotDiscover
@Slow
class SPARQL11TestSuiteRunnerSparkOntop
  extends SPARQL11TestSuiteRunnerSpark {

  override lazy val IGNORE = Set(/* AGGREGATES */
    aggregatesManifest + "agg-err-02", /* BINDINGS */
    // TODO: fix it (UNDEF involves the notion of COMPATIBILITY when joining)
    bindingsManifest + "values8", bindingsManifest + "values5", /* FUNCTIONS */
    // bnode not supported in SPARQL transformation
    functionsManifest + "bnode01", functionsManifest + "bnode02", // the SI does not preserve the original timezone
//    functionsManifest + "hours", functionsManifest + "day", // not supported in SPARQL transformation
    functionsManifest + "if01", functionsManifest + "if02",
    functionsManifest + "in01", functionsManifest + "in02",
    functionsManifest + "iri01", // not supported in H2 transformation
    functionsManifest + "md5-01", functionsManifest + "md5-02", // The SI does not support IRIs as ORDER BY conditions
    functionsManifest + "plus-1", functionsManifest + "plus-2", // SHA1 is not supported in H2
    functionsManifest + "sha1-01", functionsManifest + "sha1-02", // SHA512 is not supported in H2
    functionsManifest + "sha512-01", functionsManifest + "sha512-02",
    functionsManifest + "strdt01", functionsManifest + "strdt02", functionsManifest + "strdt03",
    functionsManifest + "strlang01", functionsManifest + "strlang02", functionsManifest + "strlang03",
//    functionsManifest + "timezone", // TZ is not supported in H2
//    functionsManifest + "tz",
    /* CONSTRUCT not supported yet */
    // Projection cannot be cast to Reduced in rdf4j
    constructManifest + "constructwhere01", constructManifest + "constructwhere02", constructManifest + "constructwhere03", // problem importing dataset
    constructManifest + "constructwhere04", /* CSV */
    // Sorting by IRI is not supported by the SI
    csvTscResManifest + "tsv01", csvTscResManifest + "tsv02", // different format for number and not supported custom datatype
    csvTscResManifest + "tsv03",
    /* GROUPING */
    // Multi-typed COALESCE as grouping condition TODO: support it
    groupingManifest + "group04",
    /* NEGATION not supported yet */
    negationManifest + "subset-by-exclusion-nex-1", negationManifest + "temporal-proximity-by-exclusion-nex-1", negationManifest + "subset-01", negationManifest + "subset-02", negationManifest + "set-equals-1", negationManifest + "subset-03", negationManifest + "exists-01", negationManifest + "exists-02", // DISABLED DUE TO ORDER OVER IRI
    negationManifest + "full-minuend", negationManifest + "partial-minuend", // TODO: enable it
    negationManifest + "full-minuend-modified", negationManifest + "partial-minuend-modified",
    /* EXISTS not supported yet */
    existsManifest + "exists01", existsManifest + "exists02", existsManifest + "exists03", existsManifest + "exists04", existsManifest + "exists05",
    /* PROPERTY PATH */
    // Not supported: ArbitraryLengthPath
    propertyPathManifest + "pp02", // wrong result, unexpected binding
    propertyPathManifest + "pp06", propertyPathManifest + "pp12", propertyPathManifest + "pp14", propertyPathManifest + "pp16", propertyPathManifest + "pp21", propertyPathManifest + "pp23", propertyPathManifest + "pp25", // Not supported: ZeroLengthPath
    propertyPathManifest + "pp28a", propertyPathManifest + "pp34", propertyPathManifest + "pp35", propertyPathManifest + "pp36", propertyPathManifest + "pp37",
    /* SERVICE not supported yet */
    serviceManifest + "service1", // no loading of the dataset
    serviceManifest + "service2", serviceManifest + "service3", serviceManifest + "service4a", serviceManifest + "service5", serviceManifest + "service6", serviceManifest + "service7",
    /* SUBQUERY */
    // Quad translated as a triple. TODO: fix it
    subqueryManifest + "subquery02", subqueryManifest + "subquery04", // EXISTS is not supported yet
    subqueryManifest + "subquery10", // ORDER BY IRI (for supported by the SI)
    subqueryManifest + "subquery11", // unbound variable: Var TODO: fix it
    subqueryManifest + "subquery12", subqueryManifest + "subquery13", // missing results (TODO: fix)
    subqueryManifest + "subquery14"
  )


//  override lazy val IGNORE_FILTER = t => t.name.startsWith("CONTAINS") || t.name.startsWith("UCASE")
//override lazy val IGNORE_FILTER = t => t.dataFile.contains("/functions")  && t.name.startsWith("isNu")
  override lazy val IGNORE_FILTER = t => t.dataFile.contains("aggregates")// && !Seq("MIN", "MAX", "COUNT", "GROUP_CONCAT").exists(t.name.contains(_)) // && !t.name.startsWith("GROUP_CONCAT")

  var engineFactory: QueryEngineFactoryOntop = _

  override def beforeAll(configMap: ConfigMap): Unit = {
    super.beforeAll(configMap)
    engineFactory = new QueryEngineFactoryOntop(spark)
  }

  val db = "TEST"

  var previousModel: Model = _
  var triplesRDD: RDD[Triple] = _
  var qef: QueryExecutionFactorySpark = _

  override def runQuery(query: Query, data: Model): SPARQLResult = {
    // do some caching here to avoid reloading the same data
    if (data != previousModel) {
      // we drop the Spark database to remove all tables
      spark.sql(s"DROP DATABASE IF EXISTS $db")

      // distribute on Spark
      triplesRDD = spark.sparkContext.parallelize(data.getGraph.find().toList.asScala)

      // we create a Spark database here to keep the implicit partitioning separate

      spark.sql(s"CREATE DATABASE IF NOT EXISTS $db")
      spark.sql(s"USE $db")

      qef = engineFactory.create(triplesRDD)

      previousModel = data
    }

    val qe = qef.createQueryExecution(query)

    // produce result based on query type
    val result = if (query.isSelectType) { // SELECT
      val rs = qe.execSelect()
      new SPARQLResult(rs)
    } else if (query.isAskType) { // ASK
      val b = qe.execAsk()
      new SPARQLResult(b)
    } else if (query.isConstructType) { // CONSTRUCT
      val triples = qe.execConstruct()
      new SPARQLResult(triples)
    } else { // DESCRIBE todo
      fail("unsupported query type: DESCRIBE")
      null
    }
    // clean up

    qe.close()

    result
  }

}

import org.scalatest.Tag

object ExperimentalTest extends Tag("net.sansa_stack.tests.ExperimentalTest")
