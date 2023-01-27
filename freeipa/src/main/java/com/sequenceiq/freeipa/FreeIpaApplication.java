package com.sequenceiq.freeipa;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.actuate.autoconfigure.metrics.web.servlet.WebMvcMetricsAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

import com.sequenceiq.cloudbreak.util.FipsOpenSSLLoaderUtil;

import springfox.documentation.swagger2.annotations.EnableSwagger2;

@EnableSwagger2
@EnableJpaRepositories(basePackages = { "com.sequenceiq" })
@SpringBootApplication(scanBasePackages = "com.sequenceiq", exclude = WebMvcMetricsAutoConfiguration.class)
public class FreeIpaApplication {

    public static void main(String[] args) {
        FipsOpenSSLLoaderUtil.registerOpenSSLJniProvider();
        SpringApplication.run(FreeIpaApplication.class, args);
    }

}

