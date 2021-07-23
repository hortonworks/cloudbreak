package com.sequenceiq.cloudbreak.service.rdsconfig;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.TestUtil;
import com.sequenceiq.cloudbreak.api.endpoint.v4.database.base.DatabaseType;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.InstanceMetadataType;
import com.sequenceiq.cloudbreak.common.database.DatabaseCommon;
import com.sequenceiq.cloudbreak.domain.RDSConfig;
import com.sequenceiq.cloudbreak.domain.RdsSslMode;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceMetaData;
import com.sequenceiq.cloudbreak.service.secret.service.SecretService;
import com.sequenceiq.redbeams.api.endpoint.v4.databaseserver.requests.SslMode;
import com.sequenceiq.redbeams.api.endpoint.v4.databaseserver.responses.DatabaseServerV4Response;
import com.sequenceiq.redbeams.api.endpoint.v4.databaseserver.responses.SslConfigV4Response;

@ExtendWith(MockitoExtension.class)
public class RedbeamsDbServerConfigurerTest {

    private static final String EXAMPLE_JDBC_URL =
        "jdbc:postgresql://dbsvr-ed671174-77de-40e5-ad59-37761d8230d9.c8uqzbscgqmb.eu-west-1.rds.amazonaws.com:5432/clouderamanager";

    private static final String DB_SERVER_CRN = "crn:cdp:redbeams:us-west-1:default:databaseServer:e63520c8-aaf0-4bf3-b872-5613ce496ac3";

    private static final String DB_HOST = "dbsvr-ed671174-77de-40e5-ad59-37761d8230d9.c8uqzbscgqmb.eu-west-1.rds.amazonaws.com";

    private static final String DB_USER = "cmuser";

    private static final String EXAMPLE_JDBC_URL_AZURE =
        "jdbc:postgresql://dbsvr-ed671174-77de-40e5-ad59-37761d8230d9.postgres.database.azure.com:5432/clouderamanager";

    private static final String DB_HOST_AZURE = "dbsvr-ed671174-77de-40e5-ad59-37761d8230d9.postgres.database.azure.com";

    private static final String DB_HOST_SHORT_NAME = "dbsvr-ed671174-77de-40e5-ad59-37761d8230d9";

    @Mock
    private RedbeamsClientService redbeamsClientService;

    @Mock
    private SecretService secretService;

    @Mock
    private DatabaseCommon dbCommon;

    @Mock
    private DbUsernameConverterService dbUsernameConverterService;

    @InjectMocks
    private RedbeamsDbServerConfigurer underTest;

    @Test
    public void getRdsConfigWhenAws() {
        when(redbeamsClientService.getByCrn(DB_SERVER_CRN)).thenReturn(createDatabaseServerV4Response(DB_HOST));
        when(dbCommon.getJdbcConnectionUrl(any(), any(), anyInt(), any())).thenReturn(EXAMPLE_JDBC_URL);
        when(dbUsernameConverterService.toConnectionUsername(anyString(), anyString())).thenReturn(DB_USER);
        Cluster testCluster = createCluster(DB_SERVER_CRN);
        Stack testStack = createStack(testCluster);

        RDSConfig config = underTest.createNewRdsConfig(testStack, testCluster, "clouderamanager", DB_USER, DatabaseType.CLOUDERA_MANAGER);

        assertThat(config.getName()).isEqualTo("CLOUDERA_MANAGER_simplestack1");
        assertThat(config.getConnectionURL()).isEqualTo(EXAMPLE_JDBC_URL);
        assertThat(config.getConnectionUserName()).isEqualTo(DB_USER);
        assertThat(config.getSslMode()).isEqualTo(RdsSslMode.DISABLED);
    }

