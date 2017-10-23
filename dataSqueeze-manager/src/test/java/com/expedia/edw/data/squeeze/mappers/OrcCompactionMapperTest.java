package com.expedia.edw.data.squeeze.mappers;


import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileStatus;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.NullWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.lib.input.FileSplit;
import org.apache.orc.TypeDescription;
import org.apache.orc.mapred.OrcStruct;
import org.apache.orc.mapred.OrcValue;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Matchers;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.io.IOException;
import java.net.URI;

import static org.mockito.Matchers.any;
import static org.powermock.api.mockito.PowerMockito.mock;
import static org.powermock.api.mockito.PowerMockito.when;


/**
 * Tests for {@link OrcCompactionMapper}
 *
 * @author Yashraj R. Sontakke
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest({OrcCompactionMapper.class, OrcStruct.class, OrcValue.class, FileSystem.class})
public class OrcCompactionMapperTest {

    private final TypeDescription typeDescription = TypeDescription.createStruct().addField("field1", TypeDescription.createInt());
    private final IntWritable intWritable = new IntWritable(1);
    private final OrcStruct orcStruct = (OrcStruct) OrcStruct.createValue(typeDescription);
    private final TestMapperWrapper mapper = new TestMapperWrapper();
    private final Mapper.Context context = mock(Mapper.Context.class);
    private final Configuration configuration = mock(Configuration.class);
    private final FileSplit fileSplit = mock(FileSplit.class);
    private final FileSystem fileSystem = mock(FileSystem.class);
    private final FileStatus fileStatus = mock(FileStatus.class);

    @Before
    public void setup() throws IOException {
        PowerMockito.mockStatic(FileSystem.class);

        orcStruct.setFieldValue("field1", intWritable);
        when(context.getConfiguration()).thenReturn(configuration);
        when(configuration.get(Matchers.anyString())).thenReturn("0");
        when(context.getInputSplit()).thenReturn(fileSplit);
        final Path path = new Path("/source/path/");
        when(fileSplit.getPath()).thenReturn(path);
        when(FileSystem.get(any(URI.class), any(Configuration.class))).thenReturn(fileSystem);
        FileStatus[] fileStatuses = {fileStatus};
        when(fileSystem.listStatus(any(Path.class))).thenReturn(fileStatuses);
        when(fileStatus.isDirectory()).thenReturn(false);
        when(fileStatus.getLen()).thenReturn(1234L);
        when(fileStatus.getPath()).thenReturn(path);
    }

    @Test
    public void testMap() throws Exception {
        when(configuration.get(Matchers.anyString())).thenReturn("12345");
        mapper.map(NullWritable.get(), orcStruct, context);
        Mockito.verify(context, Mockito.times(1)).write(Mockito.eq(new Text("/source/")), Matchers.anyObject());
    }

    @Test
    public void testMapThreshold() throws Exception {
        when(configuration.get(Matchers.anyString())).thenReturn("0");
        mapper.map(NullWritable.get(), orcStruct, context);
        Mockito.verify(context, Mockito.times(1)).write(Mockito.eq(new Text("/source/path")), Matchers.anyObject());
    }


    public class TestMapperWrapper extends OrcCompactionMapper {

        protected void map(final NullWritable key, final OrcStruct value, final Context context) throws IOException, InterruptedException {
            setup(context);
            super.map(key, value, context);
        }
    }
}
