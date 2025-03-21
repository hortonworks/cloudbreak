package com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.parameter.stack;

import static com.sequenceiq.cloudbreak.cloud.PlatformParametersConsts.LOAD_BALANCER_SKU_PARAMETER;
import static com.sequenceiq.cloudbreak.cloud.PlatformParametersConsts.STANDARD_SKU_MIGRATION_PARAMETER;

import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.cloudbreak.doc.ModelDescriptions.StackModelDescription;
import com.sequenceiq.common.api.type.LoadBalancerSku;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(Include.NON_NULL)
public class AzureStackV4Parameters extends StackV4ParameterBase {

    @Schema
    private String resourceGroupName;

    @Deprecated
    @Schema
    private boolean encryptStorage;

    @Schema(description = StackModelDescription.LOAD_BALANCER_SKU)
    private LoadBalancerSku loadBalancerSku = LoadBalancerSku.getDefault();

    public String getResourceGroupName() {
        return resourceGroupName;
    }

    public void setResourceGroupName(String resourceGroupName) {
        this.resourceGroupName = resourceGroupName;
    }

    public boolean isEncryptStorage() {
        return encryptStorage;
    }

    public void setEncryptStorage(boolean encryptStorage) {
        this.encryptStorage = encryptStorage;
    }

    public LoadBalancerSku getLoadBalancerSku() {
        return loadBalancerSku;
    }

    public void setLoadBalancerSku(LoadBalancerSku loadBalancerSku) {
        this.loadBalancerSku = loadBalancerSku;
    }

    @Override
    public Map<String, Object> asMap() {
        Map<String, Object> map = super.asMap();
        putIfValueNotNull(map, "resourceGroupName", resourceGroupName);
        putIfValueNotNull(map, "encryptStorage", encryptStorage);
        putIfValueNotNull(map, LOAD_BALANCER_SKU_PARAMETER, loadBalancerSku.name());
        map.put(STANDARD_SKU_MIGRATION_PARAMETER, "NOT_NEEDED");
        return map;
    }

    @Override
    @JsonIgnore
    @Schema(hidden = true)
    public CloudPlatform getCloudPlatform() {
        return CloudPlatform.AZURE;
    }

    @Override
    public void parse(Map<String, Object> parameters) {
        super.parse(parameters);
        resourceGroupName = getParameterOrNull(parameters, "resourceGroupName");
        encryptStorage = getBoolean(parameters, "encryptStorage");
        loadBalancerSku = LoadBalancerSku.getValueOrDefault(getParameterOrNull(parameters, "loadBalancerSku"));
    }
}
