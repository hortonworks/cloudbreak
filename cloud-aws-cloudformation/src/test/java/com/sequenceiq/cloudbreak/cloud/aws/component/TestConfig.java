package com.sequenceiq.cloudbreak.cloud.aws.component;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.concurrent.Callable;

import org.mockito.stubbing.Answer;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Profile;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.test.context.TestPropertySource;
import org.springframework.ui.freemarker.FreeMarkerConfigurationFactoryBean;

import com.sequenceiq.cloudbreak.cloud.aws.common.AwsPlatformParameters;
import com.sequenceiq.cloudbreak.cloud.aws.common.AwsPlatformResources;
import com.sequenceiq.cloudbreak.cloud.aws.common.AwsTagValidator;
import com.sequenceiq.cloudbreak.cloud.notification.ResourceNotifier;
import com.sequenceiq.cloudbreak.cloud.scheduler.SyncPollingScheduler;
import com.sequenceiq.cloudbreak.cloud.template.GroupResourceBuilder;
import com.sequenceiq.cloudbreak.cloud.template.NetworkResourceBuilder;
import com.sequenceiq.cloudbreak.cloud.transform.CloudResourceHelper;
import com.sequenceiq.cloudbreak.service.Retry;
import com.sequenceiq.cloudbreak.tag.CostTagging;
import com.sequenceiq.cloudbreak.util.FreeMarkerTemplateUtils;

import freemarker.template.TemplateException;

@TestConfiguration
@ComponentScan(basePackages = {
        "com.sequenceiq.cloudbreak.cloud.aws",
        "com.sequenceiq.cloudbreak.cloud.template",
        "com.sequenceiq.cloudbreak.polling"})
@Import(ComponentTestUtil.class)
@TestPropertySource(properties = {
        "cb.max.aws.resource.name.length=200",
        "cb.aws.hostkey.verify=true",
        "cb.aws.spotinstances.enabled=true",
        "cb.aws.credential.cache.ttl=1"
})
@Profile("component")
public class TestConfig {

    @MockBean(name = "DefaultRetryService")
    private Retry defaultRetryService;

    @MockBean
    private CostTagging defaultCostTaggingService;

    @MockBean
    private NetworkResourceBuilder<?> networkResourceBuilder;

    @MockBean
    private GroupResourceBuilder<?> groupResourceBuilder;

    @MockBean
    private ResourceNotifier resourceNotifier;

    @MockBean
    private AwsPlatformResources awsPlatformResources;

    @MockBean
    private AwsPlatformParameters awsPlatformParameters;

    @MockBean
    private AwsTagValidator awsTagValidator;

    @MockBean
    private FreeMarkerTemplateUtils freeMarkerTemplateUtils;

    static Answer<?> getAnswer() {
        return invocation -> {
            Object[] args = invocation.getArguments();
            Callable<?> task = (Callable<?>) args[0];
            return task.call();
        };
    }

    @Bean
    public freemarker.template.Configuration configurationProvider() throws IOException, TemplateException {
        FreeMarkerConfigurationFactoryBean factoryBean = new FreeMarkerConfigurationFactoryBean();
        factoryBean.setPreferFileSystemAccess(false);
        factoryBean.setTemplateLoaderPath("classpath:/");
        factoryBean.afterPropertiesSet();
        return factoryBean.getObject();
    }

    @Bean
    public AsyncTaskExecutor intermediateBuilderExecutor() {
        return new AsyncTaskExecutorTestImpl();
    }

    @Bean
    public SyncPollingScheduler<?> syncPollingScheduler() throws Exception {
        SyncPollingScheduler<?> syncPollingScheduler = mock(SyncPollingScheduler.class);
        when(syncPollingScheduler.schedule(any())).thenAnswer(
                getAnswer()
        );

        when(syncPollingScheduler.schedule(any(), anyInt(), anyInt(), anyInt())).thenAnswer(
                getAnswer()
        );

        return syncPollingScheduler;
    }

    @Bean
    public CloudResourceHelper cloudResourceHelper() {
        return new CloudResourceHelper();
    }
}
