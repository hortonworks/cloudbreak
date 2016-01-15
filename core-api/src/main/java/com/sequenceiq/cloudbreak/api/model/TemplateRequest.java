package com.sequenceiq.cloudbreak.api.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.swagger.annotations.ApiModel;

@ApiModel
@JsonIgnoreProperties(ignoreUnknown = true)
//@ValidTemplate(minCount = 1, maxCount = 24, minSize = 10, maxSize = 1000)
public class TemplateRequest extends TemplateBase {
}
