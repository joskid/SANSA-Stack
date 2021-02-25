package net.sansa_stack.rdf.common.io.hadoop.rdf.trig;

import net.sansa_stack.rdf.common.io.hadoop.rdf.base.FileInputFormatRdfBase;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.mapreduce.InputSplit;
import org.apache.hadoop.mapreduce.RecordReader;
import org.apache.hadoop.mapreduce.TaskAttemptContext;
import org.apache.jena.riot.Lang;
import org.apache.jena.sparql.core.Quad;

public class FileInputFormatTrigQuad
        extends FileInputFormatRdfBase<Quad> {
    public FileInputFormatTrigQuad() {
        super(Lang.TRIG);
    }

    @Override
    public RecordReader<LongWritable, Quad> createRecordReaderActual(InputSplit inputSplit, TaskAttemptContext context) {
        return new RecordReaderTrigQuad();
    }
}