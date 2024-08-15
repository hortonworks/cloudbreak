package com.sequenceiq.cloudbreak.service.java.vm;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "vm.default")
public class DefaultJavaConfigurations {

    private List<DefaultJavaConfiguration> javaVersions;

    public List<DefaultJavaConfiguration> getJavaVersions() {
        return javaVersions;
    }

    public void setJavaVersions(List<DefaultJavaConfiguration> javaVersions) {
        this.javaVersions = javaVersions;
    }

    public List<DefaultJavaConfiguration> defaultJavaConfigurationsAsList() {
        return javaVersions
                .stream()
                .sorted(new DefaultJavaConfigurationComparator())
                .collect(Collectors.toList());
    }

    class DefaultJavaConfigurationComparator implements java.util.Comparator<DefaultJavaConfiguration> {

        @Override
        public int compare(DefaultJavaConfiguration o1, DefaultJavaConfiguration o2) {
            return o1.getVersion() - o2.getVersion();
        }
    }
}