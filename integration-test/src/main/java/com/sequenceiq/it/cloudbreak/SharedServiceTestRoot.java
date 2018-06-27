package com.sequenceiq.it.cloudbreak;

import com.sequenceiq.cloudbreak.api.model.CredentialResponse;
import com.sequenceiq.cloudbreak.api.model.rds.RDSConfigResponse;
import com.sequenceiq.cloudbreak.api.model.v2.CloudStorageRequest;
import com.sequenceiq.cloudbreak.api.model.v2.StorageLocationRequest;
import com.sequenceiq.cloudbreak.api.model.v2.filesystem.AdlsCloudStorageParameters;
import com.sequenceiq.cloudbreak.api.model.v2.filesystem.CloudStorageParameters;
import com.sequenceiq.cloudbreak.api.model.v2.filesystem.GcsCloudStorageParameters;
import com.sequenceiq.cloudbreak.api.model.v2.filesystem.S3CloudStorageParameters;
import com.sequenceiq.cloudbreak.api.model.v2.filesystem.WasbCloudStorageParameters;
import com.sequenceiq.it.cloudbreak.newway.CloudbreakClient;
import com.sequenceiq.it.cloudbreak.newway.CloudbreakTest;
import com.sequenceiq.it.cloudbreak.newway.Credential;
import com.sequenceiq.it.cloudbreak.newway.RdsConfig;
import com.sequenceiq.it.cloudbreak.newway.cloud.CloudProvider;
import org.slf4j.Logger;
import org.testng.annotations.AfterSuite;

import javax.annotation.Nonnull;
import java.util.LinkedHashSet;
import java.util.Set;

public abstract class SharedServiceTestRoot extends CloudbreakTest {

    private static final String ATTACHED_CLUSTER_BLUEPRINT_NAME = "Data Science: Apache Spark 2, Apache Zeppelin";

    private static final String DATALAKE_BLUEPRINT_NAME = "Data Lake: Apache Ranger, Apache Hive Metastore";

    private static final String DATALAKE_CLUSTER_NAME = "datalake-cluster-%s";

    private static final String ATTACHED_CLUSTER_NAME = "v3-attached-cluster-%s";

    private final Logger logger;

    private final String implementation;

    protected SharedServiceTestRoot(@Nonnull Logger logger, String implementation) {
        this.logger = logger;
        this.implementation = implementation;
    }

    public abstract void testADatalakeClusterCreation(CloudProvider cloudProvider, String clusterName, String blueprintName) throws Exception;

    public abstract void testClusterAttachedToDatalakeCluster(CloudProvider cloudProvider, String clusterName, String datalakeClusterName,
                    String blueprintName) throws Exception;

    public abstract void testTerminateAttachedCluster(CloudProvider cloudProvider, String attachedClusterName, String datalakeClusterName,
                    String blueprintName) throws Exception;

    public abstract void testTerminateDatalakeCluster(CloudProvider cloudProvider, String attachedClusterName, String datalakeClusterName,
                    String blueprintName) throws Exception;

    public abstract Object[][] providerClusterNameForDatalake();

    public abstract Object[][] providerClusterNameAndBlueprintForAttachedCluster();

    @AfterSuite
    public void after() throws Exception {
        cleanUpCredentials();
        cleanUpRdsConfigs();
    }

    protected void cleanUpCredentials() throws Exception {
        given(CloudbreakClient.isCreated());
        when(Credential.getAll());
        then(Credential.assertThis((credential, testContext) -> {
            Set<CredentialResponse> responses = credential.getResponses();
            for (CredentialResponse response : responses) {
                if (response.getName().startsWith("autotesting")) {
                    try {
                        given(Credential.request().withName(response.getName()));
                        when(Credential.delete());
                    } catch (Exception e) {
                        logger.error("Error occured during cleanup: " + e.getMessage());
                    }
                }
            }
        }));
    }

    protected void cleanUpRdsConfigs() throws Exception {
        given(CloudbreakClient.isCreated());
        when(RdsConfig.getAll());
        then(RdsConfig.assertThis((rdsConfig, testContext) -> {
            Set<RDSConfigResponse> responses = rdsConfig.getResponses();
            for (RDSConfigResponse response : responses) {
                if ((response.getName().startsWith("hive") || response.getName().startsWith("ranger"))
                        && (response.getName().endsWith(implementation))) {
                    try {
                        given(RdsConfig.request().withName(response.getName()));
                        when(RdsConfig.delete());
                    } catch (Exception e) {
                        logger.error("Error occured during cleanup: " + e.getMessage());
                    }
                }
            }
        }));
    }

    protected StorageLocationRequest createLocation(String value, String propertyFile, String propertyName) {
        StorageLocationRequest location = new StorageLocationRequest();
        location.setValue(value);
        location.setPropertyFile(propertyFile);
        location.setPropertyName(propertyName);
        return location;
    }

    protected String getDatalakeClusterName() {
        return String.format(DATALAKE_CLUSTER_NAME, implementation);
    }

    protected String getAttachedClusterName() {
        return String.format(ATTACHED_CLUSTER_NAME, implementation);
    }

    protected String getDatalakeBlueprintName() {
        return DATALAKE_BLUEPRINT_NAME;
    }

    protected String getAttachedClusterBlueprintName() {
        return ATTACHED_CLUSTER_BLUEPRINT_NAME;
    }

    protected Set<StorageLocationRequest> defaultDatalakeStorageLocations(String parameterToInsert) {
        Set<StorageLocationRequest> request = new LinkedHashSet<>(2);
        request.add(createLocation(
                String.format("gs://%s/apps/hive/warehouse", parameterToInsert),
                "hive-site",
                "hive.metastore.warehouse.dir"));
        request.add(createLocation(
                String.format("gs://%s/apps/ranger/audit", parameterToInsert),
                "ranger-env",
                "xasecure.audit.destination.hdfs.dir"));
        return request;
    }

    protected CloudStorageRequest getCloudStorageForAttachedCluster(String parameterToInsert, CloudStorageParameters emptyType) {
        CloudStorageRequest request = new CloudStorageRequest();
        Set<StorageLocationRequest> locations = new LinkedHashSet<>(1);
        locations.add(
                createLocation(
                        String.format("s3a://%s/attached/apps/hive/warehouse", parameterToInsert),
                        "hive-site",
                        "hive.metastore.warehouse.dir"));
        request.setLocations(locations);
        if (emptyType instanceof AdlsCloudStorageParameters) {
            request.setAdls((AdlsCloudStorageParameters) emptyType);
        } else if (emptyType instanceof WasbCloudStorageParameters) {
            request.setWasb((WasbCloudStorageParameters) emptyType);
        } else if (emptyType instanceof S3CloudStorageParameters) {
            request.setS3((S3CloudStorageParameters) emptyType);
        } else if (emptyType instanceof GcsCloudStorageParameters) {
            request.setGcs((GcsCloudStorageParameters) emptyType);
        }
        return request;
    }
}
