package com.sequenceiq.cloudbreak.controller.json;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.sequenceiq.cloudbreak.controller.validation.template.ValidTemplate;
import com.wordnik.swagger.annotations.ApiModel;

@ApiModel
@JsonIgnoreProperties(ignoreUnknown = true)
@ValidTemplate(minCount = 1, maxCount = 24, minSize = 10, maxSize = 1000)
public class TemplateRequest extends TemplateBase {
}
