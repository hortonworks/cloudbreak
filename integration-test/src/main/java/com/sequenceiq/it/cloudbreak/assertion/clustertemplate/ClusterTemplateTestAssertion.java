package com.sequenceiq.it.cloudbreak.assertion.clustertemplate;

import static com.sequenceiq.cloudbreak.api.endpoint.v4.clustertemplate.ClusterTemplateV4Type.OTHER;
import static com.sequenceiq.cloudbreak.api.endpoint.v4.clustertemplate.DatalakeRequired.OPTIONAL;

import java.security.InvalidParameterException;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;

import com.sequenceiq.cloudbreak.api.endpoint.v4.clustertemplate.ClusterTemplateV4Type;
import com.sequenceiq.cloudbreak.api.endpoint.v4.clustertemplate.responses.ClusterTemplateV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.ResourceStatus;
import com.sequenceiq.distrox.api.v1.distrox.model.DistroXV1Request;
import com.sequenceiq.distrox.api.v1.distrox.model.instancegroup.InstanceGroupV1Request;
import com.sequenceiq.it.cloudbreak.CloudbreakClient;
import com.sequenceiq.it.cloudbreak.assertion.Assertion;
import com.sequenceiq.it.cloudbreak.cloud.HostGroupType;
import com.sequenceiq.it.cloudbreak.dto.clustertemplate.ClusterTemplateTestDto;
import com.sequenceiq.it.cloudbreak.dto.imagecatalog.ImageCatalogTestDto;

public class ClusterTemplateTestAssertion {

    private ClusterTemplateTestAssertion() {

    }

    public static Assertion<ClusterTemplateTestDto, CloudbreakClient> getResponse() {
        return (testContext, entity, cloudbreakClient) -> {
            Optional<ClusterTemplateV4Response> first = entity
                    .getResponses()
                    .stream()
                    .filter(f -> f.getName().equals(entity.getName())).findFirst();
            if (!first.isPresent()) {
                throw new IllegalArgumentException("No element in the result");
            }

            ClusterTemplateV4Response clusterTemplateV4Response = first.get();

//            if (clusterTemplateV4Response.getStackTemplate() == null) {
//                throw new IllegalArgumentException("Stack template is empty");
//            }

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

    public static Assertion<ClusterTemplateTestDto, CloudbreakClient> getResponses(int expectedSize) {
        return (testContext, entity, cloudbreakClient) -> {
            if (entity.getResponses().size() != expectedSize) {
                throw new IllegalArgumentException(String.format("expected size is %s but got %s", expectedSize, entity.getResponses().size()));
            }
            return entity;
        };
    }

    public static Assertion<ClusterTemplateTestDto, CloudbreakClient> containsType(ClusterTemplateV4Type expectedType) {
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

    public static Assertion<ClusterTemplateTestDto, CloudbreakClient> checkStackTemplateAfterClusterTemplateCreation() {
        return (testContext, entity, cloudbreakClient) -> {
            entity.getResponses()
                    .stream()
                    .filter(f -> f.getName().equals(entity.getName()))
                    .findFirst()
                    .ifPresentOrElse(ClusterTemplateTestAssertion::validateDistroxTemplate, ClusterTemplateTestAssertion::throwExceptionUponMissingTemplate);
            return entity;
        };
    }

    private static void throwExceptionUponMissingTemplate() {
        throw new IllegalArgumentException("No element in the result");
    }

    private static void validateDistroxTemplate(ClusterTemplateV4Response clusterTemplateV4Response) {
        DistroXV1Request distroXTemplate = clusterTemplateV4Response.getDistroXTemplate();
        if (distroXTemplate == null) {
            throw new IllegalArgumentException("Template is empty");
        }

        if (!StringUtils.isEmpty(distroXTemplate.getName())) {
            throw new IllegalArgumentException("Template name should be empty!");
        }

        if (!StringUtils.isEmpty(distroXTemplate.getCluster().getPassword())) {
            throw new IllegalArgumentException("CM password should be empty!");
        }

        if (!StringUtils.isEmpty(distroXTemplate.getCluster().getUserName())) {
            throw new IllegalArgumentException("CM username should be empty!");
        }
    }

    public static Assertion<ClusterTemplateTestDto, CloudbreakClient> checkStackTemplateAfterClusterTemplateCreationWithProperties() {
        return (testContext, entity, cloudbreakClient) -> {
            ClusterTemplateTestDto clusterTemplate = testContext.get(ClusterTemplateTestDto.class);
            Optional<ClusterTemplateV4Response> first = entity.getResponses().stream().filter(ct -> ct.getName().equals(clusterTemplate.getName())).findFirst();
            if (!first.isPresent()) {
                throw new IllegalArgumentException("No element in the result");
            }

            ClusterTemplateV4Response clusterTemplateV4Response = first.get();

            DistroXV1Request stackTemplate = clusterTemplateV4Response.getDistroXTemplate();
            if (stackTemplate == null) {
                throw new IllegalArgumentException("Stack template is empty");
            }

            if (!stackTemplate.getCluster().getDatabases().equals(Set.of("mock-test-rds"))) {
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

    private static InstanceGroupV1Request getInstanceGroup(List<InstanceGroupV1Request> instanceGroups) {
        return instanceGroups
                .stream()
                .filter(ig -> HostGroupType.MASTER.getName().equals(ig.getName()))
                .findFirst()
                .orElseThrow(() -> new InvalidParameterException("Unable to find valid instancegroup by type"));
    }
}
