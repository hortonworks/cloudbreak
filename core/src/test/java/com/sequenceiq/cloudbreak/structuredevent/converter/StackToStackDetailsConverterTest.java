package com.sequenceiq.cloudbreak.structuredevent.converter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.StackType;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.database.DatabaseAvailabilityType;
import com.sequenceiq.cloudbreak.common.json.Json;
import com.sequenceiq.cloudbreak.domain.CustomConfigurations;
import com.sequenceiq.cloudbreak.domain.stack.Database;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.StackStatus;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.service.ComponentConfigProviderService;
import com.sequenceiq.cloudbreak.service.cluster.EmbeddedDatabaseService;
import com.sequenceiq.cloudbreak.service.customconfigs.CustomConfigurationsService;
import com.sequenceiq.cloudbreak.service.database.DatabaseService;
import com.sequenceiq.cloudbreak.structuredevent.event.CustomConfigurationsDetails;
import com.sequenceiq.cloudbreak.structuredevent.event.StackDetails;
import com.sequenceiq.common.api.type.Tunnel;

@ExtendWith(MockitoExtension.class)
public class StackToStackDetailsConverterTest {

    private static final String DB_SERVER_CRN = "crn:cdp:redbeams:us-west-1:default:databaseServer:e63520c8-aaf0-4bf3-b872-5613ce496ac3";

    @Mock
    private ComponentConfigProviderService componentConfigProviderService;

    @Mock
    private InstanceGroupToInstanceGroupDetailsConverter instanceGroupToInstanceGroupDetailsConverter;

    @Mock
    private CustomConfigurationsToCustomConfigurationsDetailsConverter customConfigurationsToCustomConfigurationsDetailsConverter;

    @Mock
    private ImageToImageDetailsConverter imageToImageDetailsConverter;

    @Mock
    private EmbeddedDatabaseService embeddedDatabaseService;

    @Mock
    private CustomConfigurationsService customConfigurationsService;

    @Mock
    private DatabaseService databaseService;

    @InjectMocks
    private StackToStackDetailsConverter underTest;

    @Test
    public void testConversion() {
        // GIVEN
        Stack stack = createStack();
        // WHEN
        StackDetails actual = underTest.convert(stack, stack.getCluster(), stack.getInstanceGroupDtos());
        // THEN
        assertEquals(stack.getName(), actual.getName());
        assertEquals(stack.getTunnel().name(), actual.getTunnel());
        assertEquals(stack.getType().name(), actual.getType());
        assertEquals("ON_ROOT_VOLUME", actual.getDatabaseType());
        assertEquals(stack.getJavaVersion(), actual.getJavaVersion());
        assertNotNull(actual.getDatabaseDetails());
    }

    @Test
    public void testConversionExternalDB() {
        // GIVEN
        Stack stack = createStack();
        stack.getCluster().setDatabaseServerCrn(DB_SERVER_CRN);
        // WHEN
        StackDetails actual = underTest.convert(stack, stack.getCluster(), stack.getInstanceGroupDtos());
        // THEN
        assertEquals("EXTERNAL_DB", actual.getDatabaseType());
    }

    @Test
    public void testConversionWhenDBOnAttachedDisk() {
        // GIVEN
        Stack stack = createStack();
        when(embeddedDatabaseService.isAttachedDiskForEmbeddedDatabaseCreated(stack.getCluster(), Optional.empty())).thenReturn(true);
        // WHEN
        StackDetails actual = underTest.convert(stack, stack.getCluster(), stack.getInstanceGroupDtos());
        // THEN
        assertEquals("ON_ATTACHED_VOLUME", actual.getDatabaseType());
    }

    @Test
    public void testConversionWhenDBTypeThrowsException() {
        // GIVEN
        Stack stack = createStack();
        when(embeddedDatabaseService.isAttachedDiskForEmbeddedDatabaseCreated(stack.getCluster(), Optional.empty())).thenThrow(new RuntimeException());
        // WHEN
        StackDetails actual = underTest.convert(stack, stack.getCluster(), stack.getInstanceGroupDtos());
        // THEN
        assertEquals("UNKNOWN", actual.getDatabaseType());
    }

