package com.sequenceiq.cloudbreak.service.upgrade.sync.operationresult;

import java.util.Set;

import com.sequenceiq.cloudbreak.cloud.model.ClouderaManagerProduct;
import com.sequenceiq.cloudbreak.service.upgrade.sync.common.ParcelInfo;

public class CmParcelSyncOperationResult {

    private final Set<ParcelInfo> installedParcels;

    private final Set<ClouderaManagerProduct> foundCmProducts;

    public CmParcelSyncOperationResult(Set<ParcelInfo> installedParcels, Set<ClouderaManagerProduct> foundCmProducts) {
        this.installedParcels = installedParcels;
        this.foundCmProducts = foundCmProducts;
    }

    public Set<ParcelInfo> getInstalledParcels() {
        return installedParcels;
    }

    public Set<ClouderaManagerProduct> getFoundCmProducts() {
        return foundCmProducts;
    }

    public boolean isEmpty() {
        return installedParcels.isEmpty();
    }

    @Override
    public String toString() {
        return "CmParcelSyncOperationResult{" +
                "installedParcels=" + installedParcels +
                ", foundCmProducts=" + foundCmProducts +
                '}';
    }

}
