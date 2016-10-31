package net.sansa_stack.ml.spark.dissect.inference.data

/**
  * The SQL schema used for RDF triples in a Dataframe.
  *
  * @author Lorenz Buehmann
  */
object SQLSchema {

  def triplesTable = "TRIPLES"

  def subjectCol = "subject"

  def predicateCol = "predicate"

  def objectCol = "object"

}
