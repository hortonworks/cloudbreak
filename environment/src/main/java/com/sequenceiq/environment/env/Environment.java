package com.sequenceiq.environment.env;

import javax.persistence.Column;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.SequenceGenerator;

public class Environment {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO, generator = "environment_generator")
    @SequenceGenerator(name = "environment_generator", sequenceName = "environment_id_seq", allocationSize = 1)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(length = 1000000, columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false)
    private String cloudPlatform;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getCloudPlatform() {
        return cloudPlatform;
    }

    public void setCloudPlatform(String cloudPlatform) {
        this.cloudPlatform = cloudPlatform;
    }

    public static final class EnvironmentBuilder {
        private String name;

        private String description;

        private String cloudPlatform;

        private EnvironmentBuilder() {
        }

        public static EnvironmentBuilder anEnvironment() {
            return new EnvironmentBuilder();
        }

        public EnvironmentBuilder withName(String name) {
            this.name = name;
            return this;
        }

        public EnvironmentBuilder withDescription(String description) {
            this.description = description;
            return this;
        }

        public EnvironmentBuilder withCloudPlatform(String cloudPlatform) {
            this.cloudPlatform = cloudPlatform;
            return this;
        }

        public Environment build() {
            Environment environment = new Environment();
            environment.setName(name);
            environment.setDescription(description);
            environment.setCloudPlatform(cloudPlatform);
            return environment;
        }
    }
}
