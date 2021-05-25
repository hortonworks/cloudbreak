package com.sequenceiq.cdp.databus.model;

import java.io.Serializable;

import com.cloudera.cdp.shaded.com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.cloudera.cdp.shaded.com.fasterxml.jackson.annotation.JsonInclude;
import com.cloudera.cdp.shaded.com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Header implements Serializable {

    private String name;

    private String value;

    public Header(@JsonProperty("name") String name, @JsonProperty("value") String value) {
        this.name = name;
        this.value = value;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    @Override
    public String toString() {
        return "Header{" +
                "name='" + name + '\'' +
                ", value='" + value + '\'' +
                '}';
    }
}
