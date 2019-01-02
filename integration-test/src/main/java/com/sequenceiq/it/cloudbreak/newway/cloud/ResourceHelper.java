package com.sequenceiq.it.cloudbreak.newway.cloud;

import java.util.LinkedHashSet;
import java.util.Optional;
import java.util.Set;

import com.sequenceiq.cloudbreak.api.endpoint.v4.database.base.DatabaseType;
import com.sequenceiq.cloudbreak.api.endpoint.v4.database.requests.DatabaseV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.parameter.storage.CloudStorageV4Parameters;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.cluster.storage.CloudStorageV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.cluster.storage.location.StorageLocationV4Request;
import com.sequenceiq.it.cloudbreak.filesystem.CloudStorageTypePathPrefix;
import com.sequenceiq.it.cloudbreak.newway.LdapConfig;
import com.sequenceiq.it.cloudbreak.newway.MissingExpectedParameterException;
import com.sequenceiq.it.cloudbreak.newway.RdsConfig;
import com.sequenceiq.it.cloudbreak.newway.TestParameter;

public abstract class ResourceHelper<T extends CloudStorageV4Parameters> {

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

    public abstract CloudStorageV4Request getCloudStorageRequestForDatalake();

    public abstract CloudStorageV4Request getCloudStorageRequestForAttachedCluster();

    protected abstract T getCloudStorage();

    protected TestParameter getTestParameter() {
        return testParameter;
    }

    protected DatabaseV4Request createRdsRequestWithProperties(String configName, String userName, String password,
            String connectionUrl, DatabaseType databaseType) {
        var request = new DatabaseV4Request();
        request.setName(getParam(configName));
        request.setConnectionUserName(getParam(userName));
        request.setConnectionPassword(getParam(password));
        request.setConnectionURL(getParam(connectionUrl));
        request.setType(databaseType.name());
        return request;
    }

    protected Set<StorageLocationV4Request> defaultDatalakeStorageLocations(CloudStorageTypePathPrefix type, String parameterToInsert) {
        Set<StorageLocationV4Request> request = new LinkedHashSet<>(2);
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

    protected CloudStorageV4Request getCloudStorageForAttachedCluster(CloudStorageTypePathPrefix type, String parameterToInsert,
            CloudStorageV4Parameters cloudStorageParameterInstance) {
        var request = new CloudStorageV4Request();
        var locations = new LinkedHashSet<StorageLocationV4Request>(1);
        locations.add(
                createLocation(
                        String.format("%s://%s/attached/apps/hive/warehouse", type.getPrefix(), parameterToInsert),
                        "hive-site",
                        "hive.metastore.warehouse.dir"));
        request.setLocations(locations);
        type.setParameterForRequest(request, cloudStorageParameterInstance);
        return request;
    }

    private StorageLocationV4Request createLocation(String value, String propertyFile, String propertyName) {
        var location = new StorageLocationV4Request();
        location.setValue(value);
        location.setPropertyFile(propertyFile);
        location.setPropertyName(propertyName);
        return location;
    }

    private String getParam(String key) {
        return Optional.ofNullable(testParameter.get(key)).orElseThrow(() -> new MissingExpectedParameterException(key));
    }
}
