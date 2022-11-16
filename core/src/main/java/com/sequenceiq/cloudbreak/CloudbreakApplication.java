package com.sequenceiq.cloudbreak;

import static com.sequenceiq.cloudbreak.VersionedApplication.versionedApplication;

import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.Provider;
import java.security.Security;
import java.util.Arrays;
import java.util.stream.Collectors;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.actuate.autoconfigure.metrics.web.servlet.WebMvcMetricsAutoConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.context.annotation.FilterType;
import org.springframework.data.envers.repository.support.EnversRevisionRepositoryFactoryBean;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableAsync;

import com.sequenceiq.cloudbreak.structuredevent.service.CDPFlowStructuredEventHandler;
import com.sequenceiq.cloudbreak.util.FipsOpenSSLLoaderUtil;

import springfox.documentation.swagger2.annotations.EnableSwagger2;

@EnableAsync
@EnableSwagger2
@ComponentScan(basePackages = "com.sequenceiq",
        //TODO eliminate this exclude filter for CDPFlowStructuredEventHandler.class with https://jira.cloudera.com/browse/CB-18923
        excludeFilters = { @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, value = CDPFlowStructuredEventHandler.class) })
@EnableAspectJAutoProxy(proxyTargetClass = true)
@EnableJpaAuditing
@EnableJpaRepositories(basePackages = "com.sequenceiq", repositoryFactoryBeanClass = EnversRevisionRepositoryFactoryBean.class)
@EnableAutoConfiguration(exclude = WebMvcMetricsAutoConfiguration.class)
public class CloudbreakApplication {
    public static void main(String[] args) {
        FipsOpenSSLLoaderUtil.registerOpenSSLJniProvider();
        Provider[] providers = Security.getProviders();
        System.out.println("####### ####### SECURITY PROVIDERS BEFORE STARTING SPRING APPLICATION: "
                + Arrays.stream(providers).map(Provider::getName).collect(Collectors.joining(",")));
        try {
            KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
            String name = keyGen.getProvider().getName();
            System.out.println("####### ####### DEFAULT SECURITY PROVIDER: " + name);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }

        if (!versionedApplication().showVersionInfo(args)) {
            if (args.length == 0) {
                SpringApplication.run(CloudbreakApplication.class);
            } else {
                SpringApplication.run(CloudbreakApplication.class, args);
            }
        }
    }
}