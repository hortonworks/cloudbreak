package com.sequenceiq.cloudbreak.service.database;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Date;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.api.endpoint.v4.dto.NameOrCrn;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.database.DatabaseServerResourceStatus;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.database.DatabaseServerStatus;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.database.StackDatabaseServerResponse;
import com.sequenceiq.cloudbreak.auth.crn.RegionAwareInternalCrnGenerator;
import com.sequenceiq.cloudbreak.auth.crn.RegionAwareInternalCrnGeneratorFactory;
import com.sequenceiq.cloudbreak.common.exception.NotFoundException;
import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.cloudbreak.domain.stack.Database;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.dto.StackDto;
import com.sequenceiq.cloudbreak.repository.DatabaseRepository;
import com.sequenceiq.cloudbreak.service.externaldatabase.DatabaseServerParameterDecorator;
import com.sequenceiq.cloudbreak.service.stack.StackDtoService;
import com.sequenceiq.distrox.v1.distrox.StackOperations;
import com.sequenceiq.distrox.v1.distrox.converter.DatabaseServerConverter;
import com.sequenceiq.redbeams.api.endpoint.v4.ResourceStatus;
import com.sequenceiq.redbeams.api.endpoint.v4.databaseserver.DatabaseServerV4Endpoint;
import com.sequenceiq.redbeams.api.endpoint.v4.databaseserver.responses.DatabaseServerV4Response;
import com.sequenceiq.redbeams.api.model.common.Status;

@ExtendWith(MockitoExtension.class)
class DatabaseServiceTest {

    private static final String CLUSTER_CRN = "clusterCrn";

    private static final String CLUSTER_NAME = "clusterName";

    private static final String DATABASE_CRN = "databaseCrn";

    @Mock
    private StackOperations stackOperations;

    @Mock
    private DatabaseServerV4Endpoint databaseServerV4Endpoint;

    @Spy
    private DatabaseServerConverter databaseServerConverter;

    @Mock
    private RegionAwareInternalCrnGeneratorFactory regionAwareInternalCrnGeneratorFactory;

    @Mock
    private RegionAwareInternalCrnGenerator regionAwareInternalCrnGenerator;

    @Mock
    private DatabaseRepository databaseRepository;

    @Mock
    private Map<CloudPlatform, DatabaseServerParameterDecorator> databaseServerParameterDecoratorMap;

    @Mock
    private DatabaseServerParameterDecorator databaseServerParameterDecorator;

    @Mock
    private StackDtoService stackDtoService;

    @InjectMocks
    private DatabaseService underTest;

    @BeforeEach
    void setup() {
        lenient().when(regionAwareInternalCrnGenerator.getInternalCrnForServiceAsString()).thenReturn("crn");
        lenient().when(regionAwareInternalCrnGeneratorFactory.iam()).thenReturn(regionAwareInternalCrnGenerator);
    }

    @ParameterizedTest
    @MethodSource("nameOrCrnProvider")
    public void testGetDatabaseServerShouldReturnDatabaseServer(NameOrCrn nameOrCrn) {
        StackDto stackDto = createStackDto();
        when(databaseServerV4Endpoint.getByCrn(DATABASE_CRN)).thenReturn(createDatabaseServerV4Response());
        when(stackDtoService.getByNameOrCrn(any(), any())).thenReturn(stackDto);

        StackDatabaseServerResponse response = underTest.getDatabaseServer(nameOrCrn, "accountId");

        assertThat(response.getClusterCrn()).isEqualTo(CLUSTER_CRN);
        assertThat(response.getCrn()).isEqualTo(DATABASE_CRN);
        assertThat(response.getResourceStatus()).isEqualTo(DatabaseServerResourceStatus.SERVICE_MANAGED);
        assertThat(response.getStatus()).isEqualTo(DatabaseServerStatus.AVAILABLE);
    }

    @ParameterizedTest
    @MethodSource("nameOrCrnProvider")
    public void testGetDatabaseServerWhenNoClusterShouldThrowNotFoundException(NameOrCrn nameOrCrn) {
        StackDto stackDto = createStackDtoWithoutDatabaseServerCrn();

        when(stackDtoService.getByNameOrCrn(any(), any())).thenReturn(stackDto);

        NotFoundException exception = assertThrows(NotFoundException.class,
                () -> underTest.getDatabaseServer(nameOrCrn, "accountId"));

        assertThat(exception.getMessage()).isEqualTo(String.format("Data Hub with id: '%s' not found.", nameOrCrn.getNameOrCrn()));
        verify(databaseServerV4Endpoint, never()).getByCrn(anyString());
    }

    @ParameterizedTest
    @MethodSource("nameOrCrnProvider")
    public void testGetDatabaseServerWhenNoDatabaseCrnShouldThrowNotFoundException(NameOrCrn nameOrCrn) {
        StackDto stack = createStackDto();
        when(stack.getCluster().getDatabaseServerCrn()).thenReturn(null);
        when(stackDtoService.getByNameOrCrn(any(), any())).thenReturn(stack);

        NotFoundException exception = assertThrows(NotFoundException.class,
                () -> underTest.getDatabaseServer(nameOrCrn, "accountId"));

        assertThat(exception.getMessage()).isEqualTo(String.format("Database for Data Hub with Data Hub id: '%s' not found.", nameOrCrn.getNameOrCrn()));
        verify(databaseServerV4Endpoint, never()).getByCrn(anyString());
    }

