package com.sequenceiq.it.cloudbreak.assertion.clustertemplate;

import static com.google.common.collect.Sets.newHashSet;
import static com.sequenceiq.cloudbreak.api.endpoint.v4.clustertemplate.ClusterTemplateV4Type.OTHER;
import static com.sequenceiq.cloudbreak.api.endpoint.v4.clustertemplate.DatalakeRequired.OPTIONAL;

import java.security.InvalidParameterException;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import org.apache.commons.lang3.StringUtils;

import com.sequenceiq.cloudbreak.api.endpoint.v4.clustertemplate.ClusterTemplateV4Type;
import com.sequenceiq.cloudbreak.api.endpoint.v4.clustertemplate.responses.ClusterTemplateV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.ResourceStatus;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.StackV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.instancegroup.InstanceGroupV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.util.requests.SecurityRuleV4Request;
import com.sequenceiq.it.cloudbreak.cloud.HostGroupType;
import com.sequenceiq.it.cloudbreak.dto.clustertemplate.ClusterTemplateTestDto;
import com.sequenceiq.it.cloudbreak.dto.imagecatalog.ImageCatalogTestDto;
import com.sequenceiq.it.cloudbreak.assertion.Assertion;

public class ClusterTemplateTestAssertion {

    private ClusterTemplateTestAssertion() {

    }

    public static Assertion<ClusterTemplateTestDto> getResponse() {
        return (testContext, entity, cloudbreakClient) -> {
            Optional<ClusterTemplateV4Response> first = entity
                    .getResponses()
                    .stream()
                    .filter(f -> f.getName().equals(entity.getName())).findFirst();
            if (!first.isPresent()) {
                throw new IllegalArgumentException("No element in the result");
            }

            ClusterTemplateV4Response clusterTemplateV4Response = first.get();

            if (clusterTemplateV4Response.getStackTemplate() == null) {
                throw new IllegalArgumentException("Stack template is empty");
            }

            if (!OTHER.equals(clusterTemplateV4Response.getType())) {
                throw new IllegalArgumentException(String
                        .format("Mismatch type result, OTHER expected but got %s", clusterTemplateV4Response.getType()));
            }

            if (!OPTIONAL.equals(clusterTemplateV4Response.getDatalakeRequired())) {
                throw new IllegalArgumentException(String
                        .format("Mismatch datalake required result, OPTIONAL expected but got %s", clusterTemplateV4Response.getDatalakeRequired()));
            }

            if (!ResourceStatus.USER_MANAGED.equals(clusterTemplateV4Response.getStatus())) {
                throw new IllegalArgumentException(String
                        .format("Mismatch status result, USER_MANAGED expected but got %s", clusterTemplateV4Response.getStatus()));
            }

            return entity;
        };
    }

    public static Assertion<ClusterTemplateTestDto> getResponses(int expectedSize) {
        return (testContext, entity, cloudbreakClient) -> {
            if (entity.getResponses().size() != expectedSize) {
                throw new IllegalArgumentException(String.format("expected size is %s but got %s", expectedSize, entity.getResponses().size()));
            }
            return entity;
        };
    }

    public static Assertion<ClusterTemplateTestDto> containsType(ClusterTemplateV4Type expectedType) {
        return (testContext, entity, cloudbreakClient) -> {
            ClusterTemplateTestDto clusterTemplate = testContext.get(ClusterTemplateTestDto.class);
            Optional<ClusterTemplateV4Response> first = entity.getResponses().stream().filter(ct -> ct.getName().equals(clusterTemplate.getName())).findFirst();
            if (!first.isPresent()) {
                throw new IllegalArgumentException("No element in the result");
            }

            ClusterTemplateV4Response clusterTemplateV4Response = first.get();

            if (!expectedType.equals(clusterTemplateV4Response.getType())) {
                throw new IllegalArgumentException(String
                        .format("Mismatch type result, %s expected but got %s", expectedType, clusterTemplateV4Response.getType()));
            }
            return entity;
        };
    }

    public static Assertion<ClusterTemplateTestDto> checkStackTemplateAfterClusterTemplateCreation() {
        return (testContext, entity, cloudbreakClient) -> {
            Optional<ClusterTemplateV4Response> first = entity.getResponses().stream().filter(f -> f.getName().equals(entity.getName())).findFirst();
            if (!first.isPresent()) {
                throw new IllegalArgumentException("No element in the result");
            }

            ClusterTemplateV4Response clusterTemplateV4Response = first.get();

            StackV4Request stackTemplate = clusterTemplateV4Response.getStackTemplate();
            if (stackTemplate == null) {
                throw new IllegalArgumentException("Stack template is empty");
            }

            if (!StringUtils.isEmpty(stackTemplate.getName())) {
                throw new IllegalArgumentException("Stack template name should be empty!");
            }

            if (!StringUtils.isEmpty(stackTemplate.getCluster().getPassword())) {
                throw new IllegalArgumentException("Ambari password should be empty!");
            }

            if (!StringUtils.isEmpty(stackTemplate.getCluster().getUserName())) {
                throw new IllegalArgumentException("Ambari username should be empty!");
            }

            return entity;
        };
    }

