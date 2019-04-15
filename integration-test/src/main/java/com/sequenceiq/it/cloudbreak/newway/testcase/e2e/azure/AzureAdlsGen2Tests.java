package com.sequenceiq.it.cloudbreak.newway.testcase.e2e.azure;

import static com.sequenceiq.it.cloudbreak.newway.assertion.storage.azure.AdlsGen2TestAssertion.stackContainsAdlsGen2Properties;
import static com.sequenceiq.it.cloudbreak.newway.assertion.storage.azure.AdlsGen2TestAssertion.stackContainsStorageLocations;
import static com.sequenceiq.it.cloudbreak.newway.util.storagelocation.StorageComponent.HIVE;
import static com.sequenceiq.it.cloudbreak.newway.util.storagelocation.StorageComponent.RANGER;
import static com.sequenceiq.it.cloudbreak.newway.util.storagelocation.StorageComponent.SPARK2;
import static com.sequenceiq.it.cloudbreak.newway.util.storagelocation.StorageComponent.TEZ;
import static com.sequenceiq.it.cloudbreak.newway.util.storagelocation.StorageComponent.YARN;
import static com.sequenceiq.it.cloudbreak.newway.util.storagelocation.StorageComponent.ZEPPELIN;

import javax.inject.Inject;

import org.testng.annotations.Test;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.parameter.storage.AdlsGen2CloudStorageV4Parameters;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.cluster.storage.CloudStorageV4Request;
import com.sequenceiq.it.cloudbreak.newway.client.StackTestClient;
import com.sequenceiq.it.cloudbreak.newway.cloud.v2.azure.AzureProperties;
import com.sequenceiq.it.cloudbreak.newway.context.Description;
import com.sequenceiq.it.cloudbreak.newway.context.TestContext;
import com.sequenceiq.it.cloudbreak.newway.dto.ClusterTestDto;
import com.sequenceiq.it.cloudbreak.newway.dto.stack.StackTestDto;
import com.sequenceiq.it.cloudbreak.newway.testcase.e2e.AbstractE2ETest;
import com.sequenceiq.it.cloudbreak.newway.util.storagelocation.AzureTestStorageLocation;
import com.sequenceiq.it.cloudbreak.newway.util.storagelocation.StorageComponent;

public class AzureAdlsGen2Tests extends AbstractE2ETest {

    private static final StorageComponent[] LOCATION_STORAGE_COMPONENTS = {HIVE, SPARK2, TEZ, RANGER, YARN, ZEPPELIN};

    @Inject
    private StackTestClient stackTestClient;

    @Inject
    private AzureProperties azureProperties;

    @Test(dataProvider = TEST_CONTEXT)
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
        CloudStorageV4Request cloudStorageV4Request = adlsGen2CloudStorageV4RequestWithStorageLocations(name);

        testContext
                .given("clusterWithAdlsGen2", ClusterTestDto.class)
                .withCloudStorage(cloudStorageV4Request)
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

    private CloudStorageV4Request adlsGen2CloudStorageV4RequestWithStorageLocations(String clusterName) {
        CloudStorageV4Request request = adlsGen2CloudStorageV4RequestWithoutStorageLocations();
        String accountName = azureProperties.getCloudstorage().getAccountName();
        String storageLocation = azureProperties.getCloudstorage().getLocationName();
        AzureTestStorageLocation azureStorageLocation = new AzureTestStorageLocation(accountName, clusterName, storageLocation);
        request.setLocations(azureStorageLocation.getAdlsGen2(LOCATION_STORAGE_COMPONENTS));
        return request;
    }

    private CloudStorageV4Request adlsGen2CloudStorageV4RequestWithoutStorageLocations() {
        CloudStorageV4Request request = new CloudStorageV4Request();
        AdlsGen2CloudStorageV4Parameters adlsGen2 = new AdlsGen2CloudStorageV4Parameters();
        String accountName = azureProperties.getCloudstorage().getAccountName();
        String accountKey = azureProperties.getCloudstorage().getAccountKey();
        adlsGen2.setAccountKey(accountKey);
        adlsGen2.setAccountName(accountName);
        request.setAdlsGen2(adlsGen2);
        return request;
    }

}
