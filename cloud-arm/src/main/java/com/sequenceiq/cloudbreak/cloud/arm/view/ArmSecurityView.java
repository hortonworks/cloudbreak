package com.sequenceiq.cloudbreak.cloud.arm.view;

import java.util.ArrayList;
import java.util.List;

import com.sequenceiq.cloudbreak.cloud.model.Security;
import com.sequenceiq.cloudbreak.cloud.model.SecurityRule;

public class ArmSecurityView {

    private List<ArmPortView> ports = new ArrayList<>();

    public ArmSecurityView(Security security) {
        for (SecurityRule securityRule : security.getRules()) {
            for (String port : securityRule.getPorts()) {
                ports.add(new ArmPortView(securityRule.getCidr(), port, securityRule.getProtocol()));
            }
        }
    }

    public List<ArmPortView> getPorts() {
        return ports;
    }
}