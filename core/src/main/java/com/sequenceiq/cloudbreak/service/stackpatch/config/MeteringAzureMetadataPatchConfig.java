package com.sequenceiq.cloudbreak.service.stackpatch.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import com.sequenceiq.cloudbreak.job.stackpatcher.config.StackPatchTypeConfig;

@Configuration
@ConfigurationProperties("existing-stack-patcher.patch-configs.metering-azure-metadata")
public class MeteringAzureMetadataPatchConfig extends StackPatchTypeConfig {

    private String dateBefore;

    private String customRpmUrl;

    public String getDateBefore() {
        return dateBefore;
    }

    public void setDateBefore(String dateBefore) {
        this.dateBefore = dateBefore;
    }

    public String getCustomRpmUrl() {
        return customRpmUrl;
    }

    public void setCustomRpmUrl(String customRpmUrl) {
        this.customRpmUrl = customRpmUrl;
    }
}
