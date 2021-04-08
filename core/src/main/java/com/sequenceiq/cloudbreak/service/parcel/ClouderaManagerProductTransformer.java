package com.sequenceiq.cloudbreak.service.parcel;

import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.model.ClouderaManagerProduct;
import com.sequenceiq.cloudbreak.cloud.model.catalog.Image;
import com.sequenceiq.cloudbreak.cloud.model.component.StackRepoDetails;
import com.sequenceiq.cloudbreak.service.image.PreWarmParcelParser;

@Component
public class ClouderaManagerProductTransformer {

    @Inject
    private PreWarmParcelParser preWarmParcelParser;

    public Set<ClouderaManagerProduct> transform(Image image) {
        Set<ClouderaManagerProduct> products = new HashSet<>();
        products.add(getCdhParcel(image));
        products.addAll(getPreWarmParcels(image));
        return products;
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
