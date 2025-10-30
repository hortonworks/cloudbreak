package com.sequenceiq.cloudbreak.cm;

import static com.sequenceiq.cloudbreak.cm.ClouderaManagerClusterStatusService.FULL_VIEW;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.cloudera.api.swagger.ClouderaManagerResourceApi;
import com.cloudera.api.swagger.RoleConfigGroupsResourceApi;
import com.cloudera.api.swagger.ServicesResourceApi;
import com.cloudera.api.swagger.client.ApiClient;
import com.cloudera.api.swagger.client.ApiException;
import com.cloudera.api.swagger.model.ApiConfig;
import com.cloudera.api.swagger.model.ApiConfigList;
import com.sequenceiq.cloudbreak.client.HttpClientConfig;
import com.sequenceiq.cloudbreak.cluster.service.ClusterClientInitException;
import com.sequenceiq.cloudbreak.cluster.status.KraftMigrationStatus;
import com.sequenceiq.cloudbreak.cm.client.ClouderaManagerApiClientProvider;
import com.sequenceiq.cloudbreak.cm.client.ClouderaManagerClientInitException;
import com.sequenceiq.cloudbreak.cm.client.retry.ClouderaManagerApiFactory;
import com.sequenceiq.cloudbreak.cm.exception.ClouderaManagerOperationFailedException;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;

@ExtendWith(MockitoExtension.class)
public class ClouderaManagerClusterKraftMigrationStatusServiceTest {

    private static final String CLUSTER_NAME = "clusterName";

    private final HttpClientConfig clientConfig = new HttpClientConfig("1.2.3.4", null, null, null);

    @Mock
    private ApiClient client;

    @Mock
    private ClouderaManagerApiFactory clouderaManagerApiFactory;

    @Mock
    private ClouderaManagerApiClientProvider clouderaManagerApiClientProvider;

    @Mock
    private ClouderaManagerResourceApi clouderaManagerResourceApi;

    @Mock
    private ServicesResourceApi servicesResourceApi;

    @Mock
    private RoleConfigGroupsResourceApi roleConfigGroupsResourceApi;

    @Mock
    private ClouderaManagerConfigService configService;

    private ClouderaManagerClusterKraftMigrationStatusService subject;

    @BeforeEach
    public void init() throws ClouderaManagerClientInitException, ClusterClientInitException {
        Cluster cluster = new Cluster();
        cluster.setName(CLUSTER_NAME);
        Stack stack = new Stack();
        stack.setName(CLUSTER_NAME);
        stack.setCluster(cluster);

        subject = new ClouderaManagerClusterKraftMigrationStatusService(stack, clientConfig);
        ReflectionTestUtils.setField(subject, "clouderaManagerApiClientProvider", clouderaManagerApiClientProvider);
        ReflectionTestUtils.setField(subject, "clouderaManagerApiFactory", clouderaManagerApiFactory);
        ReflectionTestUtils.setField(subject, "configService", configService);
        when(clouderaManagerApiClientProvider.getV31Client(stack.getGatewayPort(), cluster.getCloudbreakClusterManagerUser(),
                cluster.getPassword(), clientConfig)).thenReturn(client);
        lenient().when(clouderaManagerApiFactory.getClouderaManagerResourceApi(client)).thenReturn(clouderaManagerResourceApi);
        lenient().when(clouderaManagerApiFactory.getServicesResourceApi(client)).thenReturn(servicesResourceApi);
        lenient().when(clouderaManagerApiFactory.getRoleConfigGroupsResourceApi(client)).thenReturn(roleConfigGroupsResourceApi);
        subject.initApiClient();
    }

