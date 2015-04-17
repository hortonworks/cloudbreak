package com.sequenceiq.cloudbreak.controller.json;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.sequenceiq.cloudbreak.controller.validation.ValidProvisionRequest;
import com.sequenceiq.cloudbreak.controller.validation.ValidVolume;
import com.wordnik.swagger.annotations.ApiModel;

@ApiModel
@ValidProvisionRequest
@ValidVolume(minCount = 1, maxCount = 15, minSize = 10, maxSize = 1000)
@JsonIgnoreProperties(ignoreUnknown = true)
public class TemplateRequest extends TemplateBase {
}
