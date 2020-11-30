package com.sequenceiq.cloudbreak.controller.validation.stack;

import static com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider.INTERNAL_ACTOR_CRN;

import java.util.List;
import java.util.Optional;

import javax.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.StackType;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.StackV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.cluster.ClusterV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.cluster.cm.ClouderaManagerV4Request;
import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.cloudbreak.cloud.model.ClouderaManagerProduct;
import com.sequenceiq.cloudbreak.cloud.model.ClouderaManagerProductBase;
import com.sequenceiq.cloudbreak.cloud.model.catalog.Image;
import com.sequenceiq.cloudbreak.cloud.model.catalog.StackDetails;
import com.sequenceiq.cloudbreak.cluster.service.ClusterComponentConfigProvider;
import com.sequenceiq.cloudbreak.domain.view.StackView;
import com.sequenceiq.cloudbreak.common.exception.BadRequestException;
import com.sequenceiq.cloudbreak.service.datalake.SdxClientService;
import com.sequenceiq.cloudbreak.service.stack.StackViewService;
import com.sequenceiq.sdx.api.model.SdxClusterResponse;
import com.sequenceiq.sdx.api.model.SdxClusterStatusResponse;

@Component
public class StackRuntimeVersionValidator {

    private static final Logger LOGGER = LoggerFactory.getLogger(StackRuntimeVersionValidator.class);

    @Inject
    private SdxClientService sdxClientService;

    @Inject
    private StackViewService stackViewService;

    @Inject
    private ClusterComponentConfigProvider clusterComponentConfigProvider;

    @Inject
    private EntitlementService entitlementService;

    public void validate(StackV4Request stackRequest, Image image, StackType stackType) {
        if (StackType.WORKLOAD.equals(stackType)) {
            if (isDifferentDataHubAndDataLakeVersionAllowed()) {
                LOGGER.debug("The Data Hub version validation has been turned off with entitlement.");
            } else {
                LOGGER.debug("Validating Data Hub version.");
                findRequestedStackVersion(stackRequest, image).ifPresent(requestedRuntimeVersion ->
                        checkRuntimeVersion(stackRequest.getEnvironmentCrn(), requestedRuntimeVersion));
            }
        }
    }

    private void checkRuntimeVersion(String environmentCrn, String requestedStackVersion) {
        Optional<String> optionalErrorMessage = validateStackVersionLocally(environmentCrn, requestedStackVersion);
        if (optionalErrorMessage.isPresent()) {
            LOGGER.error("Falling back to SDX service, because: {}", optionalErrorMessage.get());
            validateStackVersionWithSdxService(environmentCrn, requestedStackVersion);
        }
    }

    private Optional<String> validateStackVersionLocally(String environmentCrn, String requestedStackVersion) {
        String datalakeStackVersion;
        try {
            Optional<StackView> relatedDatalakeStack = stackViewService.findDatalakeViewByEnvironmentCrn(environmentCrn);
            if (relatedDatalakeStack.isPresent() && relatedDatalakeStack.get().isAvailable()) {
                List<ClouderaManagerProduct> clouderaManagerProductDetails =
                        clusterComponentConfigProvider.getClouderaManagerProductDetails(relatedDatalakeStack.get().getClusterView().getId());
                Optional<String> cdhVersion = getCdhVersionFromClouderaManagerProducts(clouderaManagerProductDetails);
                if (cdhVersion.isEmpty()) {
                    return Optional.of(String.format("Cannot found CDH details about related datalake stack in CB, name: %s",
                            relatedDatalakeStack.get().getName()));
                }
                datalakeStackVersion = cdhVersion.get();
            } else {
                return Optional.of(String.format("Cannot found related Data Lake stack in CB for environment CRN %s " +
                        "or the datalake isn't available yet.", environmentCrn));
            }
        } catch (Exception e) {
            return Optional.of(String.format("Something happened during check of runtime version of Data Lake stack on CB side: %s", e.getMessage()));
        }
        compareRuntimeVersions(requestedStackVersion, datalakeStackVersion);
        return Optional.empty();
    }

    private void validateStackVersionWithSdxService(String environmentCrn, String requestedStackVersion) {
        List<SdxClusterResponse> sdxClusters = sdxClientService.getByEnvironmentCrn(environmentCrn);
        sdxClusters.forEach(sdx -> {
            if (SdxClusterStatusResponse.RUNNING.equals(sdx.getStatus())) {
                compareRuntimeVersions(requestedStackVersion, sdx.getRuntime());
            } else {
                throw new BadRequestException(String.format("Datalake %s is not available yet, thus we cannot check runtime version!", sdx.getName()));
            }
        });
    }

    private Optional<String> findRequestedStackVersion(StackV4Request stackRequest, Image image) {
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
                .map(ClouderaManagerV4Request::getProducts)
                .flatMap(this::getCdhVersionFromClouderaManagerProducts);
    }

    private Optional<String> getCdhVersionFromClouderaManagerProducts(List<? extends ClouderaManagerProductBase> products) {
        return products.stream()
                .filter(product -> "CDH".equals(product.getName()))
                .map(product -> StringUtils.substringBefore(product.getVersion(), "-"))
                .findFirst();
    }

    private void compareRuntimeVersions(String requestedRuntimeVersion, String sdxRuntimeVersion) {
        if (sdxRuntimeVersion != null && !requestedRuntimeVersion.equals(sdxRuntimeVersion)) {
            String errorMessage = String.format(
                    "Data Hub cluster (%s) creation is not allowed with different runtime version than the Data Lake version (%s).",
                    requestedRuntimeVersion, sdxRuntimeVersion);
            LOGGER.error(errorMessage);
            throw new BadRequestException(errorMessage);
        }
    }

    private boolean isDifferentDataHubAndDataLakeVersionAllowed() {
        return entitlementService.isDifferentDataHubAndDataLakeVersionAllowed(INTERNAL_ACTOR_CRN, ThreadBasedUserCrnProvider.getAccountId());
    }
}
