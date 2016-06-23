package com.sequenceiq.periscope.config

import java.io.IOException
import java.util.concurrent.Executor

import javax.inject.Inject
import javax.inject.Named
import javax.ws.rs.client.Client

import org.quartz.simpl.SimpleJobFactory
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler
import org.springframework.aop.interceptor.SimpleAsyncUncaughtExceptionHandler
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.scheduling.annotation.AsyncConfigurer
import org.springframework.scheduling.annotation.EnableAsync
import org.springframework.scheduling.annotation.EnableScheduling
import org.springframework.scheduling.concurrent.ThreadPoolExecutorFactoryBean
import org.springframework.scheduling.quartz.SchedulerFactoryBean
import org.springframework.ui.freemarker.FreeMarkerConfigurationFactoryBean

import com.sequenceiq.cloudbreak.client.ConfigKey
import com.sequenceiq.cloudbreak.client.IdentityClient
import com.sequenceiq.cloudbreak.client.RestClientUtil

import freemarker.template.TemplateException

@Configuration
@EnableAsync
@EnableScheduling
class AppConfig : AsyncConfigurer {

    @Value("${periscope.threadpool.core.size:50}")
    private val corePoolSize: Int = 0
    @Value("${periscope.threadpool.max.size:500}")
    private val maxPoolSize: Int = 0
    @Value("${periscope.threadpool.queue.size:1000}")
    private val queueCapacity: Int = 0
    @Value("${periscope.client.id}")
    private val clientId: String? = null
    @Inject
    @Named("identityServerUrl")
    private val identityServerUrl: String? = null
    @Value("${rest.debug:false}")
    private val restDebug: Boolean = false
    @Value("${cert.validation:true}")
    private val certificateValidation: Boolean = false

    val threadPoolExecutorFactoryBean: ThreadPoolExecutorFactoryBean
        @Bean
        get() {
            val executorFactoryBean = ThreadPoolExecutorFactoryBean()
            executorFactoryBean.setCorePoolSize(corePoolSize)
            executorFactoryBean.setMaxPoolSize(maxPoolSize)
            executorFactoryBean.setQueueCapacity(queueCapacity)
            return executorFactoryBean
        }

    @Bean
    fun schedulerFactoryBean(): SchedulerFactoryBean {
        val scheduler = SchedulerFactoryBean()
        scheduler.setTaskExecutor(asyncExecutor)
        scheduler.isAutoStartup = true
        scheduler.setJobFactory(SimpleJobFactory())
        return scheduler
    }

    @Bean
    fun identityClient(): IdentityClient {
        return IdentityClient(identityServerUrl, clientId, ConfigKey(certificateValidation, restDebug))
    }

    @Bean
    fun restClient(): Client {
        return RestClientUtil.get(ConfigKey(certificateValidation, restDebug))
    }

    @Bean
    @Throws(IOException::class, TemplateException::class)
    fun freemarkerConfiguration(): freemarker.template.Configuration {
        val factoryBean = FreeMarkerConfigurationFactoryBean()
        factoryBean.setPreferFileSystemAccess(false)
        factoryBean.setTemplateLoaderPath("classpath:/")
        factoryBean.afterPropertiesSet()
        return factoryBean.`object`
    }

    override fun getAsyncExecutor(): Executor? {
        try {
            return threadPoolExecutorFactoryBean.`object`
        } catch (e: Exception) {
            LOGGER.error("Error creating task executor.", e)
        }

        return null
    }

    override fun getAsyncUncaughtExceptionHandler(): AsyncUncaughtExceptionHandler {
        return SimpleAsyncUncaughtExceptionHandler()
    }

    companion object {

        private val LOGGER = LoggerFactory.getLogger(AppConfig::class.java)
    }
}