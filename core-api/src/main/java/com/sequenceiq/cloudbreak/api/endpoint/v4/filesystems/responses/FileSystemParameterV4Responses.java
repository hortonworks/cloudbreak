package com.sequenceiq.cloudbreak.api.endpoint.v4.filesystems.responses;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.responses.GeneralListV4Response;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(Include.NON_NULL)
public class FileSystemParameterV4Responses extends GeneralListV4Response<FileSystemParameterV4Response> {

    public FileSystemParameterV4Responses(List<FileSystemParameterV4Response> responses) {
        super(responses);
    }
}
