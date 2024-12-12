package com.sequenceiq.cloudbreak.service.java.vm;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "vm.default")
public class DefaultJavaConfigurations {

    private List<JavaConfiguration> javaVersions;

    public List<JavaConfiguration> getJavaVersions() {
        return javaVersions;
    }

    public void setJavaVersions(List<JavaConfiguration> javaVersions) {
        this.javaVersions = javaVersions;
    }

    public List<JavaConfiguration> defaultJavaConfigurationsAsList() {
        return javaVersions
                .stream()
                .sorted(new DefaultJavaConfigurationComparator())
                .collect(Collectors.toList());
    }

    static class DefaultJavaConfigurationComparator implements java.util.Comparator<JavaConfiguration> {

        @Override
        public int compare(JavaConfiguration o1, JavaConfiguration o2) {
            return o1.getVersion() - o2.getVersion();
        }
    }

}