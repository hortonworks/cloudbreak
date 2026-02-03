package com.sequenceiq.cloudbreak.service.upgrade.image.locked;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.stream.Collectors;

import jakarta.inject.Inject;

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
        Map<String, String> activeNonCdhParcels = new HashMap<>(activatedParcels);
        activeNonCdhParcels.remove(StackType.CDH.name());
        if (activeNonCdhParcels.isEmpty()) {
            return true;
        } else {
            Map<String, String> prewarmedParcels = getPreWarmedParcels(image);
            return activeNonCdhParcels.entrySet()
                    .stream()
                    .allMatch(activatedParcelWithVersion -> isParcelVersionEqual(prewarmedParcels, activatedParcelWithVersion));
        }
    }

    private boolean isParcelVersionEqual(Map<String, String> prewarmedParcels, Entry<String, String> activatedParcelWithVersion) {
        return prewarmedParcels.entrySet().stream()
                .anyMatch(prewarmParcelEntry ->
                        activatedParcelWithVersion.getKey().equalsIgnoreCase(prewarmParcelEntry.getKey()) &&
                        activatedParcelWithVersion.getValue().equalsIgnoreCase(prewarmParcelEntry.getValue()));
    }

    private Map<String, String> getPreWarmedParcels(Image image) {
        return image.getPreWarmParcels()
                .stream()
                .map(parcel -> preWarmParcelParser.parseProductFromParcel(parcel, image.getPreWarmCsd()))
                .flatMap(Optional::stream)
                .collect(Collectors.toMap(ClouderaManagerProduct::getName, ClouderaManagerProduct::getVersion));
    }
}
