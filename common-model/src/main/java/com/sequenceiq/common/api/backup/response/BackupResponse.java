package com.sequenceiq.common.api.backup.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.sequenceiq.common.api.backup.base.BackupBase;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "BackupResponse")
@JsonIgnoreProperties(ignoreUnknown = true)
public class BackupResponse extends BackupBase {
}
