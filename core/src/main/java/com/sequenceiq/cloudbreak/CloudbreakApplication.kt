package com.sequenceiq.cloudbreak

import com.sequenceiq.cloudbreak.VersionedApplication.versionedApplication

import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.EnableAutoConfiguration
import org.springframework.context.ConfigurableApplicationContext
import org.springframework.context.annotation.ComponentScan
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity

import com.sequenceiq.cloudbreak.core.init.CloudbreakCleanupAction

@EnableAutoConfiguration
@ComponentScan(basePackages = "com.sequenceiq.cloudbreak")
@EnableGlobalMethodSecurity(prePostEnabled = true)
object CloudbreakApplication {

    @JvmStatic fun main(args: Array<String>) {
        if (!versionedApplication().showVersionInfo(args)) {
            var context: ConfigurableApplicationContext? = null
            if (args.size == 0) {
                context = SpringApplication.run(CloudbreakApplication::class.java)
            } else {
                context = SpringApplication.run(CloudbreakApplication::class.java, *args)
            }
            resetStackAndClusterStates(context)
        }
    }

    private fun resetStackAndClusterStates(context: ConfigurableApplicationContext) {
        context.getBean<CloudbreakCleanupAction>(CloudbreakCleanupAction::class.java).resetStates()
    }

}