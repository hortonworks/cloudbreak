package com.sequenceiq.it.config;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

import com.sequenceiq.cloudbreak.audit.AuditClient;

@Configuration()
@ComponentScan(basePackages = {
        "com.sequenceiq.cloudbreak.audit.config",
        "com.sequenceiq.cloudbreak.audit.converter",
        "com.sequenceiq.cloudbreak.audit.model",
        "com.sequenceiq.cloudbreak.audit.util"},
        basePackageClasses = AuditClient.class)
public class AuditBeanConfig {

}
