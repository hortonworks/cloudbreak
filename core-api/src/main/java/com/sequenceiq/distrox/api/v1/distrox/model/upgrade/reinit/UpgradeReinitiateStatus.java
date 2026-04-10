package com.sequenceiq.distrox.api.v1.distrox.model.upgrade.reinit;

public enum UpgradeReinitiateStatus {
    REINITIABLE,
    NON_REINITIABLE;

    public boolean reinitiable() {
        return REINITIABLE.equals(this);
    }
}
