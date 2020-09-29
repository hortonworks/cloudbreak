package com.sequenceiq.cloudbreak.cmtemplate.metering;

import java.io.Serializable;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class MeteringServiceFieldMapperDefinition implements Serializable {

    private String name;

    private List<String> relatedServices;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<String> getRelatedServices() {
        return relatedServices;
    }

    public void setRelatedServices(List<String> relatedServices) {
        this.relatedServices = relatedServices;
    }
}
