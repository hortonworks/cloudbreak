package com.sequenceiq.it.cloudbreak.newway.cloud;

import com.sequenceiq.cloudbreak.api.model.v2.CloudStorageRequest;
import com.sequenceiq.cloudbreak.api.model.v2.filesystem.S3CloudStorageParameters;
import com.sequenceiq.it.cloudbreak.newway.RdsConfig;
import com.sequenceiq.it.cloudbreak.newway.TestParameter;
import com.sequenceiq.it.cloudbreak.parameters.RequiredInputParameters.Aws.Database.Hive;
import com.sequenceiq.it.cloudbreak.parameters.RequiredInputParameters.Aws.Database.Ranger;
import com.sequenceiq.it.cloudbreak.parameters.RequiredInputParameters.Aws.Storage;

import static com.sequenceiq.cloudbreak.api.endpoint.v4.database.base.DatabaseType.HIVE;
import static com.sequenceiq.cloudbreak.api.endpoint.v4.database.base.DatabaseType.RANGER;
import static com.sequenceiq.it.cloudbreak.filesystem.CloudStorageTypePathPrefix.S3;

public class AwsResourceHelper extends ResourceHelper<S3CloudStorageParameters> {

    private static final String RANGER_RDS_ENTITY_ID = "AWS_RANGER_DB_CONFIG";

    private static final String HIVE_RDS_ENTITY_ID = "AWS_HIVE_DB_CONFIG";

    AwsResourceHelper(TestParameter testParameter) {
        super(testParameter);
    }

    AwsResourceHelper(TestParameter testParameter, String postfix) {
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
        request.setS3(getCloudStorage());
        request.setLocations(defaultDatalakeStorageLocations(S3, getTestParameter().get(Storage.S3_BUCKET_NAME)));
        return request;
    }

    @Override
    public CloudStorageRequest getCloudStorageRequestForAttachedCluster() {
        return getCloudStorageForAttachedCluster(S3, getTestParameter().get(Storage.S3_BUCKET_NAME), getCloudStorage());
    }

    @Override
    protected S3CloudStorageParameters getCloudStorage() {
        return new S3CloudStorageParameters();
    }
}
