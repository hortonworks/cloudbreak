package com.sequenceiq.cloudbreak.api.model.stack.cluster;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import io.swagger.annotations.ApiModel;

@ApiModel
@JsonIgnoreProperties(ignoreUnknown = true)
public class ClusterTemplateRequest extends ClusterTemplateBase {

}
