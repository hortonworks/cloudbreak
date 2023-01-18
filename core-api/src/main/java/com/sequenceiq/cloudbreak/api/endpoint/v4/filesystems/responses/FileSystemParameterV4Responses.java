package com.sequenceiq.cloudbreak.api.endpoint.v4.filesystems.responses;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.google.common.collect.Lists;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.responses.GeneralCollectionV4Response;

import io.swagger.v3.oas.annotations.media.Schema;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(Include.NON_NULL)
@Schema
public class FileSystemParameterV4Responses extends GeneralCollectionV4Response<FileSystemParameterV4Response> {

    public FileSystemParameterV4Responses(List<FileSystemParameterV4Response> responses) {
        super(responses);
    }

    public FileSystemParameterV4Responses() {
        super(Lists.newArrayList());
    }
}
