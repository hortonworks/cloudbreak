package com.sequenceiq.cloudbreak.cm.config;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.test.util.ReflectionTestUtils;

import com.google.common.collect.Sets;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.StackType;

@RunWith(MockitoJUnitRunner.class)
public class SMONMemoryWorkaroundServiceTest {

    @InjectMocks
    public SMONMemoryWorkaroundService underTest;

    @Before
    public void setUp() {
        // DL
        ReflectionTestUtils.setField(underTest, "datalakeNormalFirehoseHeapsize", 1);
        ReflectionTestUtils.setField(underTest, "datalakeExtensiveFirehoseHeapsize", 3);
        ReflectionTestUtils.setField(underTest, "datalakeNormalFirehoseNonJavaMemoryBytes", 1);
        ReflectionTestUtils.setField(underTest, "datalakeExtensiveFirehoseNonJavaMemoryBytes", 3);
        ReflectionTestUtils.setField(underTest, "datalakeMemoryExtensiveServices",
                Sets.newHashSet("REGIONSERVER"));

        // DH
        ReflectionTestUtils.setField(underTest, "datahubNormalFirehoseHeapsize", 2);
        ReflectionTestUtils.setField(underTest, "datahubExtensiveFirehoseHeapsize", 4);
        ReflectionTestUtils.setField(underTest, "datahubNormalFirehoseNonJavaMemoryBytes", 2);
        ReflectionTestUtils.setField(underTest, "datahubExtensiveFirehoseNonJavaMemoryBytes", 6);
        ReflectionTestUtils.setField(underTest, "datahubMemoryExtensiveServices",
                Sets.newHashSet("REGIONSERVER", "STREAMS_MESSAGING_MANAGER_SERVER", "KAFKA_BROKER", "KUDU_MASTER"));
    }

    @Test
    public void getFirehoseHeapsizeWhenDLAndNoRegionServerInTheBlueprintShouldReturnLowMemoryforDL() {
        Assert.assertEquals("1073741824", underTest.firehoseHeapsize(StackType.DATALAKE,
                Sets.newHashSet()));
    }

    @Test
    public void getFirehoseHeapsizeWhenDLAndRegionServerInTheBlueprintShouldReturnHighMemoryforDL() {
        Assert.assertEquals("3221225472", underTest.firehoseHeapsize(StackType.DATALAKE,
                Sets.newHashSet("REGIONSERVER")));
    }

    @Test
    public void getFirehoseHeapsizeWhenDHAndNoRegionServerInTheBlueprintShouldReturnLowMemoryforDH() {
        Assert.assertEquals("2147483648", underTest.firehoseHeapsize(StackType.WORKLOAD,
                Sets.newHashSet()));
    }

    @Test
    public void getFirehoseHeapsizeWhenDHAndRegionServerInTheBlueprintShouldReturnHighMemoryforDH() {
        Assert.assertEquals("6442450944", underTest.firehoseNonJavaMemoryBytes(StackType.WORKLOAD,
                Sets.newHashSet("REGIONSERVER")));
    }

    @Test
    public void getFirehoseNonJavaMemoryBytesWhenDLAndNoRegionServerInTheBlueprintShouldReturnLowMemoryforDL() {
        Assert.assertEquals("1073741824", underTest.firehoseNonJavaMemoryBytes(StackType.DATALAKE,
                Sets.newHashSet()));
    }

    @Test
    public void getFirehoseNonJavaMemoryBytesWhenDLAndRegionServerInTheBlueprintShouldReturnHighMemoryforDL() {
        Assert.assertEquals("3221225472", underTest.firehoseNonJavaMemoryBytes(StackType.DATALAKE,
                Sets.newHashSet("REGIONSERVER")));
    }

    @Test
    public void getFirehoseNonJavaMemoryBytesWhenDHAndNoRegionServerInTheBlueprintShouldReturnLowMemoryforDH() {
        Assert.assertEquals("2147483648", underTest.firehoseNonJavaMemoryBytes(StackType.WORKLOAD,
                Sets.newHashSet()));
    }

    @Test
    public void getFirehoseNonJavaMemoryBytesWhenDHAndRegionServerInTheBlueprintShouldReturnHighMemoryforDH() {
        Assert.assertEquals("6442450944", underTest.firehoseNonJavaMemoryBytes(StackType.WORKLOAD,
                Sets.newHashSet("REGIONSERVER")));
    }

}