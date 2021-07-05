package com.sequenceiq.environment.api.v1.tags.model;

import java.io.Serializable;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.sequenceiq.environment.api.doc.tag.TagDescription;
import com.sequenceiq.environment.api.v1.tags.model.request.AccountTagRequest;
import com.sequenceiq.environment.api.v1.tags.model.response.AccountTagResponse;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel(subTypes = {AccountTagRequest.class, AccountTagResponse.class})
@JsonIgnoreProperties(ignoreUnknown = true)
public abstract class AccountTagBase implements Serializable {

    @NotNull
    @ApiModelProperty(value = TagDescription.KEY, required = true)
    @Size(max = 127, min = 3, message = "The length of the key has to be in range of 3 to 127")
    private String key;

    @NotNull
    @ApiModelProperty(value = TagDescription.VALUE, required = true)
    @Size(max = 255, min = 3, message = "The length of the value has to be in range of 3 to 255")
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
