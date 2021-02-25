package net.sansa_stack.rdf.common.io.hadoop.rdf.trig;

import net.sansa_stack.rdf.common.io.hadoop.rdf.base.FileInputFormatRdfBase;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.mapreduce.InputSplit;
import org.apache.hadoop.mapreduce.RecordReader;
import org.apache.hadoop.mapreduce.TaskAttemptContext;
import org.apache.jena.query.Dataset;
import org.apache.jena.riot.Lang;

public class FileInputFormatTrigDataset
    extends FileInputFormatRdfBase<Dataset>
{
    public FileInputFormatTrigDataset() {
        super(Lang.TRIG);
    }

    @Override
    public RecordReader<LongWritable, Dataset> createRecordReaderActual(InputSplit inputSplit, TaskAttemptContext context) {
        return new RecordReaderTrigDataset();
    }
}