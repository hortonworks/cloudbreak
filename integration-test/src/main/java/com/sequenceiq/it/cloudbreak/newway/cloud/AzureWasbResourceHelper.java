package com.sequenceiq.it.cloudbreak.newway.cloud;

import static com.sequenceiq.cloudbreak.api.endpoint.v4.database.base.DatabaseType.HIVE;
import static com.sequenceiq.cloudbreak.api.endpoint.v4.database.base.DatabaseType.RANGER;
import static com.sequenceiq.it.cloudbreak.filesystem.CloudStorageTypePathPrefix.WASB;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.parameter.storage.WasbCloudStorageV4Parameters;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.cluster.storage.CloudStorageV4Request;
import com.sequenceiq.it.cloudbreak.newway.RdsConfig;
import com.sequenceiq.it.cloudbreak.newway.TestParameter;
import com.sequenceiq.it.cloudbreak.parameters.RequiredInputParameters.Azure.Database.Hive;
import com.sequenceiq.it.cloudbreak.parameters.RequiredInputParameters.Azure.Database.Ranger;
import com.sequenceiq.it.cloudbreak.parameters.RequiredInputParameters.Azure.Storage.Wasb;

public class AzureWasbResourceHelper extends ResourceHelper<WasbCloudStorageV4Parameters> {

    private static final String RANGER_RDS_ENTITY_ID = "AZURE_RANGER_DB_CONFIG";

    private static final String HIVE_RDS_ENTITY_ID = "AZURE_HIVE_DB_CONFIG";

    AzureWasbResourceHelper(TestParameter testParameter) {
        super(testParameter);
    }

    AzureWasbResourceHelper(TestParameter testParameter, String postfix) {
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
    public CloudStorageV4Request getCloudStorageRequestForDatalake() {
        var request = new CloudStorageV4Request();
        request.setWasb(getCloudStorage());
        request.setLocations(defaultDatalakeStorageLocations(WASB, getTestParameter().get(Wasb.STORAGE_NAME)));
        return request;
    }

    @Override
    public CloudStorageV4Request getCloudStorageRequestForAttachedCluster() {
        return getCloudStorageForAttachedCluster(WASB, getTestParameter().get(Wasb.STORAGE_NAME), getCloudStorage());
    }

    @Override
    protected WasbCloudStorageV4Parameters getCloudStorage() {
        var params = new WasbCloudStorageV4Parameters();
        params.setAccountName(getTestParameter().get(Wasb.ACCOUNT));
        params.setAccountKey(getTestParameter().get(Wasb.ACCESS_KEY));
        params.setSecure(true);
        return params;
    }

}
