package com.sequenceiq.cloudbreak.service.upgrade;

import static com.sequenceiq.cloudbreak.cloud.model.component.StackType.CDH;

import java.net.URI;
import java.util.List;

import org.apache.commons.lang3.StringUtils;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.cloudbreak.cloud.model.ClouderaManagerProduct;
import com.sequenceiq.cloudbreak.cloud.model.ClouderaManagerRepo;
import com.sequenceiq.cloudbreak.common.exception.BadRequestException;
import com.sequenceiq.cloudbreak.util.CdhVersionProvider;
import com.sequenceiq.cloudbreak.validation.UpgradeRequestValidator;

@JsonIgnoreProperties(ignoreUnknown = true)
public record UpgradeComponents(ClouderaManagerRepo repository, List<ClouderaManagerProduct> products) {

    @JsonCreator
    public UpgradeComponents(@JsonProperty("repository") ClouderaManagerRepo repository,
            @JsonProperty("products") List<ClouderaManagerProduct> products) {
        this.repository = repository;
        this.products = products;
    }

    @JsonIgnore
    public ClouderaManagerRepo getRepository() {
        return repository;
    }

    @JsonIgnore
    public List<ClouderaManagerProduct> getProducts() {
        return products;
    }

    @JsonIgnore
    public ClouderaManagerRepo getNormalizedRepository() {
        ClouderaManagerRepo repo = repository.copy();
        if (repo.getVersion().contains("-")) {
            String fullVersion = repo.getVersion();
            repo.setVersion(StringUtils.substringBefore(fullVersion, "-"));
            repo.setBuildNumber(StringUtils.substringAfter(fullVersion, "-"));
        }
        return repo;
    }

    public void validate() {
        if (repository == null) {
            throw new BadRequestException(UpgradeRequestValidator.MISSING_REPOSITORY_ERROR);
        }
        UpgradeRequestValidator.getComponentsContentError(repository.getVersion(), repository.getBaseUrl(),
                UpgradeRequestValidator.getProductNames(products, ClouderaManagerProduct::getName))
                .ifPresent(error -> {
                    throw new BadRequestException(error);
                });
        validateRepository();
        products.forEach(this::validateProduct);
        getRuntimeVersion();
    }

    private void validateRepository() {
        validateUrl(repository.getBaseUrl(), "components.repository.baseUrl");
        if (StringUtils.isNotBlank(repository.getGpgKeyUrl())) {
            validateUrl(repository.getGpgKeyUrl(), "components.repository.gpgKeyUrl");
        }
    }

    private void validateProduct(ClouderaManagerProduct product) {
        if (product == null || StringUtils.isBlank(product.getName()) || StringUtils.isBlank(product.getVersion())) {
            throw new BadRequestException("Invalid upgrade components: each product requires a name and version.");
        }
        validateUrl(product.getParcel(), "components.products.parcel");
        if (product.getCsd() != null) {
            product.getCsd().forEach(url -> validateUrl(url, "components.products.csd"));
        }
    }

    @JsonIgnore
    public String getRuntimeVersion() {
        ClouderaManagerProduct cdh = products == null ? null : products.stream()
                .filter(product -> product != null && CDH.name().equals(product.getName())).findFirst().orElse(null);
        if (cdh == null || StringUtils.isBlank(cdh.getVersion())) {
            throw new BadRequestException("Invalid upgrade components: a CDH parcel with a version is required.");
        }
        return CdhVersionProvider.getCdhStackVersionFromVersionString(cdh.getVersion())
                .orElseThrow(() -> new BadRequestException("Invalid CDH parcel version: " + cdh.getVersion()));
    }

    private void validateUrl(String url, String field) {
        try {
            URI uri = URI.create(StringUtils.defaultString(url));
            if (uri.getHost() != null && ("http".equalsIgnoreCase(uri.getScheme()) || "https".equalsIgnoreCase(uri.getScheme()))) {
                return;
            }
        } catch (IllegalArgumentException e) {
            throw new BadRequestException("Invalid HTTP or HTTPS URL in " + field + ": " + url, e);
        }
        throw new BadRequestException("Invalid HTTP or HTTPS URL in " + field + ": " + url);
    }
}