    @Test
    public void testConversionWithCustomConfigsAndClusterInstalled() {
        // GIVEN
        Stack stack = createStack();
        Cluster cluster = new Cluster();
        CustomConfigurations customConfigurations = new CustomConfigurations();
        cluster.setCustomConfigurations(customConfigurations);
        stack.setCluster(cluster);

        CustomConfigurationsDetails customConfigurationsDetails = new CustomConfigurationsDetails();

        when(customConfigurationsService.getByCrn(any())).thenReturn(customConfigurations);
        when(customConfigurationsToCustomConfigurationsDetailsConverter.convert(any(CustomConfigurations.class)))
                .thenReturn(customConfigurationsDetails);

        // WHEN
        StackDetails actual = underTest.convert(stack, stack.getCluster(), stack.getInstanceGroupDtos());

        // THEN
        assertNotNull(actual.getCustomConfigurations());
        assertEquals(customConfigurationsDetails, actual.getCustomConfigurations());
    }

    @Test
    public void testConversionWithoutCustomConfigsAndClusterInstalled() {
        // GIVEN
        Stack stack = createStack();
        Cluster cluster = new Cluster();
        stack.setCluster(cluster);

        // WHEN
        StackDetails actual = underTest.convert(stack, cluster, stack.getInstanceGroupDtos());

        // THEN
        assertNull(actual.getCustomConfigurations());
    }

    @Test
    void testConvertDatabaseDetailsWhenNoDatabaseFound() {
        Stack stack = mock(Stack.class);
        when(stack.getDatabaseId()).thenReturn(1L);
        when(stack.getTunnel()).thenReturn(Tunnel.DIRECT);
        when(stack.getType()).thenReturn(StackType.DATALAKE);
        when(stack.getStatus()).thenReturn(Status.AVAILABLE);
        Cluster cluster = new Cluster();
        when(databaseService.findById(1L)).thenReturn(Optional.empty());

        StackDetails result = underTest.convert(stack, cluster, List.of());

        assertNotNull(result.getDatabaseDetails());
        assertNull(result.getDatabaseDetails().getAttributes());
        assertNull(result.getDatabaseDetails().getAvailabilityType());
        assertNull(result.getDatabaseDetails().getEngineVersion());
    }

    @Test
    void testConvertDatabaseDetailsWhenDatalakeAndTypeSet() {
        Stack stack = mock(Stack.class);
        when(stack.getDatabaseId()).thenReturn(1L);
        when(stack.getTunnel()).thenReturn(Tunnel.DIRECT);
        when(stack.getType()).thenReturn(StackType.DATALAKE);
        when(stack.isDatalake()).thenReturn(true);
        when(stack.getStatus()).thenReturn(Status.AVAILABLE);
        Cluster cluster = new Cluster();
        Database database = new Database();
        database.setExternalDatabaseEngineVersion("14");
        database.setExternalDatabaseAvailabilityType(DatabaseAvailabilityType.NON_HA);
        database.setDatalakeDatabaseAvailabilityType(DatabaseAvailabilityType.HA);
        database.setAttributes(Json.silent("test"));
        when(databaseService.findById(1L)).thenReturn(Optional.of(database));

        StackDetails result = underTest.convert(stack, cluster, List.of());

        assertNotNull(result.getDatabaseDetails());
        assertEquals(database.getAttributes().getValue(), result.getDatabaseDetails().getAttributes());
        assertEquals(database.getDatalakeDatabaseAvailabilityType().name(), result.getDatabaseDetails().getAvailabilityType());
        assertEquals(database.getExternalDatabaseEngineVersion(), result.getDatabaseDetails().getEngineVersion());
    }

    @Test
    void testConvertDatabaseDetailsWhenDatalakeAndTypeNotSetAndExternal() {
        Stack stack = mock(Stack.class);
        when(stack.getDatabaseId()).thenReturn(1L);
        when(stack.getTunnel()).thenReturn(Tunnel.DIRECT);
        when(stack.getType()).thenReturn(StackType.DATALAKE);
        when(stack.isDatalake()).thenReturn(true);
        when(stack.getStatus()).thenReturn(Status.AVAILABLE);
        Cluster cluster = new Cluster();
        cluster.setDatabaseServerCrn(DB_SERVER_CRN);
        Database database = new Database();
        database.setExternalDatabaseEngineVersion("14");
        database.setExternalDatabaseAvailabilityType(DatabaseAvailabilityType.NON_HA);
        database.setAttributes(Json.silent("test"));
        when(databaseService.findById(1L)).thenReturn(Optional.of(database));

        StackDetails result = underTest.convert(stack, cluster, List.of());

        assertNotNull(result.getDatabaseDetails());
        assertEquals(database.getAttributes().getValue(), result.getDatabaseDetails().getAttributes());
        assertEquals("EXTERNAL_DB", result.getDatabaseDetails().getAvailabilityType());
        assertEquals(database.getExternalDatabaseEngineVersion(), result.getDatabaseDetails().getEngineVersion());
    }

