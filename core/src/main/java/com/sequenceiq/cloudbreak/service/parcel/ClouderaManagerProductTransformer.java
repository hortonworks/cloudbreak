package com.sequenceiq.cloudbreak.service.parcel;

import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.model.ClouderaManagerProduct;
import com.sequenceiq.cloudbreak.cloud.model.catalog.Image;
import com.sequenceiq.cloudbreak.cloud.model.component.StackRepoDetails;
import com.sequenceiq.cloudbreak.service.image.PreWarmParcelParser;

@Component
public class ClouderaManagerProductTransformer {

    private static final Logger LOGGER = LoggerFactory.getLogger(ClouderaManagerProductTransformer.class);

    @Inject
    private PreWarmParcelParser preWarmParcelParser;

    public Set<ClouderaManagerProduct> transform(Image image, boolean getCdhParcel, boolean getPreWarmParcels) {
        if (image.isPrewarmed()) {
            Set<ClouderaManagerProduct> products = new HashSet<>();
            if (getCdhParcel) {
                products.add(getCdhParcel(image));
            }
            if (getPreWarmParcels) {
                products.addAll(getPreWarmParcels(image));
            }
            return products;
        } else {
            LOGGER.debug("Not possible to get the products from the image because this is not a pre warmed image. ImageId: {}", image.getUuid());
            return Collections.emptySet();
        }
    }

    public Map<String, String> transformToMap(Image image, boolean getCdhParcel, boolean getPreWarmParcels) {
        return transform(image, getCdhParcel, getPreWarmParcels)
                .stream()
                .collect(Collectors.toMap(ClouderaManagerProduct::getName, ClouderaManagerProduct::getVersion));
    }

    private Set<ClouderaManagerProduct> getPreWarmParcels(Image image) {
        return image.getPreWarmParcels()
                .stream()
                .map(parcel -> preWarmParcelParser.parseProductFromParcel(parcel, image.getPreWarmCsd()))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .collect(Collectors.toSet());
    }

    private ClouderaManagerProduct getCdhParcel(Image image) {
        Map<String, String> stackInfo = image.getStackDetails().getRepo().getStack();
        return new ClouderaManagerProduct()
                .withVersion(stackInfo.get(StackRepoDetails.REPOSITORY_VERSION))
                .withName(stackInfo.get(StackRepoDetails.REPO_ID_TAG).split("-")[0])
                .withParcel(stackInfo.get(image.getOsType()));
    }

}
