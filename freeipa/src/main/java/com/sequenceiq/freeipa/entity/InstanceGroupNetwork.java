package com.sequenceiq.freeipa.entity;

import java.util.StringJoiner;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;

import com.sequenceiq.cloudbreak.common.json.Json;
import com.sequenceiq.cloudbreak.common.json.JsonToString;

@Entity
@Table
public class InstanceGroupNetwork {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO, generator = "instancegroupnetwork_generator")
    @SequenceGenerator(name = "instancegroupnetwork_generator", sequenceName = "instancegroupnetwork_id_seq", allocationSize = 1)
    private Long id;

    private String cloudPlatform;

    @Convert(converter = JsonToString.class)
    @Column(columnDefinition = "TEXT")
    private Json attributes;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String cloudPlatform() {
        return cloudPlatform;
    }

    public void setCloudPlatform(String cloudPlatform) {
        this.cloudPlatform = cloudPlatform;
    }

    public Json getAttributes() {
        return attributes;
    }

    public void setAttributes(Json attributes) {
        this.attributes = attributes;
    }

    public String getCloudPlatform() {
        return cloudPlatform;
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", InstanceGroupNetwork.class.getSimpleName() + "[", "]")
                .add("id=" + id)
                .add("cloudPlatform='" + cloudPlatform + '\'')
                .add("attributes='" + attributes + "'")
                .toString();
    }
}

