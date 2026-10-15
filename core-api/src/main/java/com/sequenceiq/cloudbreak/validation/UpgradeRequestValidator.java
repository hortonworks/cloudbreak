package com.sequenceiq.cloudbreak.validation;

import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.commons.lang3.StringUtils.isEmpty;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.apache.commons.lang3.StringUtils.isNotEmpty;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.InternalUpgradeSettings;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.cluster.cm.ClouderaManagerV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.cluster.cm.product.ClouderaManagerProductV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.cluster.cm.repository.ClouderaManagerRepositoryV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.tags.upgrade.UpgradeV4Request;
import com.sequenceiq.common.api.util.ValidatorUtil;
import com.sequenceiq.distrox.api.v1.distrox.model.cluster.cm.ClouderaManagerV1Request;
import com.sequenceiq.distrox.api.v1.distrox.model.cluster.cm.product.ClouderaManagerProductV1Request;
import com.sequenceiq.distrox.api.v1.distrox.model.cluster.cm.repository.ClouderaManagerRepositoryV1Request;

public class UpgradeRequestValidator implements ConstraintValidator<ValidUpgradeRequest, UpgradeV4Request> {

    public static final String MISSING_REPOSITORY_ERROR = "Invalid upgrade components: components.repository.version and baseUrl are required.";

    public static final String COMPONENTS_OPTIONS_ERROR = "Invalid upgrade request: components cannot be combined with runtime, imageId, "
            + "lockComponents=true, dryRun or showAvailableImages.";

    private static final String CDH_PRODUCT_NAME = "CDH";

    @Override
    public boolean isValid(UpgradeV4Request request, ConstraintValidatorContext context) {
        if (!validateEmptyRequest(request, context)) {
            return false;
        }
        if (!mutuallyExclusiveDryRunOrShowImages(request, context)) {
            return false;
        }
        if (request.getComponents() != null) {
            return validateComponents(request, context);
        }
        return isOsUpgrade(request) || isRuntimeUpgrade(request) || request.isDryRunOnly() || request.isShowAvailableImagesOnly() || request.isEmpty();
    }

    private boolean validateComponents(UpgradeV4Request request, ConstraintValidatorContext context) {
        Optional<String> error = getComponentsOptionsError(request).or(() -> getComponentsContentError(request.getComponents()));
        error.ifPresent(message -> ValidatorUtil.addConstraintViolation(context, message).disableDefaultConstraintViolation());
        return error.isEmpty();
    }

    public static Optional<String> getComponentsOptionsError(UpgradeV4Request request) {
        boolean reinitiation = Optional.ofNullable(request.getInternalUpgradeSettings())
                .map(InternalUpgradeSettings::isUpgradeReinitiation).orElse(false);
        if (Boolean.TRUE.equals(request.getLockComponents()) || request.isDryRun() || request.isShowAvailableImagesSet()
                || ((isNotBlank(request.getImageId()) || isNotBlank(request.getRuntime())) && !reinitiation)) {
            return Optional.of(COMPONENTS_OPTIONS_ERROR);
        }
        return Optional.empty();
    }

    public static Optional<String> getComponentsContentError(ClouderaManagerV4Request components) {
        if (components == null || components.getRepository() == null) {
            return Optional.of(MISSING_REPOSITORY_ERROR);
        }
        ClouderaManagerRepositoryV4Request repo = components.getRepository();
        return getComponentsContentError(repo.getVersion(), repo.getBaseUrl(),
                getProductNames(components.getProducts(), ClouderaManagerProductV4Request::getName));
    }

    public static Optional<String> getComponentsContentError(ClouderaManagerV1Request components) {
        if (components == null || components.getRepository() == null) {
            return Optional.of(MISSING_REPOSITORY_ERROR);
        }
        ClouderaManagerRepositoryV1Request repo = components.getRepository();
        return getComponentsContentError(repo.getVersion(), repo.getBaseUrl(),
                getProductNames(components.getProducts(), ClouderaManagerProductV1Request::getName));
    }

    public static Optional<String> getComponentsContentError(String cmVersion, String cmBaseUrl, List<String> productNames) {
        if (isBlank(cmVersion) || isBlank(cmBaseUrl)) {
            return Optional.of(MISSING_REPOSITORY_ERROR);
        }
        if (productNames == null || productNames.isEmpty() || productNames.stream().anyMatch(name -> isBlank(name))
                || productNames.stream().filter(CDH_PRODUCT_NAME::equals).count() != 1) {
            return Optional.of("Invalid upgrade components: components.products must contain exactly one CDH parcel and non-empty product names.");
        }
        return Optional.empty();
    }

    public static <T> List<String> getProductNames(List<T> products, Function<T, String> getName) {
        return products == null ? null : products.stream().map(product -> product == null ? null : getName.apply(product)).toList();
    }

    private boolean validateEmptyRequest(UpgradeV4Request request, ConstraintValidatorContext context) {
        if (Objects.isNull(request)) {
            String msg = "Invalid upgrade request: empty content";
            ValidatorUtil.addConstraintViolation(context, msg).disableDefaultConstraintViolation();
            return false;
        }
        return true;
    }

    private boolean mutuallyExclusiveDryRunOrShowImages(UpgradeV4Request request, ConstraintValidatorContext context) {
        if (request.isDryRun()  && request.isShowAvailableImagesSet()) {
            String msg = "Invalid upgrade request: 'dry-run' cannot be used in parallel with  'show-available-images' or "
                    + "'show-latest-available-image-per-runtime' in the request";
            ValidatorUtil.addConstraintViolation(context, msg).disableDefaultConstraintViolation();
            return false;
        }
        return true;
    }

    private boolean isOsUpgrade(UpgradeV4Request request) {
        return Boolean.TRUE.equals(request.getLockComponents()) && isEmpty(request.getRuntime());
    }

    private boolean isRuntimeUpgrade(UpgradeV4Request request) {
        boolean hasRuntimeWithoutImageId = isNotEmpty(request.getRuntime()) && isEmpty(request.getImageId());
        boolean hasImageIdWithoutRuntime = isNotEmpty(request.getImageId()) && isEmpty(request.getRuntime());
        return !Boolean.TRUE.equals(request.getLockComponents()) && (hasRuntimeWithoutImageId || hasImageIdWithoutRuntime);
    }
}
