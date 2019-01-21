package com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.filter;

import java.util.Set;

import javax.ws.rs.QueryParam;

import io.swagger.annotations.ApiModel;

@ApiModel
public class GetStackByNameV4Filter {

    @QueryParam("name")
    private String name;

    @QueryParam("entries")
    private Set<String> entries;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Set<String> getEntries() {
        return entries;
    }

    public void setEntries(Set<String> entries) {
        this.entries = entries;
    }
}
