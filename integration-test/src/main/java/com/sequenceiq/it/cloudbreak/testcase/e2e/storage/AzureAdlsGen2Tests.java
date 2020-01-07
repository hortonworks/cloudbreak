package com.sequenceiq.it.cloudbreak.testcase.e2e.storage;

import static com.sequenceiq.it.cloudbreak.assertion.storage.azure.AdlsGen2TestAssertion.stackContainsAdlsGen2Properties;
import static com.sequenceiq.it.cloudbreak.assertion.storage.azure.AdlsGen2TestAssertion.stackContainsStorageLocations;
import static com.sequenceiq.it.cloudbreak.util.storagelocation.StorageComponent.HIVE_METASTORE_EXTERNAL_WAREHOUSE;
import static com.sequenceiq.it.cloudbreak.util.storagelocation.StorageComponent.HIVE_METASTORE_WAREHOUSE;

import java.util.List;

import javax.inject.Inject;

import org.testng.annotations.Test;

import com.sequenceiq.common.api.cloudstorage.CloudStorageRequest;
import com.sequenceiq.common.api.cloudstorage.StorageIdentityBase;
import com.sequenceiq.common.api.cloudstorage.StorageLocationBase;
import com.sequenceiq.common.api.cloudstorage.old.AdlsGen2CloudStorageV1Parameters;
import com.sequenceiq.common.model.CloudStorageCdpService;
import com.sequenceiq.it.cloudbreak.client.StackTestClient;
import com.sequenceiq.it.cloudbreak.cloud.v4.azure.AzureProperties;
import com.sequenceiq.it.cloudbreak.context.Description;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.ClusterTestDto;
import com.sequenceiq.it.cloudbreak.dto.stack.StackTestDto;
import com.sequenceiq.it.cloudbreak.testcase.e2e.AbstractE2ETest;
import com.sequenceiq.it.cloudbreak.util.storagelocation.AzureTestStorageLocation;
import com.sequenceiq.it.cloudbreak.util.storagelocation.StorageComponent;

public class AzureAdlsGen2Tests extends AbstractE2ETest {

    private static final StorageComponent[] LOCATION_STORAGE_COMPONENTS = {HIVE_METASTORE_WAREHOUSE, HIVE_METASTORE_EXTERNAL_WAREHOUSE};

    @Inject
    private StackTestClient stackTestClient;

    @Inject
    private AzureProperties azureProperties;

    // I disabled the test because with this commit only S3 and WASB cloud storages are supported.
    @Test(dataProvider = TEST_CONTEXT, enabled = false)
    @Description(
            given = "there is a running cloudbreak",
            when = "a valid azure stack create request with attached ADLS Gen2 cloud storage is sent",
            and = "the stack is started",
            then = "the stack should be available AND deletable")
    public void testCreateStopAndStartClusterWithAdlsGen2CloudStorage(TestContext testContext) {
        testContext
                .given("clusterWithAdlsGen2", ClusterTestDto.class)
                .withCloudStorage(adlsGen2CloudStorageV4RequestWithoutStorageLocations())

                .given(StackTestDto.class)
                .withCluster("clusterWithAdlsGen2")

                .when(stackTestClient.createV4())
                .await(STACK_AVAILABLE)

                .then(stackTestClient.deleteV4()::action)
                .then(stackContainsAdlsGen2Properties())

                .validate();
    }

    @Test(dataProvider = TEST_CONTEXT, enabled = false)
    @Description(
            given = "there is a running cloudbreak",
            when = "a valid azure stack create request with attached ADLS Gen2 cloud storage and defined storage location is sent",
            and = "the stack is started",
            then = "the stack should be available AND deletable")
    public void testCreateStopAndStartClusterWithAdlsGen2AndCloudStorage(TestContext testContext) {
        String name = resourcePropertyProvider().getName();
        CloudStorageRequest cloudStorageRequest = adlsGen2CloudStorageV4RequestWithStorageLocations(name);

        testContext
                .given("clusterWithAdlsGen2", ClusterTestDto.class)
                .withCloudStorage(cloudStorageRequest)
                .withName(name)

                .given(StackTestDto.class)
                .withCluster("clusterWithAdlsGen2")
                .withName(name)

                .when(stackTestClient.createV4())
                .await(STACK_AVAILABLE)

                .then(stackTestClient.deleteV4()::action)
                .then(stackContainsStorageLocations())
                .then(stackContainsAdlsGen2Properties())

                .validate();
    }

    private CloudStorageRequest adlsGen2CloudStorageV4RequestWithStorageLocations(String clusterName) {
        CloudStorageRequest request = adlsGen2CloudStorageV4RequestWithoutStorageLocations();
        String accountName = azureProperties.getCloudstorage().getAccountName();
        String storageLocation = azureProperties.getCloudstorage().getBaseLocation();
        AzureTestStorageLocation azureStorageLocation = new AzureTestStorageLocation(accountName, clusterName, storageLocation);
        request.setLocations(azureStorageLocation.getAdlsGen2(LOCATION_STORAGE_COMPONENTS));
        return request;
    }

    private CloudStorageRequest adlsGen2CloudStorageV4RequestWithoutStorageLocations() {
        CloudStorageRequest request = new CloudStorageRequest();
        AdlsGen2CloudStorageV1Parameters adlsGen2 = new AdlsGen2CloudStorageV1Parameters();
        String accountName = azureProperties.getCloudstorage().getAccountName();
        String accountKey = azureProperties.getCloudstorage().getAccountKey();
        adlsGen2.setAccountKey(accountKey);
        adlsGen2.setAccountName(accountName);
        StorageLocationBase storageLocationBase = new StorageLocationBase();
        storageLocationBase.setType(CloudStorageCdpService.RANGER_AUDIT);
        storageLocationBase.setValue("somePath");
        request.setLocations(List.of(storageLocationBase));
        StorageIdentityBase storageIdentityBase = new StorageIdentityBase();
        storageIdentityBase.setAdlsGen2(adlsGen2);
        request.setIdentities(List.of(storageIdentityBase));
        return request;
    }

}
