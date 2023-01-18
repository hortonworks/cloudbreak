package com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.image;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "ImageSettingsV1Request")
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(Include.NON_NULL)
public class ImageSettingsRequest extends ImageSettingsBase {
}
