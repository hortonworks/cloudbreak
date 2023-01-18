package com.sequenceiq.common.api.type;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "DataHubStartAction")
public enum DataHubStartAction {
    START_ALL,
    DO_NOT_START
}
