package com.sequenceiq.cloudbreak.orchestrator.salt.domain;

import java.util.ArrayList;
import java.util.List;

import com.sequenceiq.cloudbreak.orchestrator.salt.client.SaltActionType;

public class SaltAction {

    private final SaltActionType action;

    /**
     * @deprecated Do not use it, it is deprecated since salt-bootstrap 0.2.2, please use master.address
     */
    @Deprecated
    private String server;

    /**
     * @deprecated Do not use it, it is deprecated since salt-bootstrap 0.11.0, please use masters
     */
    @Deprecated
    private SaltMaster master;

    private List<SaltMaster> masters;

    private List<Minion> minions;

    private Cloud cloud;

    private Os os;

    public SaltAction(SaltActionType action) {
        this.action = action;
    }

    /**
     * @deprecated Do not use it, it is deprecated since salt-bootstrap 0.2.2, please use getMaster().setAddress()
     */

    @Deprecated
    public String getServer() {
        return server;
    }

    /**
     * @deprecated Do not use it, it is deprecated since salt-bootstrap 0.2.2, please use getMaster().setAddress()
     */
    @Deprecated
    public void setServer(String server) {
        this.server = server;
    }

    public SaltActionType getAction() {
        return action;
    }

    /**
     * @deprecated Do not use it, it is deprecated since salt-bootstrap 0.11.0, please use getMasters()
     */
    @Deprecated
    public SaltMaster getMaster() {
        return master;
    }

    /**
     * @deprecated Do not use it, it is deprecated since salt-bootstrap 0.11.0, please use setMasters()
     */
    @Deprecated
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

    public List<SaltMaster> getMasters() {
        return masters;
    }

    public void setMasters(List<SaltMaster> masters) {
        this.masters = masters;
    }

    public void addMaster(SaltMaster master) {
        if (masters == null) {
            masters = new ArrayList<>();
        }
        masters.add(master);
    }

    public Cloud getCloud() {
        return cloud;
    }

    public void setCloud(Cloud cloud) {
        this.cloud = cloud;
    }

    public Os getOs() {
        return os;
    }

    public void setOs(Os os) {
        this.os = os;
    }
}
