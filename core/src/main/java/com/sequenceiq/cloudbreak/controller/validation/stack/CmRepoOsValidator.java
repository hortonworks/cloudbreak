package com.sequenceiq.cloudbreak.controller.validation.stack;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.StackV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.cluster.ClusterV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.cluster.cm.ClouderaManagerV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.cluster.cm.product.ClouderaManagerProductV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.cluster.cm.repository.ClouderaManagerRepositoryV4Request;
import com.sequenceiq.cloudbreak.cloud.model.catalog.Image;
import com.sequenceiq.cloudbreak.common.exception.BadRequestException;
import com.sequenceiq.common.model.OsType;

/**
 * Fails stack creation up-front when the OS embedded in the request's CM
 * repository baseUrl or CDH parcel URLs is inconsistent with the resolved
 * image OS (e.g. a redhat9 image with a redhat8 CM repo). See CB-32586.
 * Only enforced for RHEL8 / RHEL9 image OS values; other OS types pass through.
 */
@Component
public class CmRepoOsValidator {

    private static final Logger LOGGER = LoggerFactory.getLogger(CmRepoOsValidator.class);

    private static final Set<OsType> ENFORCED_OS_TYPES = Set.of(OsType.RHEL8, OsType.RHEL9);

    public void validate(StackV4Request stackRequest, Image image) {
        if (stackRequest == null || image == null) {
            return;
        }
        Optional<OsType> imageOsTypeOpt = OsType.getByOsOptional(image.getOs());
        LOGGER.info("Validating CM repo and parcel URLs against image OS '{}' (imageId={})", image.getOs(), image.getUuid());
        if (imageOsTypeOpt.isEmpty() || !ENFORCED_OS_TYPES.contains(imageOsTypeOpt.get())) {
            LOGGER.debug("Skipping CM repo OS validation for image OS '{}' (not RHEL8/RHEL9).", image.getOs());
            return;
        }
        OsType imageOsType = imageOsTypeOpt.get();
        ClouderaManagerV4Request cm = Optional.ofNullable(stackRequest.getCluster())
                .map(ClusterV4Request::getCm)
                .orElse(null);
        if (cm == null) {
            return;
        }
        validateBaseUrl(cm.getRepository(), imageOsType, image.getOs());
        validateProducts(cm.getProducts(), imageOsType, image.getOs());
    }

    private void validateBaseUrl(ClouderaManagerRepositoryV4Request repository, OsType imageOsType, String imageOs) {
        if (repository == null) {
            return;
        }
        String baseUrl = repository.getBaseUrl();
        if (StringUtils.isBlank(baseUrl)) {
            return;
        }
        OsType conflicting = otherRhel(imageOsType);
        if (StringUtils.containsIgnoreCase(baseUrl, "/" + conflicting.getOs() + "/")) {
            String message = String.format(
                    "CM repository baseUrl OS '%s' is not consistent with image OS '%s'. Field: cluster.cm.repository.baseUrl. URL: %s",
                    conflicting.getOs(), imageOs, baseUrl);
            LOGGER.warn(message);
            throw new BadRequestException(message);
        }
    }

    private void validateProducts(List<ClouderaManagerProductV4Request> products, OsType imageOsType, String imageOs) {
        if (products == null || products.isEmpty()) {
            return;
        }
        OsType conflicting = otherRhel(imageOsType);
        String conflictingPostfix = conflicting.getParcelPostfix();
        String conflictingRhelToken = "/" + conflicting.getOs() + "/";
        for (int i = 0; i < products.size(); i++) {
            ClouderaManagerProductV4Request product = products.get(i);
            if (product == null) {
                continue;
            }
            String parcel = product.getParcel();
            if (StringUtils.isBlank(parcel)) {
                continue;
            }
            String conflictingToken = null;
            if (containsOsToken(parcel, conflictingPostfix)) {
                conflictingToken = conflictingPostfix;
            } else if (StringUtils.containsIgnoreCase(parcel, conflictingRhelToken)) {
                conflictingToken = conflicting.getOs();
            }
            if (conflictingToken != null) {
                String message = String.format(
                        "CDH parcel URL OS '%s' is not consistent with image OS '%s'. Field: cluster.cm.products[%d].parcel. URL: %s",
                        conflictingToken, imageOs, i, parcel);
                LOGGER.warn(message);
                throw new BadRequestException(message);
            }
        }
    }

    /**
     * Matches the OS postfix (e.g. {@code el8}) only when it appears as a
     * delimited token (e.g. {@code -el8}, {@code .el8}, {@code /el8/}) so we
     * don't get tricked by substrings inside version strings.
     */
    private boolean containsOsToken(String url, String postfix) {
        String lower = url.toLowerCase();
        String p = postfix.toLowerCase();
        return lower.contains("-" + p) || lower.contains("." + p) || lower.contains("/" + p + "/") || lower.endsWith("/" + p);
    }

    private OsType otherRhel(OsType imageOsType) {
        return OsType.RHEL8.equals(imageOsType) ? OsType.RHEL9 : OsType.RHEL8;
    }
}
