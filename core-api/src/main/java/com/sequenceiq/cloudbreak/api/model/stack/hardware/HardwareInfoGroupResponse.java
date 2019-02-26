package com.sequenceiq.cloudbreak.api.model.stack.hardware;

import java.util.HashSet;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.sequenceiq.cloudbreak.api.model.JsonEntity;
import com.sequenceiq.cloudbreak.api.model.RecoveryMode;
import com.sequenceiq.cloudbreak.doc.ModelDescriptions.HardwareModelDescription;
import com.sequenceiq.cloudbreak.doc.ModelDescriptions.HostGroupModelDescription;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel
@JsonIgnoreProperties(ignoreUnknown = true)
public class HardwareInfoGroupResponse implements JsonEntity {

    @ApiModelProperty(HostGroupModelDescription.RECOVERY_MODE)
    private RecoveryMode recoveryMode = RecoveryMode.MANUAL;

    private String name;

    @ApiModelProperty(HardwareModelDescription.METADATA)
    private Set<HardwareInfoResponse> hardwareInfos = new HashSet<>();

    public RecoveryMode getRecoveryMode() {
        return recoveryMode;
    }

    public void setRecoveryMode(RecoveryMode recoveryMode) {
        this.recoveryMode = recoveryMode;
    }

    public Set<HardwareInfoResponse> getHardwareInfos() {
        return hardwareInfos;
    }

    public void setHardwareInfos(Set<HardwareInfoResponse> hardwareInfos) {
        this.hardwareInfos = hardwareInfos;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
