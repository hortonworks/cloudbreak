package com.sequenceiq.cloudbreak.domain;

import static com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility.ANY;
import static com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility.NONE;

import java.util.HashSet;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonAutoDetect(fieldVisibility = ANY, getterVisibility = NONE, setterVisibility = NONE)
@JsonIgnoreProperties(ignoreUnknown = true)
public class StorageLocations {

    private Set<StorageLocation> locations = new HashSet<>();

    public Set<StorageLocation> getLocations() {
        return locations;
    }

    public void setLocations(Set<StorageLocation> locations) {
        this.locations = locations;
    }
}
