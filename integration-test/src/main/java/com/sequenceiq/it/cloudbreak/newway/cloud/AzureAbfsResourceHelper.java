package com.sequenceiq.it.cloudbreak.newway.cloud;

import static com.sequenceiq.cloudbreak.api.model.rds.RdsType.HIVE;
import static com.sequenceiq.cloudbreak.api.model.rds.RdsType.RANGER;

import com.sequenceiq.cloudbreak.api.model.v2.CloudStorageRequest;
import com.sequenceiq.cloudbreak.api.model.v2.filesystem.AbfsCloudStorageParameters;
import com.sequenceiq.it.cloudbreak.filesystem.CloudStorageTypePathPrefix;
import com.sequenceiq.it.cloudbreak.newway.RdsConfig;
import com.sequenceiq.it.cloudbreak.newway.TestParameter;
import com.sequenceiq.it.cloudbreak.parameters.RequiredInputParameters.Azure.Database.Hive;
import com.sequenceiq.it.cloudbreak.parameters.RequiredInputParameters.Azure.Database.Ranger;

public class AzureAbfsResourceHelper extends ResourceHelper<AbfsCloudStorageParameters> {

    private static final String RANGER_RDS_ENTITY_ID = "AZURE_RANGER_DB_CONFIG";

    private static final String HIVE_RDS_ENTITY_ID = "AZURE_HIVE_DB_CONFIG";

    AzureAbfsResourceHelper(TestParameter testParameter) {
        super(testParameter);
    }

    AzureAbfsResourceHelper(TestParameter testParameter, String postfix) {
        super(testParameter, postfix);
    }

    @Override
    public RdsConfig aValidHiveDatabase() {
        var hive = RdsConfig.isCreated(HIVE_RDS_ENTITY_ID);
        hive.setRequest(createRdsRequestWithProperties(Hive.CONFIG_NAME, Hive.USER_NAME_KEY, Hive.PASSWORD_KEY, Hive.CONNECTION_URL_KEY, HIVE));
        return hive;
    }

    @Override
    public RdsConfig aValidRangerDatabase() {
        var ranger = RdsConfig.isCreated(RANGER_RDS_ENTITY_ID);
        ranger.setRequest(createRdsRequestWithProperties(Ranger.CONFIG_NAME, Ranger.USER_NAME_KEY, Ranger.PASSWORD_KEY, Ranger.CONNECTION_URL_KEY, RANGER));
        return ranger;
    }

    @Override
    public CloudStorageRequest getCloudStorageRequestForDatalake() {
        var request = new CloudStorageRequest();
        request.setAbfs(getCloudStorage());
        request.setLocations(defaultDatalakeStorageLocations(CloudStorageTypePathPrefix.ABFS, getTestParameter().get("cloudStorageName")));
        return request;
    }

    @Override
    public CloudStorageRequest getCloudStorageRequestForAttachedCluster() {
        return getCloudStorageForAttachedCluster(CloudStorageTypePathPrefix.ABFS, getTestParameter().get("cloudStorageName"), getCloudStorage());
    }

    @Override
    public AbfsCloudStorageParameters getCloudStorage() {
        var parameters = new AbfsCloudStorageParameters();
        parameters.setAccountKey(getTestParameter().get("integrationtest.filesystemconfig.accountKeyAbfs"));
        parameters.setAccountName(getTestParameter().get("integrationtest.filesystemconfig.accountNameAbfs"));
        return parameters;
    }
}