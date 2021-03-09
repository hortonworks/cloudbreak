package com.sequenceiq.cloudbreak.core.flow2.externaldatabase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.Map;

import javax.ws.rs.BadRequestException;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.dyngr.core.AttemptResults;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.database.DatabaseAvailabilityType;
import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.repository.cluster.ClusterRepository;
import com.sequenceiq.cloudbreak.service.externaldatabase.DatabaseOperation;
import com.sequenceiq.cloudbreak.service.externaldatabase.DatabaseServerParameterDecorator;
import com.sequenceiq.cloudbreak.service.externaldatabase.model.DatabaseServerParameter;
import com.sequenceiq.cloudbreak.service.externaldatabase.model.DatabaseStackConfig;
import com.sequenceiq.cloudbreak.service.rdsconfig.RedbeamsClientService;
import com.sequenceiq.environment.api.v1.environment.model.response.DetailedEnvironmentResponse;
import com.sequenceiq.redbeams.api.endpoint.v4.databaseserver.requests.AllocateDatabaseServerV4Request;
import com.sequenceiq.redbeams.api.endpoint.v4.databaseserver.responses.DatabaseServerStatusV4Response;
import com.sequenceiq.redbeams.api.endpoint.v4.databaseserver.responses.DatabaseServerV4Response;

@ExtendWith(MockitoExtension.class)
class ExternalDatabaseServiceTest {

    private static final CloudPlatform CLOUD_PLATFORM = CloudPlatform.AZURE;

    private static final String RDBMS_CRN = "rdbmsCRN";

    @Mock
    private RedbeamsClientService redbeamsClient;

    @Mock
    private ClusterRepository clusterRepository;

    private final Map<CloudPlatform, DatabaseStackConfig> dbConfigs = new HashMap<>();

    @Mock
    private Map<CloudPlatform, DatabaseServerParameterDecorator> parameterDecoratorMap;

    @Mock
    private DatabaseObtainerService databaseObtainerService;

    @Mock
    private DatabaseServerParameterDecorator dbServerParameterDecorator;

    private ExternalDatabaseService underTest;

    private DetailedEnvironmentResponse environmentResponse;

    @BeforeEach
    void setUp() {
        underTest = new ExternalDatabaseService(redbeamsClient, clusterRepository, dbConfigs, parameterDecoratorMap, databaseObtainerService);
        environmentResponse = new DetailedEnvironmentResponse();
        environmentResponse.setCloudPlatform(CLOUD_PLATFORM.name());
        dbConfigs.put(CLOUD_PLATFORM, new DatabaseStackConfig("instance", "vendor", 1234));
        lenient().when(parameterDecoratorMap.get(CLOUD_PLATFORM)).thenReturn(dbServerParameterDecorator);
    }

    @Test
    void provisionDatabaseBadRequest() {
        when(redbeamsClient.create(any())).thenThrow(BadRequestException.class);

        assertThatThrownBy(() -> underTest.provisionDatabase(new Cluster(), DatabaseAvailabilityType.HA, environmentResponse))
                .isInstanceOf(BadRequestException.class);
    }

    @ParameterizedTest
    @EnumSource(DatabaseAvailabilityType.class)
    void provisionDatabase(DatabaseAvailabilityType availabilty) throws JsonProcessingException {
        Assumptions.assumeTrue(!availabilty.isEmbedded());

        DatabaseServerStatusV4Response createResponse = new DatabaseServerStatusV4Response();
        createResponse.setResourceCrn(RDBMS_CRN);
        Cluster cluster = spy(new Cluster());
        when(redbeamsClient.create(any())).thenReturn(createResponse);
        when(databaseObtainerService.obtainAttemptResult(eq(cluster), eq(DatabaseOperation.CREATION), eq(RDBMS_CRN), eq(true)))
                .thenReturn(AttemptResults.finishWith(new DatabaseServerV4Response()));
        underTest.provisionDatabase(cluster, availabilty, environmentResponse);

        ArgumentCaptor<DatabaseServerParameter> serverParameterCaptor = ArgumentCaptor.forClass(DatabaseServerParameter.class);
        verify(redbeamsClient).create(any(AllocateDatabaseServerV4Request.class));
        verify(cluster).setDatabaseServerCrn(RDBMS_CRN);
        verify(dbServerParameterDecorator).setParameters(any(), serverParameterCaptor.capture());
        verify(clusterRepository).save(cluster);
        DatabaseServerParameter paramValue = serverParameterCaptor.getValue();
        assertThat(paramValue.isHighlyAvailable()).isEqualTo(availabilty == DatabaseAvailabilityType.HA);
    }

