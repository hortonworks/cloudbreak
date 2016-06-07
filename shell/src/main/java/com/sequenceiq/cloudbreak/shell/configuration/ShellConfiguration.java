package com.sequenceiq.cloudbreak.shell.configuration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;
import org.springframework.scheduling.concurrent.ThreadPoolExecutorFactoryBean;
import org.springframework.shell.CommandLine;
import org.springframework.shell.SimpleShellCommandLineOptions;
import org.springframework.shell.commands.ExitCommands;
import org.springframework.shell.commands.HelpCommands;
import org.springframework.shell.commands.ScriptCommands;
import org.springframework.shell.commands.VersionCommands;
import org.springframework.shell.core.CommandMarker;
import org.springframework.shell.core.JLineShellComponent;
import org.springframework.shell.plugin.HistoryFileNameProvider;
import org.springframework.shell.plugin.support.DefaultHistoryFileNameProvider;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sequenceiq.cloudbreak.client.CloudbreakClient;
import com.sequenceiq.cloudbreak.client.CloudbreakClient.CloudbreakClientBuilder;
import com.sequenceiq.cloudbreak.client.SSLConnectionException;
import com.sequenceiq.cloudbreak.shell.transformer.ResponseTransformer;

/**
 * Spring bean definitions.
 */
@Configuration
public class ShellConfiguration {

    private static final String CLIENT_ID = "cloudbreak_shell";

    @Value("${cloudbreak.address:https://cloudbreak-api.sequenceiq.com}")
    private String cloudbreakAddress;

    @Value("${identity.address:https://identity.sequenceiq.com}")
    private String identityServerAddress;

    @Value("${sequenceiq.user:}")
    private String user;

    @Value("${sequenceiq.password:}")
    private String password;

    @Value("${rest.debug:false}")
    private boolean restDebug;

    @Value("${cert.validation:true}")
    private boolean certificateValidation;

    @Value("${cmdfile:}")
    private String cmdFile;

    @Value("${server.contextPath:/cb}")
    private String cbRootContextPath;

    @Bean
    static PropertySourcesPlaceholderConfigurer propertyPlaceholderConfigurer() {
        return new PropertySourcesPlaceholderConfigurer();
    }

    @Bean
    public CloudbreakClient cloudbreakClient() {
        try {
            return new CloudbreakClientBuilder(cloudbreakAddress + cbRootContextPath, identityServerAddress, CLIENT_ID).withCredential(user, password).
                    withDebug(restDebug).withCertificateValidation(certificateValidation).build();
        } catch (SSLConnectionException e) {
            System.out.println(String.format("%s Try to start the shell with --cert.validation=false parameter.", e.getMessage()));
            System.exit(1);
        }
        return null;
    }

    @Bean
    ResponseTransformer responseTransformer() {
        return new ResponseTransformer();
    }

    @Bean
    HistoryFileNameProvider defaultHistoryFileNameProvider() {
        return new DefaultHistoryFileNameProvider();
    }

    @Bean(name = "shell")
    JLineShellComponent shell() {
        return new JLineShellComponent();
    }

    @Bean
    CommandLine commandLine() throws Exception {
        String[] args = cmdFile.length() > 0 ? new String[]{"--cmdfile", cmdFile} : new String[0];
        return SimpleShellCommandLineOptions.parseCommandLine(args);
    }

    @Bean
    ThreadPoolExecutorFactoryBean getThreadPoolExecutorFactoryBean() {
        return new ThreadPoolExecutorFactoryBean();
    }

    @Bean
    ObjectMapper objectMapper() {
        return new ObjectMapper();
    }

    @Bean
    CommandMarker exitCommand() {
        return new ExitCommands();
    }

    @Bean
    CommandMarker versionCommands() {
        return new VersionCommands();
    }

    @Bean
    CommandMarker helpCommands() {
        return new HelpCommands();
    }

    @Bean
    CommandMarker scriptCommands() {
        return new ScriptCommands();
    }

}
