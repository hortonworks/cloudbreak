package com.sequenceiq.cloudbreak.api.endpoint.v4.flexsubscription.responses;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.sequenceiq.cloudbreak.api.endpoint.v4.flexsubscription.requests.FlexSubscriptionV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.smartsense.responses.SmartSenseSubscriptionV4Response;
import com.sequenceiq.cloudbreak.doc.ModelDescriptions;
import com.sequenceiq.cloudbreak.doc.ModelDescriptions.FlexSubscriptionModelDescription;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class FlexSubscriptionV4Response extends FlexSubscriptionV4Request {

    @ApiModelProperty(value = ModelDescriptions.ID, readOnly = true)
    private Long id;

    @ApiModelProperty(value = FlexSubscriptionModelDescription.SMARTSENSE_SUBSCRIPTION, readOnly = true)
    private SmartSenseSubscriptionV4Response smartSenseSubscription;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public SmartSenseSubscriptionV4Response getSmartSenseSubscription() {
        return smartSenseSubscription;
    }

    public void setSmartSenseSubscription(SmartSenseSubscriptionV4Response smartSenseSubscription) {
        this.smartSenseSubscription = smartSenseSubscription;
    }

}