    @ParameterizedTest
    @MethodSource("testGetKraftMigrationStatusParameters")
    void testGetKraftMigrationStatus(String metadataStore,
            String kafkaBrokerSafetyValve,
            String kraftSafetyValve,
            KraftMigrationStatus expectedStatus) throws ApiException {
        String serviceName = "serviceName";
        String kraftRoleConfigGroupName = "kraftRoleConfigGroupName";
        String kafkaRoleConfigGroupName = "kafkaRoleConfigGroupName";

        when(configService.getServiceName(CLUSTER_NAME, "KAFKA", servicesResourceApi)).thenReturn(Optional.of(serviceName));
        when(configService.getRoleConfigGroupNameByTypeAndServiceName("KRAFT", CLUSTER_NAME, serviceName, roleConfigGroupsResourceApi))
                .thenReturn(kraftRoleConfigGroupName);
        when(configService.getRoleConfigGroupNameByTypeAndServiceName("KAFKA_BROKER", CLUSTER_NAME, serviceName, roleConfigGroupsResourceApi))
                .thenReturn(kafkaRoleConfigGroupName);

        ApiConfigList kraftConfigList = new ApiConfigList();
        kraftConfigList.addItemsItem(apiConfig("kraft.properties_role_safety_valve", kraftSafetyValve));

        ApiConfigList kafkaConfigList = new ApiConfigList();
        kafkaConfigList.addItemsItem(apiConfig("metadata.store", metadataStore));
        kafkaConfigList.addItemsItem(apiConfig("kafka.properties_role_safety_valve", kafkaBrokerSafetyValve));

        when(roleConfigGroupsResourceApi.readConfig(CLUSTER_NAME, kraftRoleConfigGroupName, serviceName, FULL_VIEW)).thenReturn(kraftConfigList);
        when(roleConfigGroupsResourceApi.readConfig(CLUSTER_NAME, kafkaRoleConfigGroupName, serviceName, FULL_VIEW)).thenReturn(kafkaConfigList);

        assertEquals(expectedStatus, subject.getKraftMigrationStatus());
    }

    @Test
    void testGetKraftMigrationStatusWhenServiceNameNotFound() {
        when(configService.getServiceName(CLUSTER_NAME, "KAFKA", servicesResourceApi)).thenReturn(Optional.empty());

        Exception expectedException = assertThrows(ClouderaManagerOperationFailedException.class, () -> subject.getKraftMigrationStatus());
        assertEquals("Failed to get KRaft migration status. No KAFKA service type found for cluster " + CLUSTER_NAME, expectedException.getMessage());
    }

    @Test
    void testGetKraftMigrationStatusWhenApiExceptionOccurs() throws ApiException {
        String serviceName = "serviceName";
        String kraftRoleConfigGroupName = "kraftRoleConfigGroupName";
        String kafkaRoleConfigGroupName = "kafkaRoleConfigGroupName";

        when(configService.getServiceName(CLUSTER_NAME, "KAFKA", servicesResourceApi)).thenReturn(Optional.of(serviceName));
        when(configService.getRoleConfigGroupNameByTypeAndServiceName("KRAFT", CLUSTER_NAME, serviceName, roleConfigGroupsResourceApi))
                .thenThrow(ApiException.class);

        Exception expectedException = assertThrows(ClouderaManagerOperationFailedException.class, () -> subject.getKraftMigrationStatus());
        assertEquals("Exception occurred while retrieving KRaft migration status", expectedException.getMessage());
    }

    private static Stream<Arguments> testGetKraftMigrationStatusParameters() {
        String migrationEnabledConfig = "zookeeper.metadata.migration.enable=true";

        return Stream.of(
                Arguments.of("Zookeeper", "", "", KraftMigrationStatus.ZOOKEEPER_INSTALLED),
                Arguments.of("Zookeeper", "", migrationEnabledConfig, KraftMigrationStatus.PRE_MIGRATION),
                Arguments.of("Zookeeper", migrationEnabledConfig, migrationEnabledConfig, KraftMigrationStatus.BROKERS_IN_MIGRATION),
                Arguments.of("KRaft", migrationEnabledConfig, migrationEnabledConfig, KraftMigrationStatus.BROKERS_IN_KRAFT),
                Arguments.of("KRaft", "", "", KraftMigrationStatus.KRAFT_INSTALLED)
        );
    }

    private ApiConfig apiConfig(String key, String value) {
        return new ApiConfig().name(key).value(value);
    }
}

