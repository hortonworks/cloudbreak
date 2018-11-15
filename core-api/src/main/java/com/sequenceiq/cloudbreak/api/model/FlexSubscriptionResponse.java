package com.sequenceiq.cloudbreak.api.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.sequenceiq.cloudbreak.doc.ModelDescriptions;
import com.sequenceiq.cloudbreak.doc.ModelDescriptions.FlexSubscriptionModelDescription;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel
@JsonIgnoreProperties(ignoreUnknown = true)
public class FlexSubscriptionResponse extends FlexSubscriptionRequest {

    @ApiModelProperty(value = ModelDescriptions.ID, readOnly = true)
    private Long id;

    @ApiModelProperty(value = ModelDescriptions.PUBLIC_IN_ACCOUNT, readOnly = true)
    private boolean publicInAccount = true;

    @ApiModelProperty(value = FlexSubscriptionModelDescription.SMARTSENSE_SUBSCRIPTION, readOnly = true)
    private SmartSenseSubscriptionJson smartSenseSubscription;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public boolean isPublicInAccount() {
        return publicInAccount;
    }

    public void setPublicInAccount(boolean publicInAccount) {
        this.publicInAccount = publicInAccount;
    }

    public SmartSenseSubscriptionJson getSmartSenseSubscription() {
        return smartSenseSubscription;
    }

    public void setSmartSenseSubscription(SmartSenseSubscriptionJson smartSenseSubscription) {
        this.smartSenseSubscription = smartSenseSubscription;
    }

}
