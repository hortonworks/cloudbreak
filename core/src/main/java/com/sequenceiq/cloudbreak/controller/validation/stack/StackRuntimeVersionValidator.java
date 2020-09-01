package com.sequenceiq.cloudbreak.controller.validation.stack;

import static com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider.INTERNAL_ACTOR_CRN;

import java.util.List;
import java.util.Optional;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.StackV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.cluster.ClusterV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.cluster.cm.ClouderaManagerV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.cluster.repository.RepositoryV4Request;
import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.cloudbreak.cloud.model.catalog.Image;
import com.sequenceiq.cloudbreak.cloud.model.catalog.StackDetails;
import com.sequenceiq.cloudbreak.exception.BadRequestException;
import com.sequenceiq.cloudbreak.service.datalake.SdxClientService;
import com.sequenceiq.sdx.api.model.SdxClusterResponse;

@Component
public class StackRuntimeVersionValidator {

    private static final Logger LOGGER = LoggerFactory.getLogger(StackRuntimeVersionValidator.class);

    @Inject
    private SdxClientService sdxClientService;

    @Inject
    private EntitlementService entitlementService;

    public void validate(StackV4Request stackRequest, Image image) {
        if (isDifferentDataHubAndDataLakeVersionAllowed()) {
            LOGGER.debug("The Data Hub version validation has been turned off with entitlement.");
        } else {
            LOGGER.debug("Validating Data Hub version.");
            findStackVersion(stackRequest, image).ifPresent(stackRuntimeVersion -> {
                List<SdxClusterResponse> sdxClusters = sdxClientService.getByEnvironmentCrn(stackRequest.getEnvironmentCrn());
                sdxClusters.forEach(sdx -> validateStackVersion(stackRuntimeVersion, sdx.getRuntime()));
            });
        }

    }

    private Optional<String> findStackVersion(StackV4Request stackRequest, Image image) {
        return findStackVersionInImage(image).or(() -> findStackVersionInStackRequest(stackRequest));
    }

    private Optional<String> findStackVersionInImage(Image image) {
        return Optional.of(image)
                .map(Image::getStackDetails)
                .map(StackDetails::getVersion);
    }

    private Optional<String> findStackVersionInStackRequest(StackV4Request stackRequest) {
        return Optional.of(stackRequest)
                .map(StackV4Request::getCluster)
                .map(ClusterV4Request::getCm)
                .map(ClouderaManagerV4Request::getRepository)
                .map(RepositoryV4Request::getVersion);
    }

    private void validateStackVersion(String stackVersion, String sdxRuntimeVersion) {
        if (!stackVersion.equals(sdxRuntimeVersion)) {
            String errorMessage = String.format(
                    "Data Hub cluster (%s) creation is not allowed with different runtime version than the Data Lake version (%s).",
                    stackVersion, sdxRuntimeVersion);
            LOGGER.error(errorMessage);
            throw new BadRequestException(errorMessage);
        }
    }

    private boolean isDifferentDataHubAndDataLakeVersionAllowed() {
        return entitlementService.isDifferentDataHubAndDataLakeVersionAllowed(INTERNAL_ACTOR_CRN, ThreadBasedUserCrnProvider.getAccountId());
    }
}