    public static Assertion<ClusterTemplateTestDto> checkStackTemplateAfterClusterTemplateCreationWithProperties() {
        return (testContext, entity, cloudbreakClient) -> {
            ClusterTemplateTestDto clusterTemplate = testContext.get(ClusterTemplateTestDto.class);
            Optional<ClusterTemplateV4Response> first = entity.getResponses().stream().filter(ct -> ct.getName().equals(clusterTemplate.getName())).findFirst();
            if (!first.isPresent()) {
                throw new IllegalArgumentException("No element in the result");
            }

            ClusterTemplateV4Response clusterTemplateV4Response = first.get();

            StackV4Request stackTemplate = clusterTemplateV4Response.getStackTemplate();
            if (stackTemplate == null) {
                throw new IllegalArgumentException("Stack template is empty");
            }

            if (!"10.10.0.0/16".equals(stackTemplate.getNetwork().getSubnetCIDR())) {
                throw new IllegalArgumentException("SubnetCIDR is mismatch!");
            }

            InstanceGroupV4Request master = getInstanceGroup(stackTemplate.getInstanceGroups(), HostGroupType.MASTER);
            if (!master.getRecipeNames().equals(newHashSet("mock-test-recipe"))) {
                throw new IllegalArgumentException("Master recipes are mismatches!");
            }
            if (!master.getSecurityGroup().getSecurityGroupIds().equals(newHashSet("scgId1", "scgId2"))) {
                throw new IllegalArgumentException("Security groups are mismatches!");
            }

            InstanceGroupV4Request worker = getInstanceGroup(stackTemplate.getInstanceGroups(), HostGroupType.WORKER);
            SecurityRuleV4Request securityRuleRequest = worker.getSecurityGroup().getSecurityRules().get(0);
            if (!Arrays.asList("55", "66", "77").equals(securityRuleRequest.getPorts())) {
                throw new IllegalArgumentException("Ports are mismatches!");
            }

            if (!"ftp".equals(securityRuleRequest.getProtocol())) {
                throw new IllegalArgumentException("Protocol is mismatches!");
            }

            if (!"10.0.0.0/32".equals(securityRuleRequest.getSubnet())) {
                throw new IllegalArgumentException("Subnet is mismatches!");
            }

            if (!stackTemplate.getCluster().getDatabases().equals(newHashSet("mock-test-rds"))) {
                throw new IllegalArgumentException("RDS is mismatch!");
            }

            if (!"custom-tag".equals(stackTemplate.getTags().getUserDefined().get("some-tag"))) {
                throw new IllegalArgumentException("User defined tag is mismatch!");
            }

            if (!"custom-input".equals(stackTemplate.getInputs().get("some-input"))) {
                throw new IllegalArgumentException("Input is mismatch!");
            }

            if (!"f6e778fc-7f17-4535-9021-515351df3691".equals(stackTemplate.getImage().getId())) {
                throw new IllegalArgumentException("Image ID is mismatch!");
            }

            ImageCatalogTestDto imageCatalogTestDto = testContext.get(ImageCatalogTestDto.class);
            if (!imageCatalogTestDto.getName().equals(stackTemplate.getImage().getCatalog())) {
                throw new IllegalArgumentException("Image catalog name is mismatch!");
            }

            if (!org.springframework.util.StringUtils.isEmpty(stackTemplate.getCluster().getPassword())) {
                throw new IllegalArgumentException("Password should be empty!");
            }

            if (!org.springframework.util.StringUtils.isEmpty(stackTemplate.getCluster().getUserName())) {
                throw new IllegalArgumentException("Username should be empty!");
            }

            return entity;
        };
    }

    private static InstanceGroupV4Request getInstanceGroup(List<InstanceGroupV4Request> instanceGroups, HostGroupType hostGroupType) {
        return instanceGroups
                .stream()
                .filter(ig -> hostGroupType.getName().equals(ig.getName()))
                .findFirst()
                .orElseThrow(() -> new InvalidParameterException("Unable to find valid instancegroup by type"));
    }
}
