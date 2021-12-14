package com.sequenceiq.cloudbreak.structuredevent.converter;

import java.util.HashSet;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.StackType;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status;
import com.sequenceiq.cloudbreak.common.json.Json;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.StackStatus;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.service.ComponentConfigProviderService;
import com.sequenceiq.cloudbreak.service.cluster.EmbeddedDatabaseService;
import com.sequenceiq.cloudbreak.service.rdsconfig.RedbeamsDbServerConfigurer;
import com.sequenceiq.cloudbreak.structuredevent.event.StackDetails;
import com.sequenceiq.common.api.type.Tunnel;

@ExtendWith(MockitoExtension.class)
public class StackToStackDetailsConverterTest {
    @Mock
    private ComponentConfigProviderService componentConfigProviderService;

    @Mock
    private InstanceGroupToInstanceGroupDetailsConverter instanceGroupToInstanceGroupDetailsConverter;

    @Mock
    private ImageToImageDetailsConverter imageToImageDetailsConverter;

    @Mock
    private RedbeamsDbServerConfigurer dbServerConfigurer;

    @Mock
    private EmbeddedDatabaseService embeddedDatabaseService;

    @InjectMocks
    private StackToStackDetailsConverter underTest;

    @Test
    public void testConversion() {
        // GIVEN
        Stack stack = createStack();
        // WHEN
        StackDetails actual = underTest.convert(stack);
        // THEN
        Assertions.assertEquals(stack.getName(), actual.getName());
        Assertions.assertEquals(stack.getTunnel().name(), actual.getTunnel());
        Assertions.assertEquals(stack.getType().name(), actual.getType());
        Assertions.assertEquals("ON_ROOT_VOLUME", actual.getDatabaseType());
    }

    @Test
    public void testConversionExternalDB() {
        // GIVEN
        Stack stack = createStack();
        Mockito.when(dbServerConfigurer.isRemoteDatabaseNeeded(stack.getCluster())).thenReturn(true);
        // WHEN
        StackDetails actual = underTest.convert(stack);
        // THEN
        Assertions.assertEquals("EXTERNAL_DB", actual.getDatabaseType());
    }

    @Test
    public void testConversionWhenDBOnAttachedDisk() {
        // GIVEN
        Stack stack = createStack();
        Mockito.when(embeddedDatabaseService.isAttachedDiskForEmbeddedDatabaseCreated(stack)).thenReturn(true);
        // WHEN
        StackDetails actual = underTest.convert(stack);
        // THEN
        Assertions.assertEquals("ON_ATTACHED_VOLUME", actual.getDatabaseType());
    }

    @Test
    public void testConversionWhenDBTypeThrowsException() {
        // GIVEN
        Stack stack = createStack();
        Mockito.when(embeddedDatabaseService.isAttachedDiskForEmbeddedDatabaseCreated(stack)).thenThrow(new RuntimeException());
        // WHEN
        StackDetails actual = underTest.convert(stack);
        // THEN
        Assertions.assertEquals("UNKNOWN", actual.getDatabaseType());
    }

    private Stack createStack() {
        StackStatus status = new StackStatus();
        status.setStatus(Status.AVAILABLE);

        Stack stack = new Stack();
        stack.setId(0L);
        stack.setName("stack");
        stack.setStackStatus(status);
        stack.setTunnel(Tunnel.DIRECT);
        stack.setType(StackType.WORKLOAD);
        stack.setRegion("region");
        stack.setAvailabilityZone("avzone");
        stack.setDescription("description");
        stack.setCloudPlatform("cloudplatform");
        stack.setInstanceGroups(new HashSet<>());
        stack.setTags(new Json(""));
        stack.setCluster(new Cluster());
        return stack;
    }
}
