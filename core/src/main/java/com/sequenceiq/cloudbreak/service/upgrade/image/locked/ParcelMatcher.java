package com.sequenceiq.cloudbreak.service.upgrade.image.locked;

import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.model.ClouderaManagerProduct;
import com.sequenceiq.cloudbreak.cloud.model.catalog.Image;
import com.sequenceiq.cloudbreak.cloud.model.component.StackType;
import com.sequenceiq.cloudbreak.service.image.PreWarmParcelParser;

@Component
public class ParcelMatcher {

    @Inject
    private PreWarmParcelParser preWarmParcelParser;

    // Only compares the versions of the activated parcels
    public boolean isMatchingNonCdhParcels(Image image, Map<String, String> activatedParcels) {
        Map<String, String> prewarmedParcels = getPreWarmedParcels(image);
        return isAllActivatedParcelVersionEqualExceptCdh(activatedParcels, prewarmedParcels);
    }

    private boolean isAllActivatedParcelVersionEqualExceptCdh(Map<String, String> activatedParcels, Map<String, String> prewarmedParcels) {
        return activatedParcels.entrySet()
                .stream()
                .filter(createNonCdhParcelPredicate())
                .allMatch(activatedParcelWithVersion -> isParcelVersionEqual(prewarmedParcels, activatedParcelWithVersion));
    }

    private Predicate<Entry<String, String>> createNonCdhParcelPredicate() {
        return activatedParcel -> {
            String activatedParcelName = activatedParcel.getKey();
            return !StackType.CDH.name().equals(activatedParcelName);
        };
    }

    private boolean isParcelVersionEqual(Map<String, String> prewarmedParcels, Entry<String, String> activatedParcelWithVersion) {
        String activatedParcelName = activatedParcelWithVersion.getKey();
        String prewarmedParcelVersion = prewarmedParcels.get(activatedParcelName);
        String activatedParcelVersion = activatedParcelWithVersion.getValue();
        return activatedParcelVersion.equals(prewarmedParcelVersion);
    }

    private Map<String, String> getPreWarmedParcels(Image image) {
        return image.getPreWarmParcels()
                .stream()
                .map(parcel -> preWarmParcelParser.parseProductFromParcel(parcel))
                .flatMap(Optional::stream)
                .collect(Collectors.toMap(ClouderaManagerProduct::getName, ClouderaManagerProduct::getVersion));
    }
}
