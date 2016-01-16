package com.sequenceiq.cloudbreak.shell.configuration;

import org.codehaus.jackson.map.ObjectMapper;
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

import com.sequenceiq.cloudbreak.shell.model.CloudbreakClient;
import com.sequenceiq.cloudbreak.shell.transformer.ResponseTransformer;

/**
 * Spring bean definitions.
 */
@Configuration
public class ShellConfiguration {


    @Value("${cloudbreak.address:https://cloudbreak-api.sequenceiq.com}")
    private String cloudbreakAddress;

    @Value("${identity.address:https://identity.sequenceiq.com}")
    private String identityServerAddress;

    @Value("${sequenceiq.user:}")
    private String user;

    @Value("${sequenceiq.password:}")
    private String password;

    @Value("${cmdfile:}")
    private String cmdFile;

    @Bean
    CloudbreakClient cloudbreakClient() throws Exception {
        return new CloudbreakClient(cloudbreakAddress, identityServerAddress, user, password);
    }

    @Bean
    ResponseTransformer responseTransformer() {
        return new ResponseTransformer();
    }

    @Bean
    static PropertySourcesPlaceholderConfigurer propertyPlaceholderConfigurer() {
        return new PropertySourcesPlaceholderConfigurer();
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
    ObjectMapper getObjectMapper() {
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
