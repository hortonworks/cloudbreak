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
        ReflectionTestUtils.setField(underTest, "smonSmallClusterMaxSize", 10);
        ReflectionTestUtils.setField(underTest, "smonMediumClusterMaxSize", 100);
        ReflectionTestUtils.setField(underTest, "smonLargeClusterMaxSize", 500);

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
    public void getFirehoseHeapsizeWhenDHSMALLAndRegionServerInTheBlueprintShouldReturnHighMemoryforDH() {
        Assert.assertEquals("4294967296", underTest.firehoseHeapsize(StackType.WORKLOAD,
                Sets.newHashSet("REGIONSERVER")));
    }

    @Test
    public void getFirehoseNonJavaMemoryBytesWhenDLAndNoRegionServerInTheBlueprintShouldReturnLowMemoryforDL() {
        Assert.assertEquals("1073741824", underTest.firehoseNonJavaMemoryBytes(StackType.DATALAKE,
                Sets.newHashSet(), 3));
    }

    @Test
    public void getFirehoseNonJavaMemoryBytesWhenDLSMALLAndRegionServerInTheBlueprintShouldReturnHighMemoryforDL() {
        Assert.assertEquals("3221225472", underTest.firehoseNonJavaMemoryBytes(StackType.DATALAKE,
                Sets.newHashSet("REGIONSERVER"), 3));
    }

    @Test
    public void getFirehoseNonJavaMemoryBytesWhenDHSMALLAndNoRegionServerInTheBlueprintShouldReturnLowMemoryforDH() {
        Assert.assertEquals("2147483648", underTest.firehoseNonJavaMemoryBytes(StackType.WORKLOAD,
                Sets.newHashSet(), 1));
    }

    @Test
    public void getFirehoseNonJavaMemoryBytesWhenDHMEDIUMAndNoRegionServerInTheBlueprintShouldReturnLowMemoryforDH() {
        Assert.assertEquals("2147483648", underTest.firehoseNonJavaMemoryBytes(StackType.WORKLOAD,
                Sets.newHashSet(), 99));
    }

    @Test
    public void getFirehoseNonJavaMemoryBytesWhenDHLARGEAndNoRegionServerInTheBlueprintShouldReturnLowMemoryforDH() {
        Assert.assertEquals("7516192768", underTest.firehoseNonJavaMemoryBytes(StackType.WORKLOAD,
                Sets.newHashSet(), 499));
    }

    @Test
    public void getFirehoseNonJavaMemoryBytesWhenDHGIGAAndNoRegionServerInTheBlueprintShouldReturnLowMemoryforDH() {
        Assert.assertEquals("11811160064", underTest.firehoseNonJavaMemoryBytes(StackType.WORKLOAD,
                Sets.newHashSet(), 1000));
    }

    @Test
    public void getFirehoseNonJavaMemoryBytesWhenDHSMALLAndRegionServerInTheBlueprintShouldReturnHighMemoryforDH() {
        Assert.assertEquals("6442450944", underTest.firehoseNonJavaMemoryBytes(StackType.WORKLOAD,
                Sets.newHashSet("REGIONSERVER"), 1));
    }

    @Test
    public void getFirehoseNonJavaMemoryBytesWhenDHMEDIUMAndRegionServerInTheBlueprintShouldReturnHighMemoryforDH() {
        Assert.assertEquals("6442450944", underTest.firehoseNonJavaMemoryBytes(StackType.WORKLOAD,
                Sets.newHashSet("REGIONSERVER"), 99));
    }

    @Test
    public void getFirehoseNonJavaMemoryBytesWhenDHLARGEAndRegionServerInTheBlueprintShouldReturnHighMemoryforDH() {
        Assert.assertEquals("7516192768", underTest.firehoseNonJavaMemoryBytes(StackType.WORKLOAD,
                Sets.newHashSet("REGIONSERVER"), 499));
    }

    @Test
    public void getFirehoseNonJavaMemoryBytesWhenDHGIGAAndRegionServerInTheBlueprintShouldReturnHighMemoryforDH() {
        Assert.assertEquals("11811160064", underTest.firehoseNonJavaMemoryBytes(StackType.WORKLOAD,
                Sets.newHashSet("REGIONSERVER"), 1000));
    }

}