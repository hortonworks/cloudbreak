package com.sequenceiq.datalake.service.sdx.database;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.cloudbreak.exception.BadRequestException;
import com.sequenceiq.datalake.configuration.PlatformConfig;
import com.sequenceiq.datalake.entity.SdxCluster;
import com.sequenceiq.datalake.repository.SdxClusterRepository;
import com.sequenceiq.datalake.service.sdx.SdxNotificationService;
import com.sequenceiq.datalake.service.sdx.status.SdxStatusService;
import com.sequenceiq.environment.api.v1.environment.model.response.DetailedEnvironmentResponse;
import com.sequenceiq.redbeams.api.endpoint.v4.databaseserver.DatabaseServerV4Endpoint;
import com.sequenceiq.redbeams.api.endpoint.v4.databaseserver.requests.AllocateDatabaseServerV4Request;
import com.sequenceiq.redbeams.api.endpoint.v4.databaseserver.requests.SslConfigurationV4Request;
import com.sequenceiq.redbeams.api.endpoint.v4.databaseserver.requests.SslMode;
import com.sequenceiq.redbeams.api.endpoint.v4.databaseserver.responses.DatabaseServerV4Response;
import com.sequenceiq.redbeams.api.endpoint.v4.stacks.DatabaseServerV4StackRequest;
import com.sequenceiq.redbeams.api.endpoint.v4.stacks.aws.AwsDatabaseServerV4Parameters;
import com.sequenceiq.redbeams.api.model.common.Status;
import com.sequenceiq.redbeams.client.RedbeamsServiceCrnClient;
import com.sequenceiq.sdx.api.model.SdxClusterShape;
import com.sequenceiq.sdx.api.model.SdxDatabaseAvailabilityType;

@ExtendWith(MockitoExtension.class)
public class DatabaseServiceTest {

    private static final String DATABASE_CRN = "database crn";

    private static final String CLUSTER_CRN = "cluster crn";

    private static final String USER_CRN = "crn:cdp:iam:us-west-1:1234:user:1";

    @Captor
    public ArgumentCaptor<AllocateDatabaseServerV4Request> allocateDatabaseServerV4RequestCaptor =
            ArgumentCaptor.forClass(AllocateDatabaseServerV4Request.class);

    @Mock
    private SdxClusterRepository sdxClusterRepository;

    @Mock
    private RedbeamsServiceCrnClient redbeamsClient;

    @Mock
    private SdxNotificationService notificationService;

    @Mock
    private SdxStatusService sdxStatusService;

    @Mock
    private Map<DatabaseConfigKey, DatabaseConfig> dbConfigs;

    @Mock
    private DatabaseServerV4Endpoint databaseServerV4Endpoint;

    @Mock
    private Map<CloudPlatform, DatabaseServerParameterSetter> databaseParameterSetterMap;

    @Mock
    private PlatformConfig platformConfig;

    @InjectMocks
    private DatabaseService underTest;

    static Object[][] sslEnforcementDataProvider() {
        return new Object[][]{
                // testCaseName useSslEnforcement
                {"useSslEnforcement=false", false},
                {"useSslEnforcement=true", true},
        };
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("sslEnforcementDataProvider")
    public void shouldSetDbConfigBasedOnClusterShape(String testCaseName, boolean useSslEnforcement) {
        SdxCluster cluster = new SdxCluster();
        cluster.setClusterName("NAME");
        cluster.setClusterShape(SdxClusterShape.LIGHT_DUTY);
        cluster.setCrn(CLUSTER_CRN);
        DetailedEnvironmentResponse env = new DetailedEnvironmentResponse();
        env.setName("ENV");
        env.setCloudPlatform("aws");
        DatabaseConfig databaseConfig = getDatabaseConfig();

        when(databaseServerV4Endpoint.createInternal(any(), any())).thenThrow(BadRequestException.class);
        DatabaseConfigKey dbConfigKey = new DatabaseConfigKey(CloudPlatform.AWS, SdxClusterShape.LIGHT_DUTY);
        when(dbConfigs.get(dbConfigKey)).thenReturn(databaseConfig);
        when(databaseParameterSetterMap.get(CloudPlatform.AWS)).thenReturn(getDatabaseParameterSetter());
        when(platformConfig.isExternalDatabaseSslEnforcementSupportedFor(CloudPlatform.AWS)).thenReturn(useSslEnforcement);

        assertThatCode(() -> ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> underTest.create(cluster, env))).isInstanceOf(BadRequestException.class);

        verify(databaseServerV4Endpoint).createInternal(allocateDatabaseServerV4RequestCaptor.capture(), anyString());
        AllocateDatabaseServerV4Request dbRequest = allocateDatabaseServerV4RequestCaptor.getValue();
        assertThat(dbRequest).isNotNull();
        DatabaseServerV4StackRequest databaseServer = dbRequest.getDatabaseServer();
        assertThat(databaseServer).isNotNull();
        assertThat(databaseServer.getInstanceType()).isEqualTo("instanceType");
        assertThat(databaseServer.getDatabaseVendor()).isEqualTo("vendor");
        assertThat(databaseServer.getStorageSize()).isEqualTo(100L);
        assertThat(dbRequest.getClusterCrn()).isEqualTo(CLUSTER_CRN);
        assertThat(databaseServer.getAws()).isNotNull();
        SslConfigurationV4Request sslConfiguration = dbRequest.getSslConfiguration();
        if (useSslEnforcement) {
            assertThat(sslConfiguration).isNotNull();
            assertThat(sslConfiguration.getSslMode()).isEqualTo(SslMode.ENABLED);
        } else {
            assertThat(sslConfiguration).isNull();
        }
        verifyNoInteractions(sdxClusterRepository);
        verifyNoInteractions(sdxStatusService);
        verifyNoInteractions(notificationService);
    }

    @Test
    public void shouldCallStartAndWaitForAvailableStatus() {
        SdxCluster cluster = new SdxCluster();
        cluster.setDatabaseCrn(DATABASE_CRN);

        DatabaseServerV4Response databaseServerV4Response = mock(DatabaseServerV4Response.class);

        when(databaseServerV4Endpoint.getByCrn(DATABASE_CRN)).thenReturn(databaseServerV4Response);
        when(databaseServerV4Response.getStatus()).thenReturn(Status.AVAILABLE);

        underTest.start(cluster);

        verify(databaseServerV4Endpoint).start(DATABASE_CRN);
    }

    @Test
    public void shouldCallStopAndWaitForStoppedStatus() {
        SdxCluster cluster = new SdxCluster();
        cluster.setDatabaseCrn(DATABASE_CRN);

        DatabaseServerV4Response databaseServerV4Response = mock(DatabaseServerV4Response.class);

        when(databaseServerV4Endpoint.getByCrn(DATABASE_CRN)).thenReturn(databaseServerV4Response);
        when(databaseServerV4Response.getStatus()).thenReturn(Status.STOPPED);

        underTest.stop(cluster);

        verify(databaseServerV4Endpoint).stop(DATABASE_CRN);
    }

    private DatabaseConfig getDatabaseConfig() {
        return new DatabaseConfig("instanceType", "vendor", 100);
    }

    private DatabaseServerParameterSetter getDatabaseParameterSetter() {
        return new DatabaseServerParameterSetter() {
            @Override
            public void setParameters(DatabaseServerV4StackRequest request, SdxDatabaseAvailabilityType availabilityType) {
                request.setAws(new AwsDatabaseServerV4Parameters());
            }

            @Override
            public CloudPlatform getCloudPlatform() {
                return CloudPlatform.AWS;
            }
        };
    }

}