    static Object[][] sslDataProvider() {
        return new Object[][]{
                // testCaseName sslMode rdsSslModeExpected
                {"sslMode=null", null, RdsSslMode.DISABLED},
                {"sslMode=DISABLED", SslMode.DISABLED, RdsSslMode.DISABLED},
                {"sslMode=ENABLED", SslMode.ENABLED, RdsSslMode.ENABLED},
        };
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("sslDataProvider")
    public void getRdsConfigWhenAwsAndSsl(String testCaseName, SslMode sslMode, RdsSslMode rdsSslModeExpected) {
        when(redbeamsClientService.getByCrn(DB_SERVER_CRN)).thenReturn(createDatabaseServerV4ResponseWithSsl(DB_HOST, sslMode));
        when(dbCommon.getJdbcConnectionUrl(any(), any(), anyInt(), any())).thenReturn(EXAMPLE_JDBC_URL);
        when(dbUsernameConverterService.toConnectionUsername(anyString(), anyString())).thenReturn(DB_USER);
        Cluster testCluster = createCluster(DB_SERVER_CRN);
        Stack testStack = createStack(testCluster);

        RDSConfig config = underTest.createNewRdsConfig(testStack, testCluster, "clouderamanager", DB_USER, DatabaseType.CLOUDERA_MANAGER);

        assertThat(config.getName()).isEqualTo("CLOUDERA_MANAGER_simplestack1");
        assertThat(config.getConnectionURL()).isEqualTo(EXAMPLE_JDBC_URL);
        assertThat(config.getConnectionUserName()).isEqualTo(DB_USER);
        assertThat(config.getSslMode()).isEqualTo(rdsSslModeExpected);
    }

    @Test
    public void getRdsConfigWhenAzure() {
        when(redbeamsClientService.getByCrn(DB_SERVER_CRN)).thenReturn(createDatabaseServerV4Response(DB_HOST_AZURE));
        when(dbCommon.getJdbcConnectionUrl(any(), any(), anyInt(), any())).thenReturn(EXAMPLE_JDBC_URL_AZURE);
        when(dbUsernameConverterService.toConnectionUsername(anyString(), anyString())).thenReturn("cmuser@" + DB_HOST_SHORT_NAME);
        Cluster testCluster = createCluster(DB_SERVER_CRN);
        Stack testStack = createStack(testCluster);

        RDSConfig config = underTest.createNewRdsConfig(testStack, testCluster, "clouderamanager", DB_USER, DatabaseType.CLOUDERA_MANAGER);

        assertThat(config.getName()).isEqualTo("CLOUDERA_MANAGER_simplestack1");
        assertThat(config.getConnectionURL()).isEqualTo(EXAMPLE_JDBC_URL_AZURE);
        assertThat(config.getConnectionUserName()).isEqualTo("cmuser@" + DB_HOST_SHORT_NAME);
        assertThat(config.getSslMode()).isEqualTo(RdsSslMode.DISABLED);
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("sslDataProvider")
    public void getRdsConfigWhenAzureAndSsl(String testCaseName, SslMode sslMode, RdsSslMode rdsSslModeExpected) {
        when(redbeamsClientService.getByCrn(DB_SERVER_CRN)).thenReturn(createDatabaseServerV4ResponseWithSsl(DB_HOST_AZURE, sslMode));
        when(dbCommon.getJdbcConnectionUrl(any(), any(), anyInt(), any())).thenReturn(EXAMPLE_JDBC_URL_AZURE);
        when(dbUsernameConverterService.toConnectionUsername(anyString(), anyString())).thenReturn("cmuser@" + DB_HOST_SHORT_NAME);
        Cluster testCluster = createCluster(DB_SERVER_CRN);
        Stack testStack = createStack(testCluster);

        RDSConfig config = underTest.createNewRdsConfig(testStack, testCluster, "clouderamanager", DB_USER, DatabaseType.CLOUDERA_MANAGER);

        assertThat(config.getName()).isEqualTo("CLOUDERA_MANAGER_simplestack1");
        assertThat(config.getConnectionURL()).isEqualTo(EXAMPLE_JDBC_URL_AZURE);
        assertThat(config.getConnectionUserName()).isEqualTo("cmuser@" + DB_HOST_SHORT_NAME);
        assertThat(config.getSslMode()).isEqualTo(rdsSslModeExpected);
    }

    @Test
    public void isRemoteDatabaseNeededWhenDbServerCrnIsPresent() {
        Cluster testCluster = createCluster(DB_SERVER_CRN);
        assertThat(underTest.isRemoteDatabaseNeeded(testCluster)).isTrue();
    }

    @Test
    public void isRemoteDatabaseNeeded() {
        Cluster testCluster = createCluster(null);
        assertThat(underTest.isRemoteDatabaseNeeded(testCluster)).isFalse();
    }

    private Cluster createCluster(String dbServerCrn) {
        Cluster testCluster = TestUtil.cluster();
        testCluster.setDatabaseServerCrn(dbServerCrn);
        return testCluster;
    }

    private Stack createStack(Cluster testCluster) {
        Stack testStack = TestUtil.stack();
        InstanceMetaData metaData = testStack.getNotTerminatedGatewayInstanceMetadata().iterator().next();
        metaData.setInstanceMetadataType(InstanceMetadataType.GATEWAY_PRIMARY);
        testStack.getNotTerminatedGatewayInstanceMetadata().add(metaData);
        testStack.setCluster(testCluster);
        return testStack;
    }

    private DatabaseServerV4Response createDatabaseServerV4Response(String dbHost) {
        DatabaseServerV4Response resp = new DatabaseServerV4Response();
        resp.setPort(1234);
        resp.setHost(dbHost);
        resp.setDatabaseVendor("postgres");
        return resp;
    }

    private DatabaseServerV4Response createDatabaseServerV4ResponseWithSsl(String dbHost, SslMode sslMode) {
        SslConfigV4Response sslConfig = new SslConfigV4Response();
        sslConfig.setSslMode(sslMode);
        DatabaseServerV4Response response = createDatabaseServerV4Response(dbHost);
        response.setSslConfig(sslConfig);
        return response;
    }

}
