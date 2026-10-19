package com.sequenceiq.it.cloudbreak.cloud.v4;

public class FreeIpaUpgrade {

    private FreeIpaProperties.Upgrade ha = new FreeIpaProperties.Upgrade();

    private FreeIpaProperties.Upgrade single = new FreeIpaProperties.Upgrade();

    public FreeIpaProperties.Upgrade getSingle() {
        return single;
    }

    public void setSingle(FreeIpaProperties.Upgrade single) {
        this.single = single;
    }

    public FreeIpaProperties.Upgrade getHA() {
        return ha;
    }

    public void setHA(FreeIpaProperties.Upgrade ha) {
        this.ha = ha;
    }
}
