package com.sequenceiq.remoteenvironment;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.actuate.autoconfigure.observation.web.servlet.WebMvcObservationAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.web.servlet.error.ErrorMvcAutoConfiguration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

import com.sequenceiq.cloudbreak.util.BouncyCastleFipsProviderLoader;

@EnableJpaRepositories(basePackages = {"com.sequenceiq"})
@SpringBootApplication(scanBasePackages = {"com.sequenceiq"}, exclude = {ErrorMvcAutoConfiguration.class, WebMvcObservationAutoConfiguration.class})
@EnableAspectJAutoProxy(proxyTargetClass = true)
public class RemoteEnvironmentApplication {

    public static void main(String[] args) {
        BouncyCastleFipsProviderLoader.load();
        SpringApplication.run(RemoteEnvironmentApplication.class, args);
    }

}
