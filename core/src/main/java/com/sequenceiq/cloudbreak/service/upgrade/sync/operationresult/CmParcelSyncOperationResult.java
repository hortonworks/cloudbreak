package com.sequenceiq.cloudbreak.service.upgrade.sync.operationresult;

import java.util.Set;

import com.sequenceiq.cloudbreak.cloud.model.ClouderaManagerProduct;
import com.sequenceiq.cloudbreak.service.upgrade.sync.common.ParcelInfo;

public class CmParcelSyncOperationResult {

    private final Set<ParcelInfo> activeParcels;

    private final Set<ClouderaManagerProduct> foundCmProducts;

    public CmParcelSyncOperationResult(Set<ParcelInfo> activeParcels, Set<ClouderaManagerProduct> foundCmProducts) {
        this.activeParcels = activeParcels;
        this.foundCmProducts = foundCmProducts;
    }

    public Set<ParcelInfo> getActiveParcels() {
        return activeParcels;
    }

    public Set<ClouderaManagerProduct> getFoundCmProducts() {
        return foundCmProducts;
    }

    public boolean isEmpty() {
        return activeParcels.isEmpty();
    }

    @Override
    public String toString() {
        return "CmParcelSyncOperationResult{" +
                "activeParcels=" + activeParcels +
                ", foundCmProducts=" + foundCmProducts +
                '}';
    }
}
