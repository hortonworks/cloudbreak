package com.sequenceiq.cloudbreak.service.parcel;

import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;

import jakarta.inject.Inject;
import jakarta.ws.rs.core.MediaType;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.client.RestClientFactory;
import com.sequenceiq.cloudbreak.cloud.model.ClouderaManagerProduct;
import com.sequenceiq.cloudbreak.cmtemplate.configproviders.smartrouter.SmartRouterRoles;
import com.sequenceiq.cloudbreak.common.exception.CloudbreakServiceException;
import com.sequenceiq.cloudbreak.common.json.Json;

@Service
public class SmartRouterParcelService {

    public static final String COMPONENT_NAME = SmartRouterRoles.SMARTROUTER;

    private static final Logger LOGGER = LoggerFactory.getLogger(SmartRouterParcelService.class);

    @Value("${cb.smartrouter.latestCompiledBuildUrl:}")
    private String latestCompiledBuildUrl;

    @Value("${cb.smartrouter.releaseVersion:}")
    private String releaseVersion;

    @Inject
    private RestClientFactory restClientFactory;

    public ClouderaManagerProduct getProduct(String osType) {
        if (StringUtils.isEmpty(latestCompiledBuildUrl)) {
            throw new CloudbreakServiceException(String.format("No latest compiled build url provided for %s", COMPONENT_NAME));
        }
        if (StringUtils.isEmpty(releaseVersion)) {
            throw new CloudbreakServiceException(String.format("No version provided for %s", COMPONENT_NAME));
        }
        if (StringUtils.isBlank(osType)) {
            throw new CloudbreakServiceException(String.format("No OS type provided for %s", COMPONENT_NAME));
        }
        Json latestCompiledBuild = fetchLatestCompiledBuild(osType);
        if (latestCompiledBuild.getJsonNode(osType) == null) {
            throw new CloudbreakServiceException(String.format("No latest compiled build found for %s on OS %s", COMPONENT_NAME, osType));
        }

        String gbnUrl = getRequiredField(latestCompiledBuild, osType, "gbn_url");
        String buildUrl = "%s%s/%s.x".formatted(gbnUrl, COMPONENT_NAME.toLowerCase(Locale.ROOT), releaseVersion.split("\\.")[0]);

        String buildVersion = getRequiredField(latestCompiledBuild, osType, "build");
        String productVersion = "%s.%s".formatted(releaseVersion, buildVersion);
        String parcelUrl = buildUrl + "/parcels/";
        String csdUrl = "%s/csd/%s-%s.jar".formatted(buildUrl, COMPONENT_NAME, productVersion);

        ClouderaManagerProduct product = new ClouderaManagerProduct();
        product.setName(COMPONENT_NAME);
        product.setVersion(productVersion);
        product.setParcel(parcelUrl);
        product.setCsd(List.of(csdUrl));
        LOGGER.debug("Resolved {} product for OS {}: {}", COMPONENT_NAME, osType, product);
        return product;
    }

    public Optional<ClouderaManagerProduct> getSmartRouter(Set<ClouderaManagerProduct> products) {
        return products.stream()
                .filter(product -> product.getName().equalsIgnoreCase(COMPONENT_NAME))
                .findFirst();
    }

    private Json fetchLatestCompiledBuild(String osType) {
        try {
            return new Json(restClientFactory.getOrCreateDefault()
                    .target(latestCompiledBuildUrl)
                    .queryParam("stack", COMPONENT_NAME.toLowerCase(Locale.ROOT))
                    .queryParam("release", releaseVersion)
                    .queryParam("os", osType)
                    .request()
                    .accept(MediaType.APPLICATION_JSON)
                    .get(String.class));
        } catch (Exception ex) {
            throw new CloudbreakServiceException("Failed to get Smart Router component details from " + latestCompiledBuildUrl, ex);
        }
    }

    private String getRequiredField(Json latestCompiledBuild, String osType, String field) {
        String value = latestCompiledBuild.getString("%s.%s".formatted(osType, field));
        if (StringUtils.isBlank(value)) {
            throw new CloudbreakServiceException(String.format("Field '%s' is missing from the latest compiled build of %s on OS %s",
                    field, COMPONENT_NAME, osType));
        }
        return value;
    }
}
