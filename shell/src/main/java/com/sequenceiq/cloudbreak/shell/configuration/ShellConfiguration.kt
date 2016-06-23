package com.sequenceiq.cloudbreak.shell.configuration

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer
import org.springframework.scheduling.concurrent.ThreadPoolExecutorFactoryBean
import org.springframework.shell.CommandLine
import org.springframework.shell.SimpleShellCommandLineOptions
import org.springframework.shell.commands.ExitCommands
import org.springframework.shell.commands.HelpCommands
import org.springframework.shell.commands.ScriptCommands
import org.springframework.shell.commands.VersionCommands
import org.springframework.shell.core.CommandMarker
import org.springframework.shell.core.JLineShellComponent
import org.springframework.shell.plugin.HistoryFileNameProvider
import org.springframework.shell.plugin.support.DefaultHistoryFileNameProvider
import org.springframework.shell.support.util.StringUtils

import com.fasterxml.jackson.databind.ObjectMapper
import com.sequenceiq.cloudbreak.client.CloudbreakClient
import com.sequenceiq.cloudbreak.client.CloudbreakClient.CloudbreakClientBuilder
import com.sequenceiq.cloudbreak.client.SSLConnectionException
import com.sequenceiq.cloudbreak.shell.transformer.ResponseTransformer

/**
 * Spring bean definitions.
 */
@Configuration
class ShellConfiguration {

    @Value("${cloudbreak.address:}")
    private val cloudbreakAddress: String? = null

    @Value("${identity.address:}")
    private val identityServerAddress: String? = null

    @Value("${sequenceiq.user:}")
    private val user: String? = null

    @Value("${sequenceiq.password:}")
    private val password: String? = null

    @Value("${rest.debug:false}")
    private val restDebug: Boolean = false

    @Value("${cert.validation:true}")
    private val certificateValidation: Boolean = false

    @Value("${cmdfile:}")
    private val cmdFile: String? = null

    @Value("${server.contextPath:/cb}")
    private val cbRootContextPath: String? = null

    @Bean
    fun cloudbreakClient(): CloudbreakClient? {
        try {
            var identity: String = identityServerAddress
            if (StringUtils.isEmpty(identity)) {
                identity = cloudbreakAddress!! + IDENTITY_ROOT_CONTEXT
            }
            return CloudbreakClientBuilder(cloudbreakAddress!! + cbRootContextPath!!, identity, CLIENT_ID).withCredential(user, password).withDebug(restDebug).withCertificateValidation(certificateValidation).build()
        } catch (e: SSLConnectionException) {
            println(String.format("%s Try to start the shell with --cert.validation=false parameter.", e.message))
            System.exit(1)
        }

        return null
    }

    @Bean
    internal fun responseTransformer(): ResponseTransformer<Collection<Any>> {
        return ResponseTransformer()
    }

    @Bean
    internal fun defaultHistoryFileNameProvider(): HistoryFileNameProvider {
        return DefaultHistoryFileNameProvider()
    }

    @Bean(name = "shell")
    internal fun shell(): JLineShellComponent {
        return JLineShellComponent()
    }

    @Bean
    @Throws(Exception::class)
    internal fun commandLine(): CommandLine {
        val args = if (cmdFile!!.length > 0) arrayOf("--cmdfile", cmdFile) else arrayOfNulls<String>(0)
        return SimpleShellCommandLineOptions.parseCommandLine(args)
    }

    internal val threadPoolExecutorFactoryBean: ThreadPoolExecutorFactoryBean
        @Bean
        get() = ThreadPoolExecutorFactoryBean()

    @Bean
    internal fun objectMapper(): ObjectMapper {
        return ObjectMapper()
    }

    @Bean
    internal fun exitCommand(): CommandMarker {
        return ExitCommands()
    }

    @Bean
    internal fun versionCommands(): CommandMarker {
        return VersionCommands()
    }

    @Bean
    internal fun helpCommands(): CommandMarker {
        return HelpCommands()
    }

    @Bean
    internal fun scriptCommands(): CommandMarker {
        return ScriptCommands()
    }

    companion object {

        private val CLIENT_ID = "cloudbreak_shell"

        private val IDENTITY_ROOT_CONTEXT = "/identity"

        @Bean
        internal fun propertyPlaceholderConfigurer(): PropertySourcesPlaceholderConfigurer {
            return PropertySourcesPlaceholderConfigurer()
        }
    }

}
