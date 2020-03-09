package com.sequenceiq.cloudbreak.service.image;

import static org.apache.commons.lang3.StringUtils.substringAfter;
import static org.apache.commons.lang3.StringUtils.substringBefore;
import static org.apache.commons.lang3.StringUtils.substringBeforeLast;

import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.model.ClouderaManagerProduct;

@Component
public class PreWarmParcelParser {

    private static final Logger LOGGER = LoggerFactory.getLogger(PreWarmParcelParser.class);

    public Optional<ClouderaManagerProduct> parseProductFromParcel(List<String> parcel) {
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
            LOGGER.info("The parsed product name for parcel is: '{}'.", name);
            product.setName(name);
            String version = substringAfter(substringBeforeLast(nameAndVersion.get(), "-"), "-");
            LOGGER.info("The parsed product version for parcel is: '{}'.", version);
            product.setVersion(version);
            LOGGER.info("The URL of the parcel is: '{}'", url.get());
            product.setParcel(url.get());
            return Optional.of(product);
        }
    }
}
