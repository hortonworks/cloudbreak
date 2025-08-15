package com.sequenceiq.environment.api.v1.tags.model;

import java.io.Serializable;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.sequenceiq.environment.api.doc.tag.TagDescription;
import com.sequenceiq.environment.api.v1.tags.model.request.AccountTagRequest;
import com.sequenceiq.environment.api.v1.tags.model.response.AccountTagResponse;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(subTypes = {AccountTagRequest.class, AccountTagResponse.class})
@JsonIgnoreProperties(ignoreUnknown = true)
public abstract class AccountTagBase implements Serializable {

    @NotNull
    @Schema(description = TagDescription.KEY, requiredMode = Schema.RequiredMode.REQUIRED)
    @Size(max = 63, min = 3, message = "The length of the key has to be in range of 3 to 63")
    private String key;

    @NotNull
    @Schema(description = TagDescription.VALUE, requiredMode = Schema.RequiredMode.REQUIRED)
    @Size(max = 63, min = 3, message = "The length of the value has to be in range of 3 to 63")
    private String value;

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    @Override
    public String toString() {
        return "AccountTagBase{" +
                "key='" + key + '\'' +
                ", value='" + value + '\'' +
                '}';
    }
}
