package com.sequenceiq.cloudbreak.service.stackpatch.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties("existingstackpatcher.active-patches.metering-azure-metadata")
public class MeteringAzureMetadataPatchConfig {

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
