package com.sequenceiq.provisioning.domain;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;

@Entity
public class AWSCluster {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

}