    @Test
    void testUpdateExternalDatabaseEngineVersionDoNothing() {
        when(databaseRepository.updateExternalDatabaseEngineVersion(any(), any())).thenReturn(0);
        int actual = underTest.updateExternalDatabaseEngineVersion(1L, "14", null);
        verify(databaseRepository, never()).findById(any());
        verify(databaseServerParameterDecoratorMap, never()).get(any(CloudPlatform.class));
        verify(databaseRepository, never()).save(any());
        assertEquals(0L, actual);
    }

    @Test
    void testUpdateExternalDatabaseEngineVersionDoNothingNullCloudPlatform() {
        when(databaseRepository.updateExternalDatabaseEngineVersion(any(), any())).thenReturn(1);
        int actual = underTest.updateExternalDatabaseEngineVersion(1L, "14", null);
        verify(databaseRepository, never()).findById(any());
        verify(databaseServerParameterDecoratorMap, never()).get(any(CloudPlatform.class));
        verify(databaseRepository, never()).save(any());
        assertEquals(1, actual);
    }

    @Test
    void testUpdateExternalDatabaseEngineVersionNoDBFound() {
        when(databaseRepository.updateExternalDatabaseEngineVersion(any(), any())).thenReturn(1);
        int actual = underTest.updateExternalDatabaseEngineVersion(1L, "14", CloudPlatform.AZURE);
        verify(databaseRepository, times(1)).findById(1L);
        verify(databaseServerParameterDecoratorMap, never()).get(any(CloudPlatform.class));
        verify(databaseRepository, never()).save(any());
        assertEquals(1, actual);
    }

    @Test
    void testUpdateExternalDatabaseEngineVersionNoDBChange() {
        Database database = new Database();
        when(databaseRepository.updateExternalDatabaseEngineVersion(any(), any())).thenReturn(1);
        when(databaseRepository.findById(1L)).thenReturn(Optional.of(database));
        when(databaseServerParameterDecoratorMap.get(any(CloudPlatform.class))).thenReturn(databaseServerParameterDecorator);
        when(databaseServerParameterDecorator.updateVersionRelatedDatabaseParams(any(), any())).thenReturn(Optional.empty());
        int actual = underTest.updateExternalDatabaseEngineVersion(1L, "14", CloudPlatform.AZURE);
        verify(databaseRepository, never()).save(any());
        assertEquals(1, actual);
    }

    @Test
    void testUpdateExternalDatabaseEngineVersion() {
        Database database = new Database();
        Database updatedDatabase = new Database();
        when(databaseRepository.updateExternalDatabaseEngineVersion(any(), any())).thenReturn(1);
        when(databaseRepository.findById(1L)).thenReturn(Optional.of(database));
        when(databaseServerParameterDecoratorMap.get(any(CloudPlatform.class))).thenReturn(databaseServerParameterDecorator);
        when(databaseServerParameterDecorator.updateVersionRelatedDatabaseParams(any(), any())).thenReturn(Optional.of(updatedDatabase));
        int actual = underTest.updateExternalDatabaseEngineVersion(1L, "14", CloudPlatform.AZURE);
        verify(databaseRepository, times(1)).save(updatedDatabase);
        assertEquals(1, actual);
    }

    private DatabaseServerV4Response createDatabaseServerV4Response() {
        DatabaseServerV4Response databaseServerV4Response = new DatabaseServerV4Response();
        databaseServerV4Response.setCrn(DATABASE_CRN);
        databaseServerV4Response.setName("databaseName");
        databaseServerV4Response.setDescription("description");
        databaseServerV4Response.setEnvironmentCrn("envCrn");
        databaseServerV4Response.setHost("host");
        databaseServerV4Response.setPort(1234);
        databaseServerV4Response.setDatabaseVendor("vendor");
        databaseServerV4Response.setDatabaseVendorDisplayName("vendorName");
        databaseServerV4Response.setCreationDate(new Date().getTime());
        databaseServerV4Response.setResourceStatus(ResourceStatus.SERVICE_MANAGED);
        databaseServerV4Response.setStatus(Status.AVAILABLE);
        databaseServerV4Response.setStatusReason("Everything is great");
        databaseServerV4Response.setClusterCrn(CLUSTER_CRN);
        return databaseServerV4Response;
    }

    private static Stream<NameOrCrn> nameOrCrnProvider() {
        return Stream.of(NameOrCrn.ofName(CLUSTER_NAME), NameOrCrn.ofCrn(CLUSTER_CRN));
    }

    private Stack createStack() {
        Stack stack = new Stack();
        stack.setResourceCrn(CLUSTER_CRN);
        Cluster cluster = new Cluster();
        cluster.setDatabaseServerCrn(DATABASE_CRN);
        stack.setCluster(cluster);
        return stack;
    }

    private StackDto createStackDto() {
        StackDto stack = mock(StackDto.class);
        Cluster cluster = mock(Cluster.class);
        when(cluster.getDatabaseServerCrn()).thenReturn(DATABASE_CRN);
        when(stack.getCluster()).thenReturn(cluster);
        return stack;
    }

    private StackDto createStackDtoWithoutDatabaseServerCrn() {
        StackDto stack = mock(StackDto.class);
        when(stack.getCluster()).thenReturn(null);
        return stack;
    }
}