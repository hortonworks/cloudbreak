package com.sequenceiq.it.cloudbreak.assertion.util;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.instancegroup.instancemetadata.InstanceMetaDataV4Response;
import com.sequenceiq.common.api.tag.response.MapToTaggedResponseAdapter;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.instance.InstanceMetaDataResponse;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.describe.DescribeFreeIpaResponse;
import com.sequenceiq.it.cloudbreak.CloudbreakClient;
import com.sequenceiq.it.cloudbreak.EnvironmentClient;
import com.sequenceiq.it.cloudbreak.FreeIpaClient;
import com.sequenceiq.it.cloudbreak.SdxClient;
import com.sequenceiq.it.cloudbreak.assertion.Assertion;
import com.sequenceiq.it.cloudbreak.cloud.v4.CloudProviderProxy;
import com.sequenceiq.it.cloudbreak.dto.distrox.DistroXTestDto;
import com.sequenceiq.it.cloudbreak.dto.environment.EnvironmentTestDto;
import com.sequenceiq.it.cloudbreak.dto.sdx.SdxInternalTestDto;
import com.sequenceiq.it.util.TagsUtil;

@Service
public class CloudProviderSideTagAssertion {

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
                    .getFreeIpaClient()
                    .getFreeIpaV1Endpoint()
                    .describe(envCrn);

            List<String> instanceIds = freeIpaResponse.getInstanceGroups().stream()
                    .flatMap(ig -> ig.getMetaData().stream())
                    .map(InstanceMetaDataResponse::getInstanceId)
                    .collect(Collectors.toList());

            verifyTags(instanceIds, customTags);

            return testDto;
        };
    }

    public Assertion<SdxInternalTestDto, SdxClient> verifyInternalSdxTags(Map<String, String> customTags) {
        return (testContext, testDto, client) -> {
            List<String> instanceIds = testDto.getResponse().getStackV4Response().getInstanceGroups().stream()
                    .flatMap(ig -> ig.getMetadata().stream())
                    .map(InstanceMetaDataV4Response::getInstanceId)
                    .collect(Collectors.toList());

            verifyTags(instanceIds, customTags);

            return testDto;
        };
    }

    public Assertion<DistroXTestDto, CloudbreakClient> verifyDistroxTags(Map<String, String> customTags) {
        return (testContext, testDto, client) -> {
            List<String> instanceIds = testDto.getResponse().getInstanceGroups().stream()
                    .flatMap(ig -> ig.getMetadata().stream())
                    .map(InstanceMetaDataV4Response::getInstanceId)
                    .collect(Collectors.toList());

            verifyTags(instanceIds, customTags);

            return testDto;
        };
    }

    private void verifyTags(List<String> instanceIds, Map<String, String> customTags) {
        Map<String, Map<String, String>> tagsByInstanceId = cloudProviderProxy.getCloudFunctionality().listTagsByInstanceId(instanceIds);
        tagsByInstanceId.forEach((id, tags) -> {
            tagsUtil.verifyTags(new MapToTaggedResponseAdapter(tags));
            customTags.forEach((key, value) -> assertThat(tags.get(key)).isEqualTo(value));
        });
    }
}
