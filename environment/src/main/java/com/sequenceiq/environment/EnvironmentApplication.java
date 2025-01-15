package com.sequenceiq.environment;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.actuate.autoconfigure.observation.web.servlet.WebMvcObservationAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.web.servlet.error.ErrorMvcAutoConfiguration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableScheduling;

import com.sequenceiq.cloudbreak.util.BouncyCastleFipsProviderLoader;

@EnableScheduling
@EnableJpaRepositories(basePackages = "com.sequenceiq")
@SpringBootApplication(scanBasePackages = "com.sequenceiq", exclude = { ErrorMvcAutoConfiguration.class, WebMvcObservationAutoConfiguration.class })
@EnableAspectJAutoProxy(proxyTargetClass = true)
public class EnvironmentApplication {

    public static void main(String[] args) {
        BouncyCastleFipsProviderLoader.load();
        SpringApplication.run(EnvironmentApplication.class, args);
    }

}
