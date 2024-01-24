package com.sequenceiq.periscope;

import static com.sequenceiq.periscope.VersionedApplication.versionedApplication;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.actuate.autoconfigure.observation.web.servlet.WebMvcObservationAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.web.servlet.error.ErrorMvcAutoConfiguration;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;

import com.sequenceiq.cloudbreak.util.OpenSSLLoaderUtil;

@Configuration
@EnableAspectJAutoProxy(proxyTargetClass = true)
@SpringBootApplication(scanBasePackages = "com.sequenceiq", exclude = { ErrorMvcAutoConfiguration.class, WebMvcObservationAutoConfiguration.class })
public class PeriscopeApplication {

    public static void main(String[] args) {
        OpenSSLLoaderUtil.registerOpenSSLJniProvider();
        if (!versionedApplication().showVersionInfo(args)) {
            if (args.length == 0) {
                SpringApplication.run(PeriscopeApplication.class);
            } else {
                SpringApplication.run(PeriscopeApplication.class, args);
            }
        }
    }

}
