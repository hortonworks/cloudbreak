package com.sequenceiq.cloudbreak.service.upgrade.validation.service;

import static com.sequenceiq.cloudbreak.cloud.model.component.StackType.CDH;

import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.StackType;
import com.sequenceiq.cloudbreak.cloud.model.ClouderaManagerProduct;
import com.sequenceiq.cloudbreak.cloud.model.ClouderaManagerRepo;
import com.sequenceiq.cloudbreak.cluster.model.ParcelInfo;
import com.sequenceiq.cloudbreak.cluster.model.ParcelStatus;
import com.sequenceiq.cloudbreak.common.exception.UpgradeValidationFailedException;
import com.sequenceiq.cloudbreak.service.upgrade.ClusterUpgradeProperties;
import com.sequenceiq.cloudbreak.service.upgrade.ImageFilterParamsFactory;
import com.sequenceiq.cloudbreak.service.upgrade.sync.component.CmServerQueryService;

@Component
public class LockedComponentsValidator {

    private static final Logger LOGGER = LoggerFactory.getLogger(LockedComponentsValidator.class);

    private static final String RECOVERY_MESSAGE = "Synchronize component versions with Cloudera Manager and retry the runtime upgrade.";

    @Inject
    private CmServerQueryService cmServerQueryService;

    @Inject
    private ImageFilterParamsFactory imageFilterParamsFactory;

    public void validate(ServiceUpgradeValidationRequest request) {
        validate(request, false);
    }

    public void validateOrderedOsUpgrade(ServiceUpgradeValidationRequest request) {
        validate(request, request.clusterUpgradeProperties().targetImage().isBaseImage());
    }

    private void validate(ServiceUpgradeValidationRequest request, boolean retainCurrentComponents) {
        if (request.lockComponents() && StackType.WORKLOAD.equals(request.stack().getType())) {
            try {
                ClusterUpgradeProperties properties = request.clusterUpgradeProperties();
                validateActiveParcels(request, retainCurrentComponents ? properties.currentImage().products() : properties.getAllTargetProducts());
                validateCmVersion(request, retainCurrentComponents ? properties.currentImage().clouderaManagerRepo() : properties.getClouderaManagerRepo());
                LOGGER.info("Confirmed the installed component versions before skipping the runtime upgrade.");
            } catch (UpgradeValidationFailedException e) {
                throw e;
            } catch (RuntimeException e) {
                throw new UpgradeValidationFailedException("Cannot confirm the installed component versions before skipping the runtime upgrade. "
                        + RECOVERY_MESSAGE, e);
            }
        }
    }

    private void validateActiveParcels(ServiceUpgradeValidationRequest request, Set<ClouderaManagerProduct> products) {
        Set<String> requiredParcels = imageFilterParamsFactory.getStackRelatedParcels(request.stack()).keySet();
        Map<String, String> targetParcels = products.stream()
                .filter(product -> requiredParcels.contains(product.getName()) || CDH.name().equals(product.getName()))
                .collect(Collectors.toMap(ClouderaManagerProduct::getName, ClouderaManagerProduct::getVersion));
        if (!targetParcels.containsKey(CDH.name()) || !targetParcels.keySet().containsAll(requiredParcels)) {
            throw new UpgradeValidationFailedException("Cannot confirm the target versions of the required parcels before skipping the runtime upgrade. "
                    + RECOVERY_MESSAGE);
        }
        Set<ParcelInfo> parcels = cmServerQueryService.queryAllParcels(request.stack());
        Set<String> inactiveParcels = targetParcels.entrySet().stream()
                .filter(target -> parcels.stream().noneMatch(parcel -> ParcelStatus.ACTIVATED.equals(parcel.getStatus())
                        && target.getKey().equals(parcel.getName()) && target.getValue().equals(parcel.getVersion())))
                .map(target -> target.getKey() + " " + target.getValue())
                .collect(Collectors.toSet());
        if (!inactiveParcels.isEmpty()) {
            throw new UpgradeValidationFailedException("Cannot skip the runtime upgrade because the target parcels are not active "
                    + "in Cloudera Manager: " + inactiveParcels + ". " + RECOVERY_MESSAGE);
        }
    }

    private void validateCmVersion(ServiceUpgradeValidationRequest request, ClouderaManagerRepo repo) {
        String expectedVersion = Strings.CS.removeEnd(repo.getFullVersion(), "p");
        if (StringUtils.isBlank(repo.getBuildNumber())
                || cmServerQueryService.queryCmServerVersion(request.stack())
                .map(version -> Strings.CS.removeEnd(version, "p"))
                .filter(version -> version.equals(expectedVersion)).isEmpty()) {
            throw new UpgradeValidationFailedException("Cannot skip the runtime upgrade because the installed Cloudera Manager version "
                    + "does not match the target version " + expectedVersion + " or could not be confirmed. " + RECOVERY_MESSAGE);
        }
    }
}
