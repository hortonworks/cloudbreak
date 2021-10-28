package com.sequenceiq.cloudbreak.service.upgrade.sync;

import static com.sequenceiq.cloudbreak.cloud.model.catalog.ImagePackageVersion.CM;

import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.service.upgrade.sync.common.ParcelInfo;

@Service
public class MixedPackageMessageProvider {

    public String createActiveParcelsMessage(Set<ParcelInfo> activeParcels) {
        return activeParcels.stream()
                .map(parcel -> String.format("%s %s", parcel.getName(), parcel.getVersion()))
                .collect(Collectors.joining(", "));
    }

    public String createMessageFromMap(Map<String, String> components) {
        return components.entrySet().stream()
                .map(entry -> String.format("%s %s", entry.getKey(), entry.getValue()))
                .collect(Collectors.joining(", "));
    }

    public String createSuggestedVersionsMessage(Map<String, String> clouderaManagerProducts, Set<ParcelInfo> activeParcels, String suggestedCmVersion) {
        String suggestedParcelsMessage = clouderaManagerProducts.entrySet().stream()
                .filter(product -> activeParcels.stream().anyMatch(parcelInfo -> parcelInfo.getName().equals(product.getKey())))
                .map(product -> String.format("%s %s", product.getKey(), product.getValue()))
                .collect(Collectors.joining(", "));
        return String.format(CM.getDisplayName() + " %s, %s", suggestedCmVersion, suggestedParcelsMessage);
    }
}
