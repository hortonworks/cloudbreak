package com.sequenceiq.cloudbreak.cloud.azure.connector.resource;

import static com.sequenceiq.common.model.AzureDatabaseType.FLEXIBLE_SERVER;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.azure.resourcemanager.postgresql.models.StorageProfile;
import com.azure.resourcemanager.postgresqlflexibleserver.models.Server;
import com.azure.resourcemanager.postgresqlflexibleserver.models.Storage;
import com.sequenceiq.cloudbreak.cloud.azure.AzureResourceGroupMetadataProvider;
import com.sequenceiq.cloudbreak.cloud.azure.client.AzureClient;
import com.sequenceiq.cloudbreak.cloud.azure.client.AzureFlexibleServerClient;
import com.sequenceiq.cloudbreak.cloud.azure.client.AzureSingleServerClient;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.model.DatabaseServer;
import com.sequenceiq.cloudbreak.cloud.model.DatabaseStack;
import com.sequenceiq.cloudbreak.cloud.model.database.DatabaseServerStorageMetrics;
import com.sequenceiq.common.model.AzureDatabaseType;

@ExtendWith(MockitoExtension.class)
class AzureDatabaseStorageMetricTest {

    private static final String RESOURCE_GROUP_NAME = "resource group name";

    private static final String SERVER_NAME = "serverName";

    private static final String RESOURCE_ID = "/subscriptions/sub/resourceGroups/rg/providers/Microsoft.DBforPostgreSQL/flexibleServers/serverName";

    private static final String STORAGE_PERCENT_METRIC = "storage_percent";

    private static final Duration LOOKBACK = Duration.ofHours(1);

    private static final long BYTES_IN_GB = 1024L * 1024L * 1024L;

    @Mock
    private AzureResourceGroupMetadataProvider azureResourceGroupMetadataProvider;

    @Mock
    private AuthenticatedContext ac;

    @Mock
    private CloudContext cloudContext;

    @Mock
    private AzureClient client;

    @Mock
    private DatabaseStack databaseStack;

    @InjectMocks
    private AzureDatabaseResourceService underTest;

    @BeforeEach
    void setUp() {
        lenient().when(ac.getCloudContext()).thenReturn(cloudContext);
        lenient().when(ac.getParameter(AzureClient.class)).thenReturn(client);
        lenient().when(azureResourceGroupMetadataProvider.getResourceGroupName(cloudContext, databaseStack)).thenReturn(RESOURCE_GROUP_NAME);
    }

    private void stubFlexibleServer(Integer storageSizeGB) {
        Map<String, Object> params = Map.of(AzureDatabaseType.AZURE_DATABASE_TYPE_KEY, FLEXIBLE_SERVER.name());
        when(databaseStack.getDatabaseServer()).thenReturn(DatabaseServer.builder().withServerId(SERVER_NAME).withParams(params).build());
        AzureFlexibleServerClient flexibleServerClient = mock(AzureFlexibleServerClient.class);
        when(client.getFlexibleServerClient()).thenReturn(flexibleServerClient);
        Server server = mock(Server.class);
        lenient().when(server.id()).thenReturn(RESOURCE_ID);
        Storage storage = mock(Storage.class);
        lenient().when(storage.storageSizeGB()).thenReturn(storageSizeGB);
        lenient().when(server.storage()).thenReturn(storage);
        when(flexibleServerClient.getFlexibleServer(RESOURCE_GROUP_NAME, SERVER_NAME)).thenReturn(Optional.of(server));
    }

    @Test
    void shouldComputeFreePercentageFromStoragePercentForFlexibleServer() {
        stubFlexibleServer(100);
        when(client.getLatestAverageMetricValue(eq(RESOURCE_ID), eq(STORAGE_PERCENT_METRIC), eq(LOOKBACK))).thenReturn(Optional.of(90.0d));

        Optional<DatabaseServerStorageMetrics> result = underTest.getDatabaseServerStorageMetrics(ac, databaseStack);

        assertThat(result).isPresent();
        assertThat(result.get().freeStoragePercentage()).isCloseTo(10.0d, within(0.0001d));
        assertThat(result.get().allocatedBytes()).isEqualTo(100L * BYTES_IN_GB);
        assertThat(result.get().freeBytes()).isEqualTo(Math.round(100L * BYTES_IN_GB * 0.10d));
    }

