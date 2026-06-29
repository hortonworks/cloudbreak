package com.sequenceiq.it.cloudbreak.assertion.util;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.StackV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.instancegroup.instancemetadata.InstanceMetaDataV4Response;
import com.sequenceiq.common.api.tag.response.MapToTaggedResponseAdapter;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.instance.InstanceMetaDataResponse;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.describe.DescribeFreeIpaResponse;
import com.sequenceiq.it.cloudbreak.assertion.Assertion;
import com.sequenceiq.it.cloudbreak.cloud.v4.CloudProviderProxy;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.distrox.DistroXTestDto;
import com.sequenceiq.it.cloudbreak.dto.environment.EnvironmentTestDto;
import com.sequenceiq.it.cloudbreak.dto.sdx.SdxInternalTestDto;
import com.sequenceiq.it.cloudbreak.exception.TestFailException;
import com.sequenceiq.it.cloudbreak.microservice.CloudbreakClient;
import com.sequenceiq.it.cloudbreak.microservice.EnvironmentClient;
import com.sequenceiq.it.cloudbreak.microservice.FreeIpaClient;
import com.sequenceiq.it.cloudbreak.microservice.SdxClient;
import com.sequenceiq.it.cloudbreak.util.CloudFunctionality;
import com.sequenceiq.it.util.TagsUtil;
import com.sequenceiq.sdx.api.model.SdxClusterDetailResponse;

@Service
public class CloudProviderSideTagAssertion {

    private static final Logger LOGGER = LoggerFactory.getLogger(CloudProviderSideTagAssertion.class);

    private final CloudProviderProxy cloudProviderProxy;

    private final TagsUtil tagsUtil;

    public CloudProviderSideTagAssertion(CloudProviderProxy cloudProviderProxy, TagsUtil tagsUtil) {
        this.cloudProviderProxy = cloudProviderProxy;
        this.tagsUtil = tagsUtil;
    }

    public Assertion<EnvironmentTestDto, EnvironmentClient> verifyEnvironmentTags(Map<String, String> customTags) {
        return (testContext, testDto, client) -> {
            String envCrn = testDto.getResponse().getCrn();
            DescribeFreeIpaResponse freeIpaResponse = testContext.getMicroserviceClient(FreeIpaClient.class)
                    .getDefaultClient(testContext)
                    .getFreeIpaV1Endpoint()
                    .describe(envCrn);

            List<String> instanceIds = freeIpaResponse.getInstanceGroups().stream()
                    .flatMap(ig -> ig.getMetaData().stream())
                    .map(InstanceMetaDataResponse::getInstanceId)
                    .collect(Collectors.toList());

            LOGGER.info(" TAG VALIDATION:\n Environment: {}\n FreeIPA: {}\n instance IDs: {}.\n", testDto.getName(), freeIpaResponse.getName(), instanceIds);
            verifyTags(instanceIds, customTags, testContext, freeIpaResponse.getName());

            return testDto;
        };
    }

    public Assertion<SdxInternalTestDto, SdxClient> verifyInternalSdxTags(Map<String, String> customTags) {
        return (testContext, testDto, client) -> {
            String sdxCrn = testDto.getResponse().getCrn();
            SdxClusterDetailResponse sdxClusterDetailResponse = testContext.getMicroserviceClient(SdxClient.class)
                    .getDefaultClient(testContext)
                    .sdxEndpoint()
                    .getDetailByCrn(sdxCrn, Collections.emptySet());

            List<String> instanceIds = sdxClusterDetailResponse.getStackV4Response().getInstanceGroups().stream()
                    .flatMap(ig -> ig.getMetadata().stream())
                    .map(InstanceMetaDataV4Response::getInstanceId)
                    .collect(Collectors.toList());

            LOGGER.info(" TAG VALIDATION:\n SDX: {}\n instance IDs: {}.\n", testDto.getName(), instanceIds);
            verifyTags(instanceIds, customTags, testContext, testDto.getName());

            return testDto;
        };
    }

