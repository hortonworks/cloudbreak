package com.sequenceiq.environment;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.actuate.autoconfigure.metrics.web.servlet.WebMvcMetricsAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.web.servlet.error.ErrorMvcAutoConfiguration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableScheduling;

import com.sequenceiq.cloudbreak.util.OpenSSLLoaderUtil;

import springfox.documentation.swagger2.annotations.EnableSwagger2;

@EnableSwagger2
@EnableScheduling
@EnableJpaRepositories(basePackages = "com.sequenceiq")
@SpringBootApplication(scanBasePackages = "com.sequenceiq", exclude = { WebMvcMetricsAutoConfiguration.class, ErrorMvcAutoConfiguration.class })
@EnableAspectJAutoProxy(proxyTargetClass = true)
public class EnvironmentApplication {

    public static void main(String[] args) {
        OpenSSLLoaderUtil.registerOpenSSLJniProvider();
        SpringApplication.run(EnvironmentApplication.class, args);
    }

}