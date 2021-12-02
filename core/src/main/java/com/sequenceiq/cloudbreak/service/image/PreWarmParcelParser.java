package com.sequenceiq.cloudbreak.service.image;

import static org.apache.commons.lang3.StringUtils.substringAfter;
import static org.apache.commons.lang3.StringUtils.substringBefore;
import static org.apache.commons.lang3.StringUtils.substringBeforeLast;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.inject.Inject;

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
        Optional<String> url = parcel.stream().filter(parcelPart -> parcelPart.startsWith("http://") || parcelPart.startsWith("https://"))
                .findFirst();
        Optional<String> nameAndVersion = parcel.stream().filter(parcelPart -> parcelPart.endsWith(".parcel"))
                .findFirst();
        if (url.isEmpty() || nameAndVersion.isEmpty()) {
            LOGGER.warn("Parcel URL or (name and version) could not be found in the image metadata. "
                    + "Parcel url: '{}', Parcel name and version: '{}'.", url.orElse("null"), nameAndVersion.orElse("null"));
            return Optional.empty();
        } else {
            ClouderaManagerProduct product = new ClouderaManagerProduct();
            String name = substringBefore(nameAndVersion.get(), "-");
            product.setName(name);
            product.setDisplayName(nameAndVersion.get());
            String version = substringAfter(substringBeforeLast(nameAndVersion.get(), "-"), "-");
            product.setVersion(version);
            LOGGER.debug("The parsed product name for parcel: '{}', version: '{}'. URL: '{}'",  name, version, url.get());
            product.setParcel(url.get());
            product.setCsd(collectCsdParcels(csdList, name));
            return Optional.of(product);
        }
    }

    private List<String> collectCsdParcels(List<String> csdList, String name) {
        return csdList.stream()
                .filter(csd -> csdParcelNameMatcher.matching(csd, name))
                .collect(Collectors.toList());
    }
}
