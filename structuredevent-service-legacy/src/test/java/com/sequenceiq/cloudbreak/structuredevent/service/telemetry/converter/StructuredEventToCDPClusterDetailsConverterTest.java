package com.sequenceiq.cloudbreak.structuredevent.service.telemetry.converter;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.cloudera.thunderhead.service.common.usage.UsageProto;
import com.sequenceiq.cloudbreak.cloud.model.StackTags;
import com.sequenceiq.cloudbreak.common.json.Json;
import com.sequenceiq.cloudbreak.structuredevent.event.StackDetails;
import com.sequenceiq.cloudbreak.structuredevent.event.StructuredFlowEvent;
import com.sequenceiq.cloudbreak.structuredevent.event.StructuredSyncEvent;

@ExtendWith(MockitoExtension.class)
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
        doReturn(UsageProto.CDPClusterShape.newBuilder().build()).when(clusterShapeConverter).convert((StructuredFlowEvent) any());
        doReturn(UsageProto.CDPClusterShape.newBuilder().build()).when(clusterShapeConverter).convert((StructuredSyncEvent) any());
        doReturn(UsageProto.CDPImageDetails.newBuilder().build()).when(imageDetailsConverter).convert((StructuredFlowEvent) any());
        doReturn(UsageProto.CDPImageDetails.newBuilder().build()).when(imageDetailsConverter).convert((StructuredSyncEvent) any());
        doReturn(UsageProto.CDPVersionDetails.newBuilder().build()).when(versionDetailsConverter).convert((StructuredFlowEvent) any());
        doReturn(UsageProto.CDPVersionDetails.newBuilder().build()).when(versionDetailsConverter).convert((StructuredSyncEvent) any());
    }

    @Test
    public void testConvertWithNull() {
        Assertions.assertNotNull(underTest.convert((StructuredFlowEvent) null), "We should return empty object for not null");
        Assertions.assertNotNull(underTest.convert((StructuredSyncEvent) null), "We should return empty object for not null");
    }

    @Test
    public void testUserTagsConversionWithNullStackDetails() {
        StructuredFlowEvent structuredFlowEvent = new StructuredFlowEvent();
        structuredFlowEvent.setStack(null);
        UsageProto.CDPClusterDetails clusterDetails = underTest.convert(structuredFlowEvent);

        Assertions.assertEquals("", clusterDetails.getUserTags());

        StructuredSyncEvent structuredSyncEvent = new StructuredSyncEvent();
        structuredSyncEvent.setStack(null);
        clusterDetails = underTest.convert(structuredSyncEvent);

        Assertions.assertEquals("", clusterDetails.getUserTags());
    }

    @Test
    public void testUserTagsConversionWithNullTags() {
        StackDetails stackDetails = new StackDetails();
        stackDetails.setTags(null);

        StructuredFlowEvent structuredFlowEvent = new StructuredFlowEvent();
        structuredFlowEvent.setStack(stackDetails);
        UsageProto.CDPClusterDetails clusterDetails = underTest.convert(structuredFlowEvent);

        Assertions.assertEquals("", clusterDetails.getUserTags());

        StructuredSyncEvent structuredSyncEvent = new StructuredSyncEvent();
        structuredSyncEvent.setStack(stackDetails);
        clusterDetails = underTest.convert(structuredSyncEvent);

        Assertions.assertEquals("", clusterDetails.getUserTags());
    }

    @Test
    public void testVariantConversionWithNullVariant() {
        StackDetails stackDetails = new StackDetails();
        stackDetails.setPlatformVariant(null);

        StructuredFlowEvent structuredFlowEvent = new StructuredFlowEvent();
        structuredFlowEvent.setStack(stackDetails);
        UsageProto.CDPClusterDetails clusterDetails = underTest.convert(structuredFlowEvent);

        Assertions.assertEquals(UsageProto.CDPCloudProviderVariantType.Value.UNSET, clusterDetails.getCloudProviderVariant());

        StructuredSyncEvent structuredSyncEvent = new StructuredSyncEvent();
        structuredSyncEvent.setStack(stackDetails);
        clusterDetails = underTest.convert(structuredSyncEvent);

        Assertions.assertEquals(0, clusterDetails.getCloudProviderVariantValue());
    }

    @Test
    public void testVariantConversionWithNotNullVariant() {
        StackDetails stackDetails = new StackDetails();
        stackDetails.setPlatformVariant("AWS_NATIVE");
        stackDetails.setMultiAz(true);

        StructuredFlowEvent structuredFlowEvent = new StructuredFlowEvent();
        structuredFlowEvent.setStack(stackDetails);
        UsageProto.CDPClusterDetails clusterDetails = underTest.convert(structuredFlowEvent);

        Assertions.assertEquals(UsageProto.CDPCloudProviderVariantType.Value.AWS_NATIVE, clusterDetails.getCloudProviderVariant());

        StructuredSyncEvent structuredSyncEvent = new StructuredSyncEvent();
        structuredSyncEvent.setStack(stackDetails);
        clusterDetails = underTest.convert(structuredSyncEvent);

        Assertions.assertEquals(2, clusterDetails.getCloudProviderVariantValue());
        Assertions.assertEquals(true, clusterDetails.getMultiAz());
    }

    @Test
    public void testUserTagsConversionWithEmptyTags() {
        StackDetails stackDetails = new StackDetails();
        stackDetails.setTags(new Json(""));

        StructuredFlowEvent structuredFlowEvent = new StructuredFlowEvent();
        structuredFlowEvent.setStack(stackDetails);
        UsageProto.CDPClusterDetails clusterDetails = underTest.convert(structuredFlowEvent);

        Assertions.assertEquals("", clusterDetails.getUserTags());

        StructuredSyncEvent structuredSyncEvent = new StructuredSyncEvent();
        structuredSyncEvent.setStack(stackDetails);
        clusterDetails = underTest.convert(structuredSyncEvent);

        Assertions.assertEquals("", clusterDetails.getUserTags());
    }

    @Test
    public void testUserTagsConversionWithCorrectTags() {
        StackDetails stackDetails = new StackDetails();
        Map<String, String> userTags = new HashMap<>();
        userTags.put("key1", "value1");
        userTags.put("key2", "value2");
        stackDetails.setTags(new Json(new StackTags(userTags, new HashMap<>(), new HashMap<>())));

        StructuredFlowEvent structuredFlowEvent = new StructuredFlowEvent();
        structuredFlowEvent.setStack(stackDetails);
        UsageProto.CDPClusterDetails clusterDetails = underTest.convert(structuredFlowEvent);

        Assertions.assertEquals("{\"key1\":\"value1\",\"key2\":\"value2\"}", clusterDetails.getUserTags());

        StructuredSyncEvent structuredSyncEvent = new StructuredSyncEvent();
        structuredSyncEvent.setStack(stackDetails);
        clusterDetails = underTest.convert(structuredSyncEvent);

        Assertions.assertEquals("{\"key1\":\"value1\",\"key2\":\"value2\"}", clusterDetails.getUserTags());
    }
}
