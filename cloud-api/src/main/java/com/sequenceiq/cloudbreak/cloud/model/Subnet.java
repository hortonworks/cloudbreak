package com.sequenceiq.cloudbreak.cloud.model;

import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class Subnet {

    private final String cidr;

    @JsonCreator
    public Subnet(@JsonProperty("cidr") String cidr) {
        this.cidr = cidr;
    }

    public String getCidr() {
        return cidr;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Subnet)) {
            return false;
        }
        Subnet subnet = (Subnet) o;
        return Objects.equals(cidr, subnet.cidr);
    }

    @Override
    public int hashCode() {
        return Objects.hash(cidr);
    }

}
