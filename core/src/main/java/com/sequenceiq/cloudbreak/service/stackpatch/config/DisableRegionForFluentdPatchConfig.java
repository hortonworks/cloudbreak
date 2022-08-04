package com.sequenceiq.cloudbreak.service.stackpatch.config;

import java.lang.module.ModuleDescriptor;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import com.sequenceiq.cloudbreak.job.stackpatcher.config.StackPatchTypeConfig;

@Configuration
@ConfigurationProperties("existing-stack-patcher.patch-configs.disable-region-for-fluentd")
public class DisableRegionForFluentdPatchConfig extends StackPatchTypeConfig {

    private String affectedVersionFrom;

    public String getAffectedVersionFrom() {
        return affectedVersionFrom;
    }

    public void setAffectedVersionFrom(String affectedVersionFrom) {
        this.affectedVersionFrom = affectedVersionFrom;
    }

    public ModuleDescriptor.Version getVersionModelFromAffectedVersion() {
        return ModuleDescriptor.Version.parse(affectedVersionFrom);
    }
}
