package com.sequenceiq.it.cloudbreak.newway.cloud;

import com.sequenceiq.cloudbreak.api.endpoint.v4.filesystems.requests.CloudStorageRequest;
import com.sequenceiq.cloudbreak.api.endpoint.v4.filesystems.requests.gcs.GcsCloudStorageParameters;
import com.sequenceiq.it.cloudbreak.newway.RdsConfig;
import com.sequenceiq.it.cloudbreak.newway.TestParameter;
import com.sequenceiq.it.cloudbreak.parameters.RequiredInputParameters.Gcp.Database.Hive;
import com.sequenceiq.it.cloudbreak.parameters.RequiredInputParameters.Gcp.Database.Ranger;
import com.sequenceiq.it.cloudbreak.parameters.RequiredInputParameters.Gcp.Storage;

import static com.sequenceiq.cloudbreak.api.endpoint.v4.database.base.DatabaseType.HIVE;
import static com.sequenceiq.cloudbreak.api.endpoint.v4.database.base.DatabaseType.RANGER;
import static com.sequenceiq.it.cloudbreak.filesystem.CloudStorageTypePathPrefix.GCS;

public class GcpResourceHelper extends ResourceHelper<GcsCloudStorageParameters> {

    private static final String RANGER_RDS_ENTITY_ID = "GCP_RANGER_DB_CONFIG";

    private static final String HIVE_RDS_ENTITY_ID = "GCP_HIVE_DB_CONFIG";

    GcpResourceHelper(TestParameter testParameter) {
        super(testParameter);
    }

    GcpResourceHelper(TestParameter testParameter, String postfix) {
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
        request.setGcs(getCloudStorage());
        request.setLocations(defaultDatalakeStorageLocations(GCS, getTestParameter().get(Storage.BUCKET_NAME)));
        return request;
    }

    @Override
    public CloudStorageRequest getCloudStorageRequestForAttachedCluster() {
        return getCloudStorageForAttachedCluster(GCS, getTestParameter().get(Storage.BUCKET_NAME), getCloudStorage());
    }

    @Override
    protected GcsCloudStorageParameters getCloudStorage() {
        return new GcsCloudStorageParameters();
    }
}
