package com.sequenceiq.datalake;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.actuate.autoconfigure.metrics.web.servlet.WebMvcMetricsAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.web.servlet.error.ErrorMvcAutoConfiguration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

import com.sequenceiq.cloudbreak.util.OpenSSLLoaderUtil;

import springfox.documentation.swagger2.annotations.EnableSwagger2;

@EnableJpaRepositories(basePackages = { "com.sequenceiq" })
@EnableSwagger2
@SpringBootApplication(scanBasePackages = { "com.sequenceiq" }, exclude = { WebMvcMetricsAutoConfiguration.class, ErrorMvcAutoConfiguration.class })
public class DatalakeApplication {

    public static void main(String[] args) {
        OpenSSLLoaderUtil.registerOpenSSLJniProvider();
        SpringApplication.run(DatalakeApplication.class, args);
    }

}

