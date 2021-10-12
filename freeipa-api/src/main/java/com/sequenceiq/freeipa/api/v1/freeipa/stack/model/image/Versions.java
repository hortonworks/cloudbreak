package com.sequenceiq.freeipa.api.v1.freeipa.stack.model.image;

import java.util.List;
import java.util.Optional;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Versions {

    private static final String FREEIPA_VERSIONS_PROPERTY = "freeipa";

    private final List<FreeIpaVersions> freeIpaVersions;

    @JsonCreator
    public Versions(@JsonProperty(FREEIPA_VERSIONS_PROPERTY) List<FreeIpaVersions> freeIpaVersions) {
        this.freeIpaVersions = Optional.ofNullable(freeIpaVersions).orElse(List.of());
    }

    @JsonProperty(FREEIPA_VERSIONS_PROPERTY)
    public List<FreeIpaVersions> getFreeIpaVersions() {
        return freeIpaVersions;
    }

    @Override
    public String toString() {
        final StringBuffer sb = new StringBuffer("Versions{");
        sb.append("freeIpaVersions=").append(freeIpaVersions);
        sb.append('}');
        return sb.toString();
    }
}
