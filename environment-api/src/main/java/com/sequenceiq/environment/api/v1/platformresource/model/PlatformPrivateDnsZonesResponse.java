package com.sequenceiq.environment.api.v1.platformresource.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import jakarta.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class PlatformPrivateDnsZonesResponse implements Serializable {

    private List<PlatformPrivateDnsZoneResponse> privateDnsZoneResponses = new ArrayList<>();

    public PlatformPrivateDnsZonesResponse() {
    }

    public PlatformPrivateDnsZonesResponse(@NotNull List<PlatformPrivateDnsZoneResponse> privateDnsZoneResponses) {
        this.privateDnsZoneResponses = privateDnsZoneResponses;
    }

    public List<PlatformPrivateDnsZoneResponse> getPrivateDnsZoneResponses() {
        return privateDnsZoneResponses;
    }

    public void setPrivateDnsZoneResponses(List<PlatformPrivateDnsZoneResponse> privateDnsZoneResponses) {
        this.privateDnsZoneResponses = privateDnsZoneResponses;
    }

    @Override
    public String toString() {
        return "PlatformPrivateDnsZonesResponse{" +
                "privateDnsZoneResponses=" + privateDnsZoneResponses +
                '}';
    }

}
