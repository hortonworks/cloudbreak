package com.sequenceiq.distrox.v1.distrox.converter;

import static com.sequenceiq.cloudbreak.util.NullUtil.getIfNotNull;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.parameter.instancegroup.AwsInstanceGroupV4Parameters;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.parameter.instancegroup.AzureAvailabiltySetV4;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.parameter.instancegroup.AzureInstanceGroupV4Parameters;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.parameter.instancegroup.GcpInstanceGroupV4Parameters;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.parameter.instancegroup.OpenStackInstanceGroupV4Parameters;
import com.sequenceiq.distrox.api.v1.distrox.model.instancegroup.AwsInstanceGroupV1Parameters;
import com.sequenceiq.distrox.api.v1.distrox.model.instancegroup.AzureAvailabiltySetV1Parameters;
import com.sequenceiq.distrox.api.v1.distrox.model.instancegroup.AzureInstanceGroupV1Parameters;
import com.sequenceiq.distrox.api.v1.distrox.model.instancegroup.GcpInstanceGroupV1Parameters;
import com.sequenceiq.distrox.api.v1.distrox.model.instancegroup.OpenstackInstanceGroupV1Parameters;

@Component
public class InstanceGroupParameterConverter {

    public AzureInstanceGroupV4Parameters convert(AzureInstanceGroupV1Parameters source) {
        AzureInstanceGroupV4Parameters response = new AzureInstanceGroupV4Parameters();
        response.setAvailabilitySet(getIfNotNull(source.getAvailabilitySet(), this::getAvailabilitySet));
        return response;
    }

    private AzureAvailabiltySetV4 getAvailabilitySet(AzureAvailabiltySetV1Parameters source) {
        AzureAvailabiltySetV4 response = new AzureAvailabiltySetV4();
        response.setFaultDomainCount(source.getFaultDomainCount());
        response.setName(source.getName());
        response.setUpdateDomainCount(source.getUpdateDomainCount());
        return response;
    }

    public AzureInstanceGroupV1Parameters convert(AzureInstanceGroupV4Parameters source) {
        AzureInstanceGroupV1Parameters response = new AzureInstanceGroupV1Parameters();
        response.setAvailabilitySet(getIfNotNull(source.getAvailabilitySet(), this::getAvailabilitySet));
        return response;
    }

    private AzureAvailabiltySetV1Parameters getAvailabilitySet(AzureAvailabiltySetV4 source) {
        AzureAvailabiltySetV1Parameters response = new AzureAvailabiltySetV1Parameters();
        response.setFaultDomainCount(source.getFaultDomainCount());
        response.setName(source.getName());
        response.setUpdateDomainCount(source.getUpdateDomainCount());
        return response;
    }

    public AwsInstanceGroupV4Parameters convert(AwsInstanceGroupV1Parameters source) {
        return new AwsInstanceGroupV4Parameters();
    }

    public AwsInstanceGroupV1Parameters convert(AwsInstanceGroupV4Parameters source) {
        return new AwsInstanceGroupV1Parameters();
    }

    public GcpInstanceGroupV4Parameters convert(GcpInstanceGroupV1Parameters source) {
        return new GcpInstanceGroupV4Parameters();
    }

    public GcpInstanceGroupV1Parameters convert(GcpInstanceGroupV4Parameters source) {
        return new GcpInstanceGroupV1Parameters();
    }

    public OpenStackInstanceGroupV4Parameters convert(OpenstackInstanceGroupV1Parameters source) {
        return new OpenStackInstanceGroupV4Parameters();
    }

    public OpenstackInstanceGroupV1Parameters convert(OpenStackInstanceGroupV4Parameters source) {
        return new OpenstackInstanceGroupV1Parameters();
    }
}
