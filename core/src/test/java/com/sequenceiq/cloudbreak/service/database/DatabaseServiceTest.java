package com.sequenceiq.cloudbreak.service.database;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Date;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.database.DatabaseServerResourceStatus;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.database.DatabaseServerStatus;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.database.StackDatabaseServerResponse;
import com.sequenceiq.cloudbreak.common.exception.NotFoundException;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.distrox.v1.distrox.StackOperations;
import com.sequenceiq.distrox.v1.distrox.converter.DatabaseServerConverter;
import com.sequenceiq.redbeams.api.endpoint.v4.ResourceStatus;
import com.sequenceiq.redbeams.api.endpoint.v4.databaseserver.DatabaseServerV4Endpoint;
import com.sequenceiq.redbeams.api.endpoint.v4.databaseserver.responses.DatabaseServerV4Response;
import com.sequenceiq.redbeams.api.model.common.Status;

@ExtendWith(MockitoExtension.class)
class DatabaseServiceTest {

    private static final String CLUSTER_CRN = "clusterCrn";

    private static final String DATABASE_CRN = "databaseCrn";

    @Mock
    private StackOperations stackOperations;

    @Mock
    private DatabaseServerV4Endpoint databaseServerV4Endpoint;

    @Spy
    private DatabaseServerConverter databaseServerConverter;

    @InjectMocks
    private DatabaseService underTest;

    @Test
    public void testGetDatabaseServerShouldReturnDatabaseServer() {
        when(stackOperations.getStackByCrn(CLUSTER_CRN)).thenReturn(createStack());
        when(databaseServerV4Endpoint.getByCrn(DATABASE_CRN)).thenReturn(createDatabaseServerV4Response());
        StackDatabaseServerResponse response = underTest.getDatabaseServer(CLUSTER_CRN);

        assertThat(response.getClusterCrn()).isEqualTo(CLUSTER_CRN);
        assertThat(response.getCrn()).isEqualTo(DATABASE_CRN);
        assertThat(response.getResourceStatus()).isEqualTo(DatabaseServerResourceStatus.SERVICE_MANAGED);
        assertThat(response.getStatus()).isEqualTo(DatabaseServerStatus.AVAILABLE);
    }

    @Test
    public void testGetDatabaseServerWhenNoClusterShouldThrowNotFoundException() {
        when(stackOperations.getStackByCrn(CLUSTER_CRN)).thenReturn(new Stack());

        NotFoundException exception = assertThrows(NotFoundException.class,
                () -> underTest.getDatabaseServer(CLUSTER_CRN));

        assertThat(exception.getMessage()).isEqualTo("Data Hub with crn: 'clusterCrn' not found.");
        verify(databaseServerV4Endpoint, never()).getByCrn(anyString());
    }

    @Test
    public void testGetDatabaseServerWhenNoDatabaseCrnShouldThrowNotFoundException() {
        Stack stack = createStack();
        stack.getCluster().setDatabaseServerCrn(null);
        when(stackOperations.getStackByCrn(CLUSTER_CRN)).thenReturn(stack);

        NotFoundException exception = assertThrows(NotFoundException.class,
                () -> underTest.getDatabaseServer(CLUSTER_CRN));

        assertThat(exception.getMessage()).isEqualTo("Database for Data Hub with Data Hub crn: 'clusterCrn' not found.");
        verify(databaseServerV4Endpoint, never()).getByCrn(anyString());
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

    private Stack createStack() {
        Stack stack = new Stack();
        stack.setResourceCrn(CLUSTER_CRN);
        Cluster cluster = new Cluster();
        cluster.setDatabaseServerCrn(DATABASE_CRN);
        stack.setCluster(cluster);
        return stack;
    }
}