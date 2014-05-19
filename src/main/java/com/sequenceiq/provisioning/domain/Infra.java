package com.sequenceiq.provisioning.domain;


import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.MappedSuperclass;

@MappedSuperclass
public abstract class Infra {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    public Infra() {

    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }
}
