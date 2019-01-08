package com.sequenceiq.it.cloudbreak.newway.cloud;

import com.sequenceiq.cloudbreak.api.endpoint.v4.database.requests.DatabaseV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.database.base.DatabaseType;
import com.sequenceiq.cloudbreak.api.model.v2.CloudStorageRequest;
import com.sequenceiq.cloudbreak.api.model.v2.StorageLocationRequest;
import com.sequenceiq.cloudbreak.api.model.v2.filesystem.CloudStorageParameters;
import com.sequenceiq.it.cloudbreak.filesystem.CloudStorageTypePathPrefix;
import com.sequenceiq.it.cloudbreak.newway.LdapConfig;
import com.sequenceiq.it.cloudbreak.newway.MissingExpectedParameterException;
import com.sequenceiq.it.cloudbreak.newway.RdsConfig;
import com.sequenceiq.it.cloudbreak.newway.TestParameter;

import java.util.LinkedHashSet;
import java.util.Optional;
import java.util.Set;

public abstract class ResourceHelper<T extends CloudStorageParameters> {

    private final TestParameter testParameter;

    private final String postfix;

    ResourceHelper(TestParameter testParameter) {
        this(testParameter, "");
    }

    ResourceHelper(TestParameter testParameter, String postfix) {
        this.testParameter = testParameter;
        this.postfix = postfix;
    }

    public LdapConfig aValidLdap() {
        return LdapConfig.isCreatedWithParametersAndName(testParameter, getLdapConfigName());
    }

    public String getLdapConfigName() {
        var ldapName = testParameter.get("ldapConfigName");
        return ldapName == null ? String.format("ldapconfig%s", postfix) : String.format("%s%s", ldapName, postfix);
    }

    public abstract RdsConfig aValidHiveDatabase();

    public abstract RdsConfig aValidRangerDatabase();

    public abstract CloudStorageRequest getCloudStorageRequestForDatalake();

    public abstract CloudStorageRequest getCloudStorageRequestForAttachedCluster();

    protected abstract T getCloudStorage();

    protected TestParameter getTestParameter() {
        return testParameter;
    }

    protected DatabaseV4Request createRdsRequestWithProperties(String configName, String userName, String password, String connectionUrl, DatabaseType databaseType) {
        var request = new DatabaseV4Request();
        request.setName(getParam(configName));
        request.setConnectionUserName(getParam(userName));
        request.setConnectionPassword(getParam(password));
        request.setConnectionURL(getParam(connectionUrl));
        request.setType(databaseType.name());
        return request;
    }

    protected Set<StorageLocationRequest> defaultDatalakeStorageLocations(CloudStorageTypePathPrefix type, String parameterToInsert) {
        Set<StorageLocationRequest> request = new LinkedHashSet<>(2);
        request.add(createLocation(
                String.format("%s://%s/apps/hive/warehouse", type.getPrefix(), parameterToInsert),
                "hive-site",
                "hive.metastore.warehouse.dir"));
        request.add(createLocation(
                String.format("%s://%s/apps/ranger/audit", type.getPrefix(), parameterToInsert),
                "ranger-env",
                "xasecure.audit.destination.hdfs.dir"));
        return request;
    }

    protected CloudStorageRequest getCloudStorageForAttachedCluster(CloudStorageTypePathPrefix type, String parameterToInsert,
                    CloudStorageParameters cloudStorageParameterInstance) {
        var request = new CloudStorageRequest();
        var locations = new LinkedHashSet<StorageLocationRequest>(1);
        locations.add(
                createLocation(
                        String.format("%s://%s/attached/apps/hive/warehouse", type.getPrefix(), parameterToInsert),
                        "hive-site",
                        "hive.metastore.warehouse.dir"));
        request.setLocations(locations);
        type.setParameterForRequest(request, cloudStorageParameterInstance);
        return request;
    }

    private StorageLocationRequest createLocation(String value, String propertyFile, String propertyName) {
        var location = new StorageLocationRequest();
        location.setValue(value);
        location.setPropertyFile(propertyFile);
        location.setPropertyName(propertyName);
        return location;
    }

    private String getParam(String key) {
        return Optional.ofNullable(testParameter.get(key)).orElseThrow(() -> new MissingExpectedParameterException(key));
    }
}
