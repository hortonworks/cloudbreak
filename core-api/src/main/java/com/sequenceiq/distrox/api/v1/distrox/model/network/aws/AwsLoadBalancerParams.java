package com.sequenceiq.distrox.api.v1.distrox.model.network.aws;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;

import java.io.Serializable;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.cloudbreak.common.mappable.MappableBase;
import com.sequenceiq.cloudbreak.common.network.LoadBalancerConstants;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(NON_NULL)
public class AwsLoadBalancerParams extends MappableBase implements Serializable {

    @Schema
    private Boolean stickySessionForLoadBalancerTarget = Boolean.FALSE;

    public Boolean getStickySessionForLoadBalancerTarget() {
        return stickySessionForLoadBalancerTarget;
    }

    public void setStickySessionForLoadBalancerTarget(Boolean stickySessionForLoadBalancerTarget) {
        this.stickySessionForLoadBalancerTarget = stickySessionForLoadBalancerTarget;
    }

    @Override
    @JsonIgnore
    @Schema(hidden = true)
    public CloudPlatform getCloudPlatform() {
        return CloudPlatform.AWS;
    }

    @Override
    public void parse(Map<String, Object> parameters) {
        stickySessionForLoadBalancerTarget = getBoolean(parameters, LoadBalancerConstants.STICKY_SESSION_FOR_LOAD_BALANCER_TARGET);
    }

    @Override
    public Map<String, Object> asMap() {
        Map<String, Object> map = super.asMap();
        putIfValueNotNull(map, LoadBalancerConstants.STICKY_SESSION_FOR_LOAD_BALANCER_TARGET, stickySessionForLoadBalancerTarget);
        return map;
    }

}
