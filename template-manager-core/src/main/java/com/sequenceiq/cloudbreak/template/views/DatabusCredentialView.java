package com.sequenceiq.cloudbreak.template.views;

import com.sequenceiq.common.api.telemetry.model.DataBusCredential;

public class DatabusCredentialView {

    private String machineUserName;

    private String accessKey;

    private String privateKey;

    private String accessKeyType;

    public DatabusCredentialView(DataBusCredential dataBusCredential) {
        this.machineUserName = dataBusCredential.getMachineUserName();
        this.accessKey = dataBusCredential.getAccessKey();
        this.privateKey = dataBusCredential.getPrivateKey();
        this.accessKeyType = dataBusCredential.getAccessKeyType();
    }

    public String getMachineUserName() {
        return machineUserName;
    }

    public String getAccessKey() {
        return accessKey;
    }

    public String getPrivateKey() {
        return privateKey;
    }

    public String getAccessKeyType() {
        return accessKeyType;
    }
}
