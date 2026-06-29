package com.sequenceiq.it.cloudbreak.util.aws.amazonrgt.action;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.it.cloudbreak.util.aws.AwsResources;
import com.sequenceiq.it.cloudbreak.util.aws.amazonrgt.client.AwsResourceGroupTaggingClient;

import software.amazon.awssdk.services.resourcegroupstaggingapi.ResourceGroupsTaggingApiClient;
import software.amazon.awssdk.services.resourcegroupstaggingapi.model.GetResourcesRequest;
import software.amazon.awssdk.services.resourcegroupstaggingapi.model.GetResourcesResponse;
import software.amazon.awssdk.services.resourcegroupstaggingapi.model.Tag;
import software.amazon.awssdk.services.resourcegroupstaggingapi.model.TagFilter;

@Component
public class AwsResourceGroupTaggingActions extends AwsResourceGroupTaggingClient {

    private static final Logger LOGGER = LoggerFactory.getLogger(AwsResourceGroupTaggingActions.class);

    private static final String CLOUDERA_ENVIRONMENT_RESOURCE_NAME = "Cloudera-Environment-Resource-Name";

    public Map<String, Map<String, String>> getResourcesAndTagsByEnvironmentCrnAndResourceTypes(String envCrn, List<AwsResources> resourceTypes) {
        Map<String, Map<String, String>> resourcesAndTags = new HashMap<>();
        try (ResourceGroupsTaggingApiClient taggingClient = buildTaggingClient()) {
            String paginationToken = null;
            do {
                GetResourcesResponse response = taggingClient.getResources(GetResourcesRequest.builder()
                        .tagFilters(TagFilter.builder()
                                .key(CLOUDERA_ENVIRONMENT_RESOURCE_NAME)
                                .values(envCrn)
                                .build())
                        .resourceTypeFilters(resourceTypes.stream()
                                .map(AwsResources::getTaggingApiType)
                                .distinct()
                                .collect(Collectors.toList()))
                        .paginationToken(paginationToken)
                        .build());
                response.resourceTagMappingList().forEach(mapping ->
                        resourcesAndTags.put(mapping.resourceARN(), mapping.tags().stream()
                                .collect(Collectors.toMap(Tag::key, Tag::value)))
                );
                paginationToken = response.paginationToken();
            } while (paginationToken != null && !paginationToken.isEmpty());
        }
        return resourcesAndTags;
    }
}
