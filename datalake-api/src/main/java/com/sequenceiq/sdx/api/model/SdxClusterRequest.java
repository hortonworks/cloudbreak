package com.sequenceiq.sdx.api.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SdxClusterRequest extends SdxClusterRequestBase {

    @Schema(description = ModelDescriptions.RUNTIME_VERSION)
    private String runtime;

    /**
     * @deprecated use imageSettingsV4Request's os field instead
     */
    @Schema(description = ModelDescriptions.OS)
    @Deprecated
    private String os;

    @Schema(description = ModelDescriptions.ARCHITECTURE)
    private String architecture;

    public String getRuntime() {
        return runtime;
    }

    public void setRuntime(String runtime) {
        this.runtime = runtime;
    }

    @Deprecated
    public String getOs() {
        return os;
    }

    @Deprecated
    public void setOs(String os) {
        this.os = os;
    }

    public String getArchitecture() {
        return architecture;
    }

    public void setArchitecture(String architecture) {
        this.architecture = architecture;
    }

    @Override
    public String toString() {
        return "SdxClusterRequest{" +
                "runtime='" + runtime + '\'' +
                ", os='" + os + '\'' +
                ", architecture='" + architecture + '\'' +
                "} " + super.toString();
    }
}
