package com.sequenceiq.freeipa.service.rotation;

import com.sequenceiq.cloudbreak.auth.crn.Crn;
import com.sequenceiq.cloudbreak.logger.MdcContextInfoProvider;
import com.sequenceiq.freeipa.entity.projection.FreeIpaListView;

/**
 * MdcContextInfoProvider adapter for FreeIpaListView.
 */
class FreeIpaMdcContextInfoProvider implements MdcContextInfoProvider {

    private final FreeIpaListView view;

    FreeIpaMdcContextInfoProvider(FreeIpaListView view) {
        this.view = view;
    }

    @Override
    public String getWorkspaceName() {
        return null;
    }

    @Override
    public String getEnvironmentCrn() {
        return view.environmentCrn();
    }

    @Override
    public String getResourceCrn() {
        return view.resourceCrn();
    }

    @Override
    public String getResourceName() {
        return view.name();
    }

    @Override
    public String getResourceType() {
        return "FREEIPA";
    }

    @Override
    public String getTenantName() {
        try {
            return Crn.safeFromString(view.resourceCrn()).getAccountId();
        } catch (Exception e) {
            return null;
        }
    }
}