    @Test
    void testConvertDatabaseDetailsWhenDatalakeAndTypeNotSetAndNotExternal() {
        Stack stack = mock(Stack.class);
        when(stack.getDatabaseId()).thenReturn(1L);
        when(stack.getTunnel()).thenReturn(Tunnel.DIRECT);
        when(stack.getType()).thenReturn(StackType.DATALAKE);
        when(stack.isDatalake()).thenReturn(true);
        when(stack.getStatus()).thenReturn(Status.AVAILABLE);
        Cluster cluster = new Cluster();
        Database database = new Database();
        database.setExternalDatabaseEngineVersion("14");
        database.setExternalDatabaseAvailabilityType(DatabaseAvailabilityType.NON_HA);
        database.setAttributes(Json.silent("test"));
        when(databaseService.findById(1L)).thenReturn(Optional.of(database));

        StackDetails result = underTest.convert(stack, cluster, List.of());

        assertNotNull(result.getDatabaseDetails());
        assertEquals(database.getAttributes().getValue(), result.getDatabaseDetails().getAttributes());
        assertEquals(DatabaseAvailabilityType.NON_HA.name(), result.getDatabaseDetails().getAvailabilityType());
        assertEquals(database.getExternalDatabaseEngineVersion(), result.getDatabaseDetails().getEngineVersion());
    }

    @Test
    void testConvertDatabaseDetailsWhenDatalakeAndTypeNotSetAndNotExternalAndAvailabilityDefault() {
        Stack stack = mock(Stack.class);
        when(stack.getDatabaseId()).thenReturn(1L);
        when(stack.getTunnel()).thenReturn(Tunnel.DIRECT);
        when(stack.getType()).thenReturn(StackType.DATALAKE);
        when(stack.isDatalake()).thenReturn(true);
        when(stack.getStatus()).thenReturn(Status.AVAILABLE);
        Cluster cluster = new Cluster();
        Database database = new Database();
        database.setExternalDatabaseEngineVersion("14");
        database.setAttributes(Json.silent("test"));
        when(databaseService.findById(1L)).thenReturn(Optional.of(database));

        StackDetails result = underTest.convert(stack, cluster, List.of());

        assertNotNull(result.getDatabaseDetails());
        assertEquals(database.getAttributes().getValue(), result.getDatabaseDetails().getAttributes());
        assertEquals(DatabaseAvailabilityType.NONE.name(), result.getDatabaseDetails().getAvailabilityType());
        assertEquals(database.getExternalDatabaseEngineVersion(), result.getDatabaseDetails().getEngineVersion());
    }

    @Test
    void testConvertDatabaseDetailsWhenDatahubAndTypeSet() {
        Stack stack = mock(Stack.class);
        when(stack.getDatabaseId()).thenReturn(1L);
        when(stack.getTunnel()).thenReturn(Tunnel.DIRECT);
        when(stack.getType()).thenReturn(StackType.WORKLOAD);
        when(stack.isDatalake()).thenReturn(false);
        when(stack.getStatus()).thenReturn(Status.AVAILABLE);
        Cluster cluster = new Cluster();
        Database database = new Database();
        database.setExternalDatabaseEngineVersion("14");
        database.setExternalDatabaseAvailabilityType(DatabaseAvailabilityType.NON_HA);
        database.setDatalakeDatabaseAvailabilityType(DatabaseAvailabilityType.HA);
        database.setAttributes(Json.silent("test"));
        when(databaseService.findById(1L)).thenReturn(Optional.of(database));

        StackDetails result = underTest.convert(stack, cluster, List.of());

        assertNotNull(result.getDatabaseDetails());
        assertEquals(database.getAttributes().getValue(), result.getDatabaseDetails().getAttributes());
        assertEquals(database.getExternalDatabaseAvailabilityType().name(), result.getDatabaseDetails().getAvailabilityType());
        assertEquals(database.getExternalDatabaseEngineVersion(), result.getDatabaseDetails().getEngineVersion());
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
        stack.setJavaVersion(11);
        return stack;
    }
}
