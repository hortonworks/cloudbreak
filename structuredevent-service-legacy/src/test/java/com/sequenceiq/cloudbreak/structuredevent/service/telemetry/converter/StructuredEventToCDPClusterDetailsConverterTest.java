package com.sequenceiq.cloudbreak.structuredevent.service.telemetry.converter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import com.cloudera.thunderhead.service.common.usage.UsageProto.CDPCloudProviderVariantType.Value;
import com.cloudera.thunderhead.service.common.usage.UsageProto.CDPClusterDetails;
import com.cloudera.thunderhead.service.common.usage.UsageProto.CDPClusterShape;
import com.cloudera.thunderhead.service.common.usage.UsageProto.CDPImageDetails;
import com.cloudera.thunderhead.service.common.usage.UsageProto.CDPVersionDetails;
import com.sequenceiq.cloudbreak.cloud.model.StackTags;
import com.sequenceiq.cloudbreak.common.json.Json;
import com.sequenceiq.cloudbreak.structuredevent.event.ClusterDetails;
import com.sequenceiq.cloudbreak.structuredevent.event.DatabaseDetails;
import com.sequenceiq.cloudbreak.structuredevent.event.StackDetails;
import com.sequenceiq.cloudbreak.structuredevent.event.StructuredFlowEvent;
import com.sequenceiq.cloudbreak.structuredevent.event.StructuredSyncEvent;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class StructuredEventToCDPClusterDetailsConverterTest {

    @InjectMocks
    private StructuredEventToCDPClusterDetailsConverter underTest;

    @Mock
    private StructuredEventToCDPClusterShapeConverter clusterShapeConverter;

    @Mock
    private StructuredEventToCDPImageDetailsConverter imageDetailsConverter;

    @Mock
    private StructuredEventToCDPVersionDetailsConverter versionDetailsConverter;

    @Mock
    private MultiAzConverter multiAzConverter;

    @BeforeEach
    public void setUp() {
        doReturn(CDPClusterShape.newBuilder().build()).when(clusterShapeConverter).convert((StructuredFlowEvent) any());
        doReturn(CDPClusterShape.newBuilder().build()).when(clusterShapeConverter).convert((StructuredSyncEvent) any());
        doReturn(CDPImageDetails.newBuilder().build()).when(imageDetailsConverter).convert((StructuredFlowEvent) any());
        doReturn(CDPImageDetails.newBuilder().build()).when(imageDetailsConverter).convert((StructuredSyncEvent) any());
        doReturn(CDPVersionDetails.newBuilder().build()).when(versionDetailsConverter).convert((StructuredFlowEvent) any());
        doReturn(CDPVersionDetails.newBuilder().build()).when(versionDetailsConverter).convert((StructuredSyncEvent) any());
    }

    @Test
    public void testConvertWithNull() {
        assertNotNull(underTest.convert((StructuredFlowEvent) null), "We should return empty object for not null");
        assertNotNull(underTest.convert((StructuredSyncEvent) null), "We should return empty object for not null");
    }

    @Test
    public void testUserTagsConversionWithNullStackDetails() {
        StructuredFlowEvent structuredFlowEvent = new StructuredFlowEvent();
        structuredFlowEvent.setStack(null);
        CDPClusterDetails clusterDetails = underTest.convert(structuredFlowEvent);

        assertEquals("", clusterDetails.getUserTags());

        StructuredSyncEvent structuredSyncEvent = new StructuredSyncEvent();
        structuredSyncEvent.setStack(null);
        clusterDetails = underTest.convert(structuredSyncEvent);

        assertEquals("", clusterDetails.getUserTags());
    }

    @Test
    public void testUserTagsConversionWithNullTags() {
        StackDetails stackDetails = new StackDetails();
        stackDetails.setTags(null);

        StructuredFlowEvent structuredFlowEvent = new StructuredFlowEvent();
        structuredFlowEvent.setStack(stackDetails);
        CDPClusterDetails clusterDetails = underTest.convert(structuredFlowEvent);

        assertEquals("", clusterDetails.getUserTags());

        StructuredSyncEvent structuredSyncEvent = new StructuredSyncEvent();
        structuredSyncEvent.setStack(stackDetails);
        clusterDetails = underTest.convert(structuredSyncEvent);

        assertEquals("", clusterDetails.getUserTags());
    }

    @Test
    public void testVariantConversionWithNullVariant() {
        StackDetails stackDetails = new StackDetails();
        stackDetails.setPlatformVariant(null);

        StructuredFlowEvent structuredFlowEvent = new StructuredFlowEvent();
        structuredFlowEvent.setStack(stackDetails);
        CDPClusterDetails clusterDetails = underTest.convert(structuredFlowEvent);

        assertEquals(Value.UNSET, clusterDetails.getCloudProviderVariant());

        StructuredSyncEvent structuredSyncEvent = new StructuredSyncEvent();
        structuredSyncEvent.setStack(stackDetails);
        clusterDetails = underTest.convert(structuredSyncEvent);

        assertEquals(0, clusterDetails.getCloudProviderVariantValue());
    }

    @Test
    public void testVariantConversionWithNotNullVariant() {
        StackDetails stackDetails = new StackDetails();
        stackDetails.setPlatformVariant("AWS_NATIVE");
        stackDetails.setMultiAz(true);

        StructuredFlowEvent structuredFlowEvent = new StructuredFlowEvent();
        structuredFlowEvent.setStack(stackDetails);
        CDPClusterDetails clusterDetails = underTest.convert(structuredFlowEvent);

        assertEquals(Value.AWS_NATIVE, clusterDetails.getCloudProviderVariant());

        StructuredSyncEvent structuredSyncEvent = new StructuredSyncEvent();
        structuredSyncEvent.setStack(stackDetails);
        clusterDetails = underTest.convert(structuredSyncEvent);

        assertEquals(2, clusterDetails.getCloudProviderVariantValue());
        assertEquals(true, clusterDetails.getMultiAz());
    }

    @Test
    public void testUserTagsConversionWithEmptyTags() {
        StackDetails stackDetails = new StackDetails();
        stackDetails.setTags(new Json(""));

        StructuredFlowEvent structuredFlowEvent = new StructuredFlowEvent();
        structuredFlowEvent.setStack(stackDetails);
        CDPClusterDetails clusterDetails = underTest.convert(structuredFlowEvent);

        assertEquals("", clusterDetails.getUserTags());
        assertEquals("", clusterDetails.getApplicationTags());

        StructuredSyncEvent structuredSyncEvent = new StructuredSyncEvent();
        structuredSyncEvent.setStack(stackDetails);
        clusterDetails = underTest.convert(structuredSyncEvent);

        assertEquals("", clusterDetails.getUserTags());
        assertEquals("", clusterDetails.getApplicationTags());
    }

    @Test
    public void testUserTagsConversionWithCorrectTags() {
        StackDetails stackDetails = new StackDetails();
        Map<String, String> userTags = new HashMap<>();
        userTags.put("key1", "value1");
        userTags.put("key2", "value2");
        Map<String, String> appTags = new HashMap<>();
        appTags.put("appKey1", "appValue1");
        appTags.put("appKey2", "appValue2");
        stackDetails.setTags(new Json(new StackTags(userTags, appTags, new HashMap<>())));

        StructuredFlowEvent structuredFlowEvent = new StructuredFlowEvent();
        structuredFlowEvent.setStack(stackDetails);
        CDPClusterDetails clusterDetails = underTest.convert(structuredFlowEvent);

        assertEquals("{\"key1\":\"value1\",\"key2\":\"value2\"}", clusterDetails.getUserTags());
        assertEquals("{\"appKey1\":\"appValue1\",\"appKey2\":\"appValue2\"}", clusterDetails.getApplicationTags());

        StructuredSyncEvent structuredSyncEvent = new StructuredSyncEvent();
        structuredSyncEvent.setStack(stackDetails);
        clusterDetails = underTest.convert(structuredSyncEvent);

        assertEquals("{\"key1\":\"value1\",\"key2\":\"value2\"}", clusterDetails.getUserTags());
        assertEquals("{\"appKey1\":\"appValue1\",\"appKey2\":\"appValue2\"}", clusterDetails.getApplicationTags());
    }

    @Test
    void testExternalDbAndSslEnablementWhenClusterIsNull() {
        CDPClusterDetails result = underTest.convert(new StructuredFlowEvent());

        assertFalse(result.getSslEnabled());
        assertFalse(result.getUsingExternalDatabase());
    }

    @Test
    @DisplayName("Test when ClusterDetails exists but the DbSslEnabled field is false then the result CDPClusterDetails should also contain false, for " +
            "the designated field")
    void testWhenClusterIsNotNullAndDbSslEnabledIsFalse() {
        StructuredFlowEvent event = new StructuredFlowEvent();
        ClusterDetails clusterDetails = new ClusterDetails();
        clusterDetails.setDbSslEnabled(false);
        event.setCluster(clusterDetails);
        CDPClusterDetails result = underTest.convert(event);

        assertFalse(result.getSslEnabled());
    }

    @Test
    @DisplayName("Test when ClusterDetails exists and the DbSslEnabled field is true then the result CDPClusterDetails should also contain true, for " +
            "the designated field")
    void testWhenClusterIsNotNullAndDbSslEnabledIsTrue() {
        StructuredFlowEvent event = new StructuredFlowEvent();
        ClusterDetails clusterDetails = new ClusterDetails();
        clusterDetails.setDbSslEnabled(true);
        event.setCluster(clusterDetails);
        CDPClusterDetails result = underTest.convert(event);

        assertTrue(result.getSslEnabled());
    }

    @Test
    @DisplayName("Test when ClusterDetails exists but the ExternalDatabase field is false then the result CDPClusterDetails should also contain false, for " +
            "the designated field")
    void testWhenClusterIsNotNullAndExternalDatabaseIsFalse() {
        StructuredFlowEvent event = new StructuredFlowEvent();
        ClusterDetails clusterDetails = new ClusterDetails();
        clusterDetails.setExternalDatabase(false);
        event.setCluster(clusterDetails);
        CDPClusterDetails result = underTest.convert(event);

        assertFalse(result.getUsingExternalDatabase());
    }

    @Test
    @DisplayName("Test when ClusterDetails exists and the ExternalDatabase field is true then the result CDPClusterDetails should also contain true, for " +
            "the designated field")
    void testWhenClusterIsNotNullAndExternalDatabaseIsTrue() {
        StructuredFlowEvent event = new StructuredFlowEvent();
        ClusterDetails clusterDetails = new ClusterDetails();
        clusterDetails.setExternalDatabase(true);
        event.setCluster(clusterDetails);
        CDPClusterDetails result = underTest.convert(event);

        assertTrue(result.getUsingExternalDatabase());
    }

    @Test
    void testDatabaseDetailsConversionFlow() {
        StackDetails stackDetails = new StackDetails();
        DatabaseDetails databaseDetails = new DatabaseDetails();
        databaseDetails.setAvailabilityType("HA");
        databaseDetails.setEngineVersion("14");
        databaseDetails.setAttributes("attr");
        stackDetails.setDatabaseDetails(databaseDetails);
        StructuredFlowEvent structuredFlowEvent = new StructuredFlowEvent();
        structuredFlowEvent.setStack(stackDetails);

        CDPClusterDetails result = underTest.convert(structuredFlowEvent);

        assertEquals(databaseDetails.getAttributes(), result.getDatabaseDetails().getAttributes());
        assertEquals(databaseDetails.getEngineVersion(), result.getDatabaseDetails().getEngineVersion());
        assertEquals(databaseDetails.getAvailabilityType(), result.getDatabaseDetails().getAvailabilityType());
    }

    @Test
    void testDatabaseDetailsConversionFlowWithEmptyDatabaseDetails() {
        StackDetails stackDetails = new StackDetails();
        DatabaseDetails databaseDetails = new DatabaseDetails();
        stackDetails.setDatabaseDetails(databaseDetails);
        StructuredFlowEvent structuredFlowEvent = new StructuredFlowEvent();
        structuredFlowEvent.setStack(stackDetails);

        CDPClusterDetails result = underTest.convert(structuredFlowEvent);

        assertEquals("", result.getDatabaseDetails().getAttributes());
        assertEquals("", result.getDatabaseDetails().getEngineVersion());
        assertEquals("", result.getDatabaseDetails().getAvailabilityType());
    }

    @Test
    void testDatabaseDetailsConversionSync() {
        StackDetails stackDetails = new StackDetails();
        DatabaseDetails databaseDetails = new DatabaseDetails();
        databaseDetails.setAvailabilityType("HA");
        databaseDetails.setEngineVersion("14");
        databaseDetails.setAttributes("attr");
        stackDetails.setDatabaseDetails(databaseDetails);
        StructuredSyncEvent structuredSyncEvent = new StructuredSyncEvent();
        structuredSyncEvent.setStack(stackDetails);

        CDPClusterDetails result = underTest.convert(structuredSyncEvent);

        assertEquals(databaseDetails.getAttributes(), result.getDatabaseDetails().getAttributes());
        assertEquals(databaseDetails.getEngineVersion(), result.getDatabaseDetails().getEngineVersion());
        assertEquals(databaseDetails.getAvailabilityType(), result.getDatabaseDetails().getAvailabilityType());
    }

    @Test
    void testDatabaseDetailsConversionSyncWithEmptyDatabaseDetails() {
        StackDetails stackDetails = new StackDetails();
        DatabaseDetails databaseDetails = new DatabaseDetails();
        stackDetails.setDatabaseDetails(databaseDetails);
        StructuredSyncEvent structuredSyncEvent = new StructuredSyncEvent();
        structuredSyncEvent.setStack(stackDetails);

        CDPClusterDetails result = underTest.convert(structuredSyncEvent);

        assertEquals("", result.getDatabaseDetails().getAttributes());
        assertEquals("", result.getDatabaseDetails().getEngineVersion());
        assertEquals("", result.getDatabaseDetails().getAvailabilityType());
    }

}
