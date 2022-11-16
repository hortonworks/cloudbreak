package com.sequenceiq.consumption;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.actuate.autoconfigure.metrics.web.servlet.WebMvcMetricsAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableScheduling;

import com.cloudera.crypto.provider.OpenSSLJniProvider;

import springfox.documentation.swagger2.annotations.EnableSwagger2;

@EnableSwagger2
@EnableScheduling
@EnableJpaRepositories(basePackages = "com.sequenceiq")
@SpringBootApplication(scanBasePackages = "com.sequenceiq", exclude = WebMvcMetricsAutoConfiguration.class)
public class ConsumptionApplication {

    public static void main(String[] args) {
        OpenSSLJniProvider.register();
        SpringApplication.run(ConsumptionApplication.class, args);
    }

}
