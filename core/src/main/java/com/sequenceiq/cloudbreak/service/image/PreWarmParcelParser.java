package com.sequenceiq.cloudbreak.service.image;

import static org.apache.commons.lang3.StringUtils.substringAfter;
import static org.apache.commons.lang3.StringUtils.substringBefore;
import static org.apache.commons.lang3.StringUtils.substringBeforeLast;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import jakarta.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.model.ClouderaManagerProduct;

@Component
public class PreWarmParcelParser {

    private static final Logger LOGGER = LoggerFactory.getLogger(PreWarmParcelParser.class);

    @Inject
    private CsdParcelNameMatcher csdParcelNameMatcher;

    public Optional<ClouderaManagerProduct> parseProductFromParcel(List<String> parcel, List<String> csdList) {
        Optional<String> baseUrl = getBaseUrl(parcel);
        Optional<String> nameAndVersion = getNameAndVersion(parcel);
        if (baseUrl.isEmpty() || nameAndVersion.isEmpty()) {
            LOGGER.warn("Parcel URL or (name and version) could not be found in the image metadata. "
                    + "Parcel baseUrl: '{}', Parcel name and version: '{}'.", baseUrl.orElse("null"), nameAndVersion.orElse("null"));
            return Optional.empty();
        } else {
            ClouderaManagerProduct product = new ClouderaManagerProduct();
            String name = substringBefore(nameAndVersion.get(), "-");
            product.setName(name);
            product.setDisplayName(nameAndVersion.get());
            product.setVersion(substringAfter(substringBeforeLast(nameAndVersion.get(), "-"), "-"));
            product.setParcel(baseUrl.get());
            product.setParcelFileUrl(removeUnnecessaryCharacters(baseUrl.get()).concat("/").concat(nameAndVersion.get()));
            product.setCsd(collectCsdParcels(csdList, name));
            LOGGER.debug("The parsed product {}", product);
            return Optional.of(product);
        }
    }

    private Optional<String> getNameAndVersion(List<String> parcel) {
        return parcel.stream()
                .filter(parcelPart -> parcelPart.endsWith(".parcel"))
                .findFirst();
    }

    private Optional<String> getBaseUrl(List<String> parcel) {
        return parcel.stream()
                .filter(parcelPart -> parcelPart.startsWith("http://") || parcelPart.startsWith("https://"))
                .findFirst();
    }

    private List<String> collectCsdParcels(List<String> csdList, String name) {
        return csdList.stream()
                .filter(csd -> csdParcelNameMatcher.matching(csd, name))
                .collect(Collectors.toList());
    }

    private String removeUnnecessaryCharacters(String baseUrl) {
        return StringUtils.stripEnd(baseUrl, "./");
    }
}
