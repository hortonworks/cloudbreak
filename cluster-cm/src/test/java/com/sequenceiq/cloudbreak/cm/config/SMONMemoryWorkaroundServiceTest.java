package com.sequenceiq.cloudbreak.cm.config;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.google.common.collect.Sets;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.StackType;

@ExtendWith(MockitoExtension.class)
class SMONMemoryWorkaroundServiceTest {

    @InjectMocks
    public SMONMemoryWorkaroundService underTest;

    @BeforeEach
    void setUp() {
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
    void getFirehoseHeapsizeWhenDLAndNoRegionServerInTheBlueprintShouldReturnLowMemoryforDL() {
        assertEquals("1073741824", underTest.firehoseHeapsize(StackType.DATALAKE,
                Sets.newHashSet()));
    }

    @Test
    void getFirehoseHeapsizeWhenDLAndRegionServerInTheBlueprintShouldReturnHighMemoryforDL() {
        assertEquals("3221225472", underTest.firehoseHeapsize(StackType.DATALAKE,
                Sets.newHashSet("REGIONSERVER")));
    }

    @Test
    void getFirehoseHeapsizeWhenDHAndNoRegionServerInTheBlueprintShouldReturnLowMemoryforDH() {
        assertEquals("2147483648", underTest.firehoseHeapsize(StackType.WORKLOAD,
                Sets.newHashSet()));
    }

    @Test
    void getFirehoseHeapsizeWhenDHSMALLAndRegionServerInTheBlueprintShouldReturnHighMemoryforDH() {
        assertEquals("4294967296", underTest.firehoseHeapsize(StackType.WORKLOAD,
                Sets.newHashSet("REGIONSERVER")));
    }

    @Test
    void getFirehoseNonJavaMemoryBytesWhenDLAndNoRegionServerInTheBlueprintShouldReturnLowMemoryforDL() {
        assertEquals("1073741824", underTest.firehoseNonJavaMemoryBytes(StackType.DATALAKE,
                Sets.newHashSet(), 3));
    }

    @Test
    void getFirehoseNonJavaMemoryBytesWhenDLSMALLAndRegionServerInTheBlueprintShouldReturnHighMemoryforDL() {
        assertEquals("3221225472", underTest.firehoseNonJavaMemoryBytes(StackType.DATALAKE,
                Sets.newHashSet("REGIONSERVER"), 3));
    }

    @Test
    void getFirehoseNonJavaMemoryBytesWhenDHSMALLAndNoRegionServerInTheBlueprintShouldReturnLowMemoryforDH() {
        assertEquals("2147483648", underTest.firehoseNonJavaMemoryBytes(StackType.WORKLOAD,
                Sets.newHashSet(), 1));
    }

    @Test
    void getFirehoseNonJavaMemoryBytesWhenDHMEDIUMAndNoRegionServerInTheBlueprintShouldReturnLowMemoryforDH() {
        assertEquals("2147483648", underTest.firehoseNonJavaMemoryBytes(StackType.WORKLOAD,
                Sets.newHashSet(), 99));
    }

    @Test
    void getFirehoseNonJavaMemoryBytesWhenDHLARGEAndNoRegionServerInTheBlueprintShouldReturnLowMemoryforDH() {
        assertEquals("7516192768", underTest.firehoseNonJavaMemoryBytes(StackType.WORKLOAD,
                Sets.newHashSet(), 499));
    }

    @Test
    void getFirehoseNonJavaMemoryBytesWhenDHGIGAAndNoRegionServerInTheBlueprintShouldReturnLowMemoryforDH() {
        assertEquals("11811160064", underTest.firehoseNonJavaMemoryBytes(StackType.WORKLOAD,
                Sets.newHashSet(), 1000));
    }

    @Test
    void getFirehoseNonJavaMemoryBytesWhenDHSMALLAndRegionServerInTheBlueprintShouldReturnHighMemoryforDH() {
        assertEquals("6442450944", underTest.firehoseNonJavaMemoryBytes(StackType.WORKLOAD,
                Sets.newHashSet("REGIONSERVER"), 1));
    }

    @Test
    void getFirehoseNonJavaMemoryBytesWhenDHMEDIUMAndRegionServerInTheBlueprintShouldReturnHighMemoryforDH() {
        assertEquals("6442450944", underTest.firehoseNonJavaMemoryBytes(StackType.WORKLOAD,
                Sets.newHashSet("REGIONSERVER"), 99));
    }

    @Test
    void getFirehoseNonJavaMemoryBytesWhenDHLARGEAndRegionServerInTheBlueprintShouldReturnHighMemoryforDH() {
        assertEquals("7516192768", underTest.firehoseNonJavaMemoryBytes(StackType.WORKLOAD,
                Sets.newHashSet("REGIONSERVER"), 499));
    }

    @Test
    void getFirehoseNonJavaMemoryBytesWhenDHGIGAAndRegionServerInTheBlueprintShouldReturnHighMemoryforDH() {
        assertEquals("11811160064", underTest.firehoseNonJavaMemoryBytes(StackType.WORKLOAD,
                Sets.newHashSet("REGIONSERVER"), 1000));
    }

}