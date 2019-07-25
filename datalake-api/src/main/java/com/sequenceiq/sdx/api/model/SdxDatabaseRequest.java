package com.sequenceiq.sdx.api.model;

import javax.validation.constraints.NotNull;

public class SdxDatabaseRequest {
    @NotNull
    private Boolean create;

    public Boolean getCreate() {
        return create;
    }

    public void setCreate(Boolean create) {
        this.create = create;
    }

}
