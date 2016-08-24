package com.sequenceiq.cloudbreak.orchestrator.salt.domain;

import java.util.ArrayList;
import java.util.List;

import com.sequenceiq.cloudbreak.orchestrator.salt.client.SaltActionType;

public class SaltAction {

    private SaltActionType action;

    /**
     * @deprecated  Do not use it, it is deprecated since salt-bootstrap 0.2.2, please use master.address
     */
    @Deprecated
    private String server;

    private SaltMaster master;

    private List<Minion> minions;

    public SaltAction(SaltActionType action) {
        this.action = action;
    }

    /**
     * @deprecated  Do not use it, it is deprecated since salt-bootstrap 0.2.2, please use getMaster().setAddress()
     */

    @Deprecated
    public String getServer() {
        return server;
    }

    /**
     * @deprecated  Do not use it, it is deprecated since salt-bootstrap 0.2.2, please use getMaster().setAddress()
     */

    @Deprecated
    public void setServer(String server) {
        this.server = server;
    }

    public SaltActionType getAction() {
        return action;
    }

    public SaltMaster getMaster() {
        return master;
    }

    public void setMaster(SaltMaster master) {
        this.master = master;
    }

    public List<Minion> getMinions() {
        return minions;
    }

    public void setMinions(List<Minion> minions) {
        this.minions = minions;
    }

    public void addMinion(Minion minion) {
        if (minions == null) {
            minions = new ArrayList<>();
        }
        minions.add(minion);
    }
}
