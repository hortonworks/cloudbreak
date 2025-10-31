package com.sequenceiq.freeipa.api.v2.freeipa.stack.model.crossrealm;

import java.util.List;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import com.sequenceiq.cloudbreak.common.type.KdcType;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.doc.FreeIpaModelDescriptions;

import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.media.Schema;

public abstract class PrepareCrossRealmTrustV2KdcBaseRequest {

    @NotEmpty
    @Schema(description = FreeIpaModelDescriptions.CrossRealmTrustModelDescriptions.KDC_REALM, requiredMode = Schema.RequiredMode.REQUIRED)
    private String realm;

    @NotEmpty
    @Size(min = 1, max = 1)
    @Schema(description = FreeIpaModelDescriptions.CrossRealmTrustModelDescriptions.KDC_SERVER, requiredMode = Schema.RequiredMode.REQUIRED)
    private List<PrepareCrossRealmTrustV2KdcServerRequest> servers;

    public String getRealm() {
        return realm;
    }

    public void setRealm(String realm) {
        this.realm = realm;
    }

    public List<PrepareCrossRealmTrustV2KdcServerRequest> getServers() {
        return servers;
    }

    public void setServers(List<PrepareCrossRealmTrustV2KdcServerRequest> servers) {
        this.servers = servers;
    }

    @Hidden
    public abstract KdcType getKdcType();

    @Override
    public String toString() {
        return "PrepareCrossRealmTrustKdcBaseRequest{" +
                "realm='" + realm + '\'' +
                ", servers=" + servers +
                '}';
    }
}
