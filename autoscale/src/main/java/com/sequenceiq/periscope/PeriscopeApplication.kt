package com.sequenceiq.periscope

import com.sequenceiq.periscope.VersionedApplication.versionedApplication

import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.EnableAutoConfiguration
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Configuration

@Configuration
@EnableAutoConfiguration
@ComponentScan(basePackages = "com.sequenceiq.periscope")
object PeriscopeApplication {

    @JvmStatic fun main(args: Array<String>) {
        if (!versionedApplication().showVersionInfo(args)) {
            if (args.size == 0) {
                SpringApplication.run(PeriscopeApplication::class.java)
            } else {
                SpringApplication.run(PeriscopeApplication::class.java, *args)
            }
        }
    }

}
