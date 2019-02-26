package com.sequenceiq.cloudbreak.api.model;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

import javax.validation.Valid;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.sequenceiq.cloudbreak.api.model.v2.StorageLocationRequest;
import com.sequenceiq.cloudbreak.doc.ModelDescriptions.FileSystem;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel("FileSystem")
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(Include.NON_NULL)
public class FileSystemRequest extends FileSystemBase {

    @Valid
    @ApiModelProperty(FileSystem.LOCATIONS)
    private Set<StorageLocationRequest> locations = new HashSet<>();

    public Set<StorageLocationRequest> getLocations() {
        return locations;
    }

    public void setLocations(Set<StorageLocationRequest> locations) {
        this.locations = locations;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof FileSystemRequest)) {
            return false;
        }
        FileSystemRequest that = (FileSystemRequest) o;
        return Objects.equals(locations, that.locations);
    }

    @Override
    public int hashCode() {
        return Objects.hash(locations);
    }

}