    public Assertion<DistroXTestDto, CloudbreakClient> verifyDistroxTags(Map<String, String> customTags) {
        return (testContext, testDto, client) -> {
            String distroxCrn = testDto.getResponse().getCrn();
            StackV4Response stackV4Response = testContext.getMicroserviceClient(CloudbreakClient.class)
                    .getDefaultClient(testContext)
                    .distroXV1Endpoint()
                    .getByCrn(distroxCrn, Collections.emptySet());

            List<String> instanceIds = stackV4Response.getInstanceGroups().stream()
                    .flatMap(ig -> ig.getMetadata().stream())
                    .map(InstanceMetaDataV4Response::getInstanceId)
                    .collect(Collectors.toList());

            LOGGER.info(" TAG VALIDATION:\n Distrox: {}\n instance IDs: {}.\n", testDto.getName(), instanceIds);
            verifyTags(instanceIds, customTags, testContext, testDto.getName());

            return testDto;
        };
    }

    private void verifyTags(List<String> instanceIds, Map<String, String> customTags, TestContext testContext, String resourceName) {
        if (instanceIds != null && !instanceIds.isEmpty() || !instanceIds.contains(null)) {
            CloudFunctionality cloudFunctionality = cloudProviderProxy.getCloudFunctionality();
            Map<String, Map<String, String>> tagsByInstanceId = cloudFunctionality.listTagsByInstanceId(resourceName, instanceIds);
            tagsByInstanceId.forEach((id, tags) -> {
                LOGGER.info(" Verifying resource: {} instance ID: {} with tags: {}", resourceName, id, tags);
                tagsUtil.verifyTags(new MapToTaggedResponseAdapter(tags), testContext);
                customTags.forEach((key, value) -> assertThat(tags.get(cloudFunctionality.transformTagKeyOrValue(key)))
                        .isEqualTo(cloudFunctionality.transformTagKeyOrValue(value)));
            });
        } else {
            LOGGER.error("Tag validation is not possible, because of {} instance ids: {} null or contains null!", resourceName, instanceIds);
            throw new TestFailException(String.format(" Tag validation is not possible, because of %s instance ids: %s null or contains null ", resourceName,
                    instanceIds));
        }
    }

    public Assertion<EnvironmentTestDto, EnvironmentClient> verifyUserDefinedTags(Map<String, String> userDefinedTags) {
        return (testContext, testDto, client) -> {
            String envCrn = testDto.getResponse().getCrn();
            CloudFunctionality cloudFunctionality = cloudProviderProxy.getCloudFunctionality();

            Map<String, Map<String, String>> resources = cloudFunctionality.getAllResourcesAndTagsForEnvironment(envCrn);
            LOGGER.info("Verifying all tags for environment resources.\n Number of resources: {}.\n Custom tags: {}.", resources.size(), userDefinedTags);
            if (!resources.isEmpty() && !resources.containsKey(null) && !resources.containsValue(null)) {
                Map<String, List<String>> failedResources = new HashMap<>();

                resources.forEach((arn, tags) -> {
                    LOGGER.info("Verifying ARN: {} with tags: {}", arn, tags);
                    List<String> missingTags = userDefinedTags.entrySet().stream()
                            .filter(tag -> {
                                String transformedKey = cloudFunctionality.transformTagKeyOrValue(tag.getKey());
                                String transformedValue = cloudFunctionality.transformTagKeyOrValue(tag.getValue());
                                return !transformedValue.equals(tags.get(transformedKey));
                            })
                            .map(entry -> entry.getKey() + "=" + entry.getValue())
                            .collect(Collectors.toList());

                    if (!missingTags.isEmpty()) {
                        failedResources.put(arn, missingTags);
                    }
                });

                if (!failedResources.isEmpty()) {
                    failedResources.forEach((arn, missingTags) ->
                            LOGGER.error("TAG VALIDATION FAILED: ARN: {} is missing tags: {}", arn, missingTags));
                    throw new TestFailException(String.format(
                            "Tag validation failed for %d out of %d resources. Resources with missing tags: %s",
                            failedResources.size(), resources.size(), failedResources));
                }
            } else {
                LOGGER.error("Tag validation is not possible, because of ARN list: {} null or contains null!", resources);
                throw new TestFailException(String.format(" Tag validation is not possible, because of ARN list: %s null or contains null ",
                        resources));
            }
            return testDto;
        };
    }
}
