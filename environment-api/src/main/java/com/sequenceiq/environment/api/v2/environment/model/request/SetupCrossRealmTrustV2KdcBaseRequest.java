package com.sequenceiq.environment.api.v2.environment.model.request;

import java.util.ArrayList;
import java.util.List;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import com.sequenceiq.cloudbreak.common.type.KdcType;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.doc.FreeIpaModelDescriptions.CrossRealmTrustModelDescriptions;

import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.media.Schema;

public abstract class SetupCrossRealmTrustV2KdcBaseRequest {

    @NotEmpty
    @Schema(description = CrossRealmTrustModelDescriptions.KDC_REALM, requiredMode = Schema.RequiredMode.REQUIRED)
    private String realm;

    @NotEmpty
    @Size(min = 1, max = 1)
    @Schema(description = CrossRealmTrustModelDescriptions.KDC_SERVER, requiredMode = Schema.RequiredMode.REQUIRED)
    private List<SetupCrossRealmTrustV2KdcServerRequest> servers = new ArrayList<>();

    public String getRealm() {
        return realm;
    }

    public void setRealm(String realm) {
        this.realm = realm;
    }

    public List<SetupCrossRealmTrustV2KdcServerRequest> getServers() {
        return servers;
    }

    public void setServers(List<SetupCrossRealmTrustV2KdcServerRequest> servers) {
        this.servers = servers;
    }

    @Hidden
    public abstract KdcType getKdcType();

    @Override
    public String toString() {
        return "SetupCrossRealmTrustKdcBaseRequest{" +
                "realm='" + realm + '\'' +
                ", servers=" + servers +
                '}';
    }
}
