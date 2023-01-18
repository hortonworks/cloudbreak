package com.sequenceiq.environment.api.v1.environment.model.base;

import java.io.Serializable;

import com.sequenceiq.environment.api.v1.environment.model.AwsDataServicesV1Parameters;
import com.sequenceiq.environment.api.v1.environment.model.AzureDataServicesV1Parameters;
import com.sequenceiq.environment.api.v1.environment.model.GcpDataServicesV1Parameters;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(subTypes = { AwsDataServicesV1Parameters.class, AzureDataServicesV1Parameters.class, GcpDataServicesV1Parameters.class })
public class BaseDataServicesV1Parameters implements Serializable {

    @Override
    public int hashCode() {
        return super.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        return super.equals(obj);
    }

    @Override
    public String toString() {
        return "BaseDataServicesV1Parameters{}";
    }
}
