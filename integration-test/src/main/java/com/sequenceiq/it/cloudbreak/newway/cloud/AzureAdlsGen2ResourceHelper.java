package com.sequenceiq.it.cloudbreak.newway.cloud;

import static com.sequenceiq.cloudbreak.api.endpoint.v4.database.base.DatabaseType.HIVE;
import static com.sequenceiq.cloudbreak.api.endpoint.v4.database.base.DatabaseType.RANGER;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.parameter.storage.AdlsGen2CloudStorageV4Parameters;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.cluster.storage.CloudStorageV4Request;
import com.sequenceiq.it.cloudbreak.filesystem.CloudStorageTypePathPrefix;
import com.sequenceiq.it.cloudbreak.newway.RdsConfig;
import com.sequenceiq.it.cloudbreak.newway.TestParameter;
import com.sequenceiq.it.cloudbreak.parameters.RequiredInputParameters.Azure.Database.Hive;
import com.sequenceiq.it.cloudbreak.parameters.RequiredInputParameters.Azure.Database.Ranger;

public class AzureAdlsGen2ResourceHelper extends ResourceHelper<AdlsGen2CloudStorageV4Parameters> {

    private static final String RANGER_RDS_ENTITY_ID = "AZURE_RANGER_DB_CONFIG";

    private static final String HIVE_RDS_ENTITY_ID = "AZURE_HIVE_DB_CONFIG";

    AzureAdlsGen2ResourceHelper(TestParameter testParameter) {
        super(testParameter);
    }

    AzureAdlsGen2ResourceHelper(TestParameter testParameter, String postfix) {
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
        request.setAdlsGen2(getCloudStorage());
        return request;
    }

    @Override
    public CloudStorageV4Request getCloudStorageRequestForAttachedCluster() {
        return getCloudStorageForAttachedCluster(CloudStorageTypePathPrefix.ADLS_GEN_2, getTestParameter().get("cloudStorageName"), getCloudStorage());
    }

    @Override
    public AdlsGen2CloudStorageV4Parameters getCloudStorage() {
        var parameters = new AdlsGen2CloudStorageV4Parameters();
        parameters.setAccountKey(getTestParameter().get("integrationtest.filesystemconfig.accountKeyAbfs"));
        parameters.setAccountName(getTestParameter().get("integrationtest.filesystemconfig.accountNameAbfs"));
        return parameters;
    }
}