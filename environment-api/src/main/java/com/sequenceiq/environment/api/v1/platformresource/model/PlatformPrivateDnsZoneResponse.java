package com.sequenceiq.environment.api.v1.platformresource.model;

import java.io.Serializable;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema
@JsonIgnoreProperties(ignoreUnknown = true)
public class PlatformPrivateDnsZoneResponse implements Serializable {

    private String id;

    public PlatformPrivateDnsZoneResponse() {
    }

    public PlatformPrivateDnsZoneResponse(String id) {
        this.id = id;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    @Override
    public String toString() {
        return "PlatformPrivateDnsZoneResponse{" +
                "id='" + id + '\'' +
                '}';
    }

}