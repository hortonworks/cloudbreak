package com.sequenceiq.freeipa.entity;

import java.util.Objects;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.SequenceGenerator;

import com.fasterxml.jackson.annotation.JsonBackReference;

@Entity
public class DynamicEntitlement {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO, generator = "dynamicentitlement_generator")
    @SequenceGenerator(name = "dynamicentitlement_generator", sequenceName = "dynamicentitlement_id_seq", allocationSize = 1)
    private Long id;

    @Column(nullable = false)
    private String entitlement;

    private Boolean entitlementValue;

    @ManyToOne(fetch = FetchType.LAZY)
    @JsonBackReference
    private Stack stack;

    public DynamicEntitlement() {
    }

    public DynamicEntitlement(String entitlement, Boolean entitlementValue, Stack stack) {
        this.entitlement = entitlement;
        this.entitlementValue = entitlementValue;
        this.stack = stack;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getEntitlement() {
        return entitlement;
    }

    public void setEntitlement(String entitlement) {
        this.entitlement = entitlement;
    }

    public Boolean getEntitlementValue() {
        return entitlementValue;
    }

    public void setEntitlementValue(Boolean entitlementValue) {
        this.entitlementValue = entitlementValue;
    }

    public Stack getStack() {
        return stack;
    }

    public void setStack(Stack stack) {
        this.stack = stack;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        } else if (o == null || getClass() != o.getClass()) {
            return false;
        } else {
            DynamicEntitlement dynamicEntitlement = (DynamicEntitlement) o;
            return Objects.equals(entitlement, dynamicEntitlement.entitlement);
        }
    }

    @Override
    public int hashCode() {
        return Objects.hash(entitlement);
    }

}