    @ParameterizedTest
    @ValueSource(booleans = { false, true })
    void terminateDatabase(boolean forced) throws JsonProcessingException {
        Cluster cluster = new Cluster();
        cluster.setDatabaseServerCrn(RDBMS_CRN);
        DatabaseServerV4Response deleteResponse = new DatabaseServerV4Response();
        deleteResponse.setCrn(RDBMS_CRN);
        when(databaseObtainerService.obtainAttemptResult(eq(cluster), eq(DatabaseOperation.DELETION), eq(RDBMS_CRN), eq(false)))
                .thenReturn(AttemptResults.finishWith(new DatabaseServerV4Response()));
        when(redbeamsClient.deleteByCrn(any(), anyBoolean())).thenReturn(deleteResponse);

        underTest.terminateDatabase(cluster, DatabaseAvailabilityType.HA, environmentResponse, forced);

        ArgumentCaptor<Boolean> forceCaptor = ArgumentCaptor.forClass(Boolean.class);
        verify(redbeamsClient).deleteByCrn(eq(RDBMS_CRN), forceCaptor.capture());
        assertThat(forceCaptor.getValue()).isEqualTo(forced);
    }

    @Test
    void terminateDatabaseWhenCrnNull() {
        Cluster cluster = new Cluster();
        cluster.setDatabaseServerCrn(null);

        underTest.terminateDatabase(cluster, DatabaseAvailabilityType.HA, environmentResponse, true);

        verify(redbeamsClient, never()).deleteByCrn(anyString(), anyBoolean());
    }

    @Test
    void startDatabase() throws JsonProcessingException {
        Cluster cluster = new Cluster();
        cluster.setDatabaseServerCrn(RDBMS_CRN);
        DatabaseServerV4Response deleteResponse = new DatabaseServerV4Response();
        deleteResponse.setCrn(RDBMS_CRN);
        when(databaseObtainerService.obtainAttemptResult(eq(cluster), eq(DatabaseOperation.START), eq(RDBMS_CRN), eq(false)))
                .thenReturn(AttemptResults.finishWith(new DatabaseServerV4Response()));

        underTest.startDatabase(cluster, DatabaseAvailabilityType.HA, environmentResponse);

        verify(redbeamsClient).startByCrn(RDBMS_CRN);
    }

    @Test
    void startDatabaseWhenCrnNull() {
        Cluster cluster = new Cluster();
        cluster.setDatabaseServerCrn(null);

        underTest.startDatabase(cluster, DatabaseAvailabilityType.HA, environmentResponse);

        verify(redbeamsClient, never()).startByCrn(anyString());
    }

    @Test
    void stopDatabase() throws JsonProcessingException {
        Cluster cluster = spy(new Cluster());
        cluster.setDatabaseServerCrn(RDBMS_CRN);
        DatabaseServerV4Response deleteResponse = new DatabaseServerV4Response();
        deleteResponse.setCrn(RDBMS_CRN);
        when(databaseObtainerService.obtainAttemptResult(eq(cluster), eq(DatabaseOperation.STOP), eq(RDBMS_CRN), eq(false)))
                .thenReturn(AttemptResults.finishWith(new DatabaseServerV4Response()));

        underTest.stopDatabase(cluster, DatabaseAvailabilityType.HA, environmentResponse);

        verify(redbeamsClient).stopByCrn(RDBMS_CRN);
    }

    @Test
    void stopDatabaseWhenCrnNull() {
        Cluster cluster = spy(new Cluster());
        cluster.setDatabaseServerCrn(null);

        underTest.stopDatabase(cluster, DatabaseAvailabilityType.HA, environmentResponse);

        verify(redbeamsClient, never()).stopByCrn(anyString());
    }
}
