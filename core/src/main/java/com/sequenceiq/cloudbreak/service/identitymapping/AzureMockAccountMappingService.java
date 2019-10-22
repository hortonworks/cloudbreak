package com.sequenceiq.cloudbreak.service.identitymapping;

import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.init.CloudPlatformConnectors;
import com.sequenceiq.cloudbreak.common.exception.CloudbreakServiceException;
import com.sequenceiq.cloudbreak.converter.spi.CredentialToCloudCredentialConverter;
import com.sequenceiq.cloudbreak.dto.credential.Credential;

@Component
public class AzureMockAccountMappingService {

    public static final String MSI_RESOURCE_GROUP_NAME = "msi";

    private static final String FIXED_MANAGED_IDENTITY = "/subscriptions/${subscriptionId}/resourceGroups/${resourceGroupId}/" +
            "providers/Microsoft.ManagedIdentity/userAssignedIdentities/mock-idbroker-admin-identity";

    private static final Map<String, String> MOCK_IDBROKER_USER_MAPPINGS = Map.ofEntries(
            Map.entry("accumulo", FIXED_MANAGED_IDENTITY),
            Map.entry("ambari-qa", FIXED_MANAGED_IDENTITY),
            Map.entry("ams", FIXED_MANAGED_IDENTITY),
            Map.entry("atlas", FIXED_MANAGED_IDENTITY),
            Map.entry("docker", FIXED_MANAGED_IDENTITY),
            Map.entry("dpprofiler", FIXED_MANAGED_IDENTITY),
            Map.entry("falcon", FIXED_MANAGED_IDENTITY),
            Map.entry("flume", FIXED_MANAGED_IDENTITY),
            Map.entry("hbase", FIXED_MANAGED_IDENTITY),
            Map.entry("hcat", FIXED_MANAGED_IDENTITY),
            Map.entry("hdfs", FIXED_MANAGED_IDENTITY),
            Map.entry("hive", FIXED_MANAGED_IDENTITY),
            Map.entry("httpfs", FIXED_MANAGED_IDENTITY),
            Map.entry("hue", FIXED_MANAGED_IDENTITY),
            Map.entry("impala", FIXED_MANAGED_IDENTITY),
            Map.entry("infra-solr", FIXED_MANAGED_IDENTITY),
            Map.entry("kafka", FIXED_MANAGED_IDENTITY),
            Map.entry("kms", FIXED_MANAGED_IDENTITY),
            Map.entry("knox", FIXED_MANAGED_IDENTITY),
            Map.entry("kudu", FIXED_MANAGED_IDENTITY),
            Map.entry("livy", FIXED_MANAGED_IDENTITY),
            Map.entry("mahout", FIXED_MANAGED_IDENTITY),
            Map.entry("mapred", FIXED_MANAGED_IDENTITY),
            Map.entry("oozie", FIXED_MANAGED_IDENTITY),
            Map.entry("ranger", FIXED_MANAGED_IDENTITY),
            Map.entry("sentry", FIXED_MANAGED_IDENTITY),
            Map.entry("slider", FIXED_MANAGED_IDENTITY),
            Map.entry("solr", FIXED_MANAGED_IDENTITY),
            Map.entry("spark", FIXED_MANAGED_IDENTITY),
            Map.entry("sqoop", FIXED_MANAGED_IDENTITY),
            Map.entry("storm", FIXED_MANAGED_IDENTITY),
            Map.entry("tez", FIXED_MANAGED_IDENTITY),
            Map.entry("yarn", FIXED_MANAGED_IDENTITY),
            Map.entry("yarn-ats", FIXED_MANAGED_IDENTITY),
            Map.entry("ycloudadm", FIXED_MANAGED_IDENTITY),
            Map.entry("zeppelin", FIXED_MANAGED_IDENTITY),
            Map.entry("zookeeper", FIXED_MANAGED_IDENTITY)
    );

    @Inject
    private CloudPlatformConnectors cloudPlatformConnectors;

    @Inject
    private CredentialToCloudCredentialConverter credentialConverter;

    public Map<String, String> getGroupMappings(String resourceGroup, Credential credential, String adminGroupName) {
        String subscriptionId = credential.getAzure().getSubscriptionId();
        if (StringUtils.isNotEmpty(adminGroupName)) {
            return replacePlaceholders(getGroupMappings(adminGroupName), resourceGroup, subscriptionId);
        } else {
            throw new CloudbreakServiceException("Failed to get group mappings because of missing adminGroupName");
        }
    }

    public Map<String, String> getUserMappings(String resourceGroup, Credential credential) {
        String subscriptionId = credential.getAzure().getSubscriptionId();
        return replacePlaceholders(MOCK_IDBROKER_USER_MAPPINGS, resourceGroup, subscriptionId);
    }

    private Map<String, String> replacePlaceholders(Map<String, String> mapping, String resourceGroup, String subscriptionId) {
        return mapping.entrySet()
                .stream()
                .map(e -> Map.entry(e.getKey(), e.getValue().
                        replace("${subscriptionId}", subscriptionId).
                        replace("${resourceGroupId}", resourceGroup)))
                .collect(Collectors.toMap(Entry::getKey, Entry::getValue));
    }

    private Map<String, String> getGroupMappings(String adminGroupName) {
        return Map.ofEntries(
                Map.entry(adminGroupName, FIXED_MANAGED_IDENTITY)
        );
    }

}
