package com.sequenceiq.it.cloudbreak.util.aws.amazonrgt;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.it.cloudbreak.util.aws.AwsResources;
import com.sequenceiq.it.cloudbreak.util.aws.amazonrgt.action.AwsResourceGroupTaggingActions;

@Component
public class AmazonResourceGroupTaggingUtil {

    private static final Logger LOGGER = LoggerFactory.getLogger(AmazonResourceGroupTaggingUtil.class);

    @Inject
    private AwsResourceGroupTaggingActions awsResourceGroupTaggingActions;

    private AmazonResourceGroupTaggingUtil() {
    }

    public Map<String, Map<String, String>> getAllResourcesAndTagsForEnvironment(String envCrn) {
        LOGGER.info("TAG VALIDATION: Getting tags for all resource types for env CRN: {}", envCrn);
        List<AwsResources> allResources = Arrays.asList(AwsResources.values());
        Map<String, Map<String, String>> result =
                awsResourceGroupTaggingActions.getResourcesAndTagsByEnvironmentCrnAndResourceTypes(envCrn, allResources);
        Arrays.stream(AwsResources.values()).forEach(resourceType -> {
            long count = countArnsMatchingType(result.keySet(), resourceType);
            LOGGER.info("TAG VALIDATION: Got {} resources of type {} ({}) for the Environment",
                    count, resourceType.name(), resourceType.getTaggingApiType());
        });
        LOGGER.info("TAG VALIDATION: Got {} total resources for env CRN: {}", result.size(), envCrn);
        return result;
    }

    private long countArnsMatchingType(Set<String> arns, AwsResources resourceType) {
        String[] parts = resourceType.getTaggingApiType().split(":", 2);
        String service = parts[0];
        String resourceSubtype = parts[1];
        return arns.stream()
                .filter(arn -> arn.contains(":" + service + ":") &&
                        (arn.contains(":" + resourceSubtype + ":") || arn.contains(":" + resourceSubtype + "/")))
                .count();
    }
}
