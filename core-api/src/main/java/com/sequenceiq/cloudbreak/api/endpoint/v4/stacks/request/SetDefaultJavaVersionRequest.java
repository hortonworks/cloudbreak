package com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

import jakarta.validation.constraints.Pattern;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema
@JsonIgnoreProperties(ignoreUnknown = true)
public class SetDefaultJavaVersionRequest {

    @Pattern(regexp = "^(8|11|17|21|22|23|24|25|26)$",
            message = "The requested default Java version is not valid. Valid versions are 8, 11, 17, 21, 22, 23, 24, 25, 26")
    @Schema(description = "Default java version", requiredMode = REQUIRED)
    private String defaultJavaVersion;

    @Schema(description = "Restart services after setting the default java version", defaultValue = "false")
    private boolean restartServices;

    @Schema(description = "Restart CM after setting the default java version", defaultValue = "false")
    private boolean restartCM;

    public String getDefaultJavaVersion() {
        return defaultJavaVersion;
    }

    public void setDefaultJavaVersion(String defaultJavaVersion) {
        this.defaultJavaVersion = defaultJavaVersion;
    }

    public boolean isRestartServices() {
        return restartServices;
    }

    public void setRestartServices(boolean restartServices) {
        this.restartServices = restartServices;
    }

    public boolean isRestartCM() {
        return restartCM;
    }

    public void setRestartCM(boolean restartCM) {
        this.restartCM = restartCM;
    }
}
