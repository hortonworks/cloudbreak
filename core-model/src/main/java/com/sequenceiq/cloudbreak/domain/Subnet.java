package com.sequenceiq.cloudbreak.domain;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.ManyToOne;

@Entity
public class Subnet implements ProvisionEntity {

    @Id
    @GeneratedValue
    private Long id;
    private String cidr;
    @ManyToOne
    private Stack stack;
    private boolean modifiable;

    public Subnet() {
    }

    public Subnet(String cidr) {
        this(cidr, true, null);
    }

    public Subnet(String cidr, boolean modifiable, Stack stack) {
        this.cidr = cidr;
        this.stack = stack;
        this.modifiable = modifiable;
    }

    public Long getId() {
        return id;
    }

    public String getCidr() {
        return cidr;
    }

    public Stack getStack() {
        return stack;
    }

    public void setStack(Stack stack) {
        this.stack = stack;
    }

    public boolean isModifiable() {
        return modifiable;
    }

    public void setModifiable(boolean modifiable) {
        this.modifiable = modifiable;
    }

    @Override
    public String toString() {
        return cidr;
    }
}
