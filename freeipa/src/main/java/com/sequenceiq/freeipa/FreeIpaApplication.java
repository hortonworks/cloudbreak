package com.sequenceiq.freeipa;

import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.Provider;
import java.security.Security;
import java.util.Arrays;
import java.util.stream.Collectors;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.actuate.autoconfigure.metrics.web.servlet.WebMvcMetricsAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

import com.sequenceiq.cloudbreak.util.FipsOpenSSLLoaderUtil;

import springfox.documentation.swagger2.annotations.EnableSwagger2;

@EnableSwagger2
@EnableJpaRepositories(basePackages = {"com.sequenceiq"})
@SpringBootApplication(scanBasePackages = "com.sequenceiq", exclude = WebMvcMetricsAutoConfiguration.class)
public class FreeIpaApplication {

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
        SpringApplication.run(FreeIpaApplication.class, args);
    }

}

