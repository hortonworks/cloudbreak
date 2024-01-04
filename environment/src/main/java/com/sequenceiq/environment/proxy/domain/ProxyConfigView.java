package com.sequenceiq.environment.proxy.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import org.hibernate.annotations.Where;

@Entity
@Where(clause = "archived = false")
@Table(name = "proxyconfig")
public class ProxyConfigView extends ProxyConfigBase {

    @Override
    public String toString() {
        return super.toString();
    }
}