    @Test
    void shouldReturnEmptyWhenNoStoragePercentMetric() {
        stubFlexibleServer(100);
        when(client.getLatestAverageMetricValue(eq(RESOURCE_ID), eq(STORAGE_PERCENT_METRIC), eq(LOOKBACK))).thenReturn(Optional.empty());

        assertThat(underTest.getDatabaseServerStorageMetrics(ac, databaseStack)).isEmpty();
    }

    @Test
    void shouldReturnEmptyWhenFlexibleServerNotFound() {
        Map<String, Object> params = Map.of(AzureDatabaseType.AZURE_DATABASE_TYPE_KEY, FLEXIBLE_SERVER.name());
        when(databaseStack.getDatabaseServer()).thenReturn(DatabaseServer.builder().withServerId(SERVER_NAME).withParams(params).build());
        AzureFlexibleServerClient flexibleServerClient = mock(AzureFlexibleServerClient.class);
        when(client.getFlexibleServerClient()).thenReturn(flexibleServerClient);
        when(flexibleServerClient.getFlexibleServer(RESOURCE_GROUP_NAME, SERVER_NAME)).thenReturn(Optional.empty());

        assertThat(underTest.getDatabaseServerStorageMetrics(ac, databaseStack)).isEmpty();
    }

    @Test
    void shouldReturnNullFreeBytesWhenFlexibleServerStorageSizeUnknown() {
        stubFlexibleServer(null);
        when(client.getLatestAverageMetricValue(eq(RESOURCE_ID), eq(STORAGE_PERCENT_METRIC), eq(LOOKBACK))).thenReturn(Optional.of(80.0d));

        Optional<DatabaseServerStorageMetrics> result = underTest.getDatabaseServerStorageMetrics(ac, databaseStack);

        assertThat(result).isPresent();
        assertThat(result.get().freeStoragePercentage()).isCloseTo(20.0d, within(0.0001d));
        assertThat(result.get().allocatedBytes()).isNull();
        assertThat(result.get().freeBytes()).isNull();
    }

    @Test
    void shouldComputeFreePercentageFromStoragePercentForSingleServer() {
        when(databaseStack.getDatabaseServer()).thenReturn(DatabaseServer.builder().withServerId(SERVER_NAME).build());
        AzureSingleServerClient singleServerClient = mock(AzureSingleServerClient.class);
        when(client.getSingleServerClient()).thenReturn(singleServerClient);
        com.azure.resourcemanager.postgresql.models.Server server = mock(com.azure.resourcemanager.postgresql.models.Server.class);
        when(server.id()).thenReturn(RESOURCE_ID);
        StorageProfile storageProfile = mock(StorageProfile.class);
        when(storageProfile.storageMB()).thenReturn(100 * 1024);
        when(server.storageProfile()).thenReturn(storageProfile);
        when(singleServerClient.getSingleServer(RESOURCE_GROUP_NAME, SERVER_NAME)).thenReturn(Optional.of(server));
        when(client.getLatestAverageMetricValue(eq(RESOURCE_ID), eq(STORAGE_PERCENT_METRIC), eq(LOOKBACK))).thenReturn(Optional.of(75.0d));

        Optional<DatabaseServerStorageMetrics> result = underTest.getDatabaseServerStorageMetrics(ac, databaseStack);

        assertThat(result).isPresent();
        assertThat(result.get().freeStoragePercentage()).isCloseTo(25.0d, within(0.0001d));
        assertThat(result.get().allocatedBytes()).isEqualTo(100L * BYTES_IN_GB);
        assertThat(result.get().freeBytes()).isEqualTo(Math.round(100L * BYTES_IN_GB * 0.25d));
    }
}
