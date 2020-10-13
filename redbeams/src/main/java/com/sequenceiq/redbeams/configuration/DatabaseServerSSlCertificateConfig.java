package com.sequenceiq.redbeams.configuration;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javax.annotation.PostConstruct;

import org.apache.commons.lang3.StringUtils;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import com.google.common.base.Strings;
import com.google.common.collect.Sets;

@Configuration
@ConfigurationProperties(prefix = "redbeams.ssl")
public class DatabaseServerSSlCertificateConfig {

    private Map<String, String> certs = new HashMap<>();

    private Map<String, Set<String>> certCache = new HashMap<>();

    @PostConstruct
    public void setupCertsCache() {
        certCache = certs.entrySet()
                .stream()
                .filter(e -> StringUtils.isNoneBlank(e.getValue()))
                .collect(Collectors.toMap(e -> e.getKey(), e -> Sets.newHashSet(e.getValue().split(";"))));
    }

    public Map<String, String> getCerts() {
        return certs;
    }

    public Set<String> getCertsByPlatform(String cloudPlatform) {
        Set<String> actualCerts = new HashSet<>();
        if (!Strings.isNullOrEmpty(cloudPlatform)) {
            actualCerts = certCache.get(cloudPlatform.toLowerCase());
        }
        if (actualCerts == null) {
            actualCerts = new HashSet<>();
        }
        return actualCerts;
    }
}
