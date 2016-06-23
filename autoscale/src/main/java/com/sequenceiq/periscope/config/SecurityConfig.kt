package com.sequenceiq.periscope.config

import com.sequenceiq.periscope.api.AutoscaleApi.API_ROOT_CONTEXT

import java.io.IOException

import javax.inject.Inject
import javax.servlet.FilterChain
import javax.servlet.ServletException
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.access.expression.method.DefaultMethodSecurityExpressionHandler
import org.springframework.security.access.expression.method.MethodSecurityExpressionHandler
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity
import org.springframework.security.config.annotation.method.configuration.GlobalMethodSecurityConfiguration
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.oauth2.config.annotation.web.configuration.EnableResourceServer
import org.springframework.security.oauth2.config.annotation.web.configuration.ResourceServerConfigurerAdapter
import org.springframework.security.oauth2.config.annotation.web.configurers.ResourceServerSecurityConfigurer
import org.springframework.security.oauth2.provider.token.RemoteTokenServices
import org.springframework.security.web.authentication.preauth.AbstractPreAuthenticatedProcessingFilter
import org.springframework.web.filter.OncePerRequestFilter

import com.sequenceiq.periscope.domain.PeriscopeUser
import com.sequenceiq.periscope.service.security.OwnerBasedPermissionEvaluator
import com.sequenceiq.periscope.service.security.UserDetailsService
import com.sequenceiq.periscope.service.security.UserFilterField

@Configuration
class SecurityConfig {

    @Inject
    private val userDetailsService: UserDetailsService? = null

    @Inject
    private val ownerBasedPermissionEvaluator: OwnerBasedPermissionEvaluator? = null

    @Bean
    internal fun expressionHandler(): MethodSecurityExpressionHandler {
        val expressionHandler = DefaultMethodSecurityExpressionHandler()
        ownerBasedPermissionEvaluator!!.setUserDetailsService(userDetailsService)
        expressionHandler.setPermissionEvaluator(ownerBasedPermissionEvaluator)
        return expressionHandler
    }

    @Configuration
    @EnableGlobalMethodSecurity(prePostEnabled = true)
    protected class MethodSecurityConfig : GlobalMethodSecurityConfiguration() {

        @Inject
        private val expressionHandler: MethodSecurityExpressionHandler? = null

        override fun createExpressionHandler(): MethodSecurityExpressionHandler {
            return expressionHandler
        }
    }

    @Configuration
    @EnableResourceServer
    protected class ResourceServerConfiguration : ResourceServerConfigurerAdapter() {

        @Value("${periscope.client.id}")
        private val clientId: String? = null

        @Value("${periscope.client.secret}")
        private val clientSecret: String? = null

        @Autowired
        @Qualifier("identityServerUrl")
        private val identityServerUrl: String? = null

        @Autowired
        private val userDetailsService: UserDetailsService? = null

        @Bean
        internal fun remoteTokenServices(): RemoteTokenServices {
            val rts = RemoteTokenServices()
            rts.setClientId(clientId)
            rts.setClientSecret(clientSecret)
            rts.setCheckTokenEndpointUrl(identityServerUrl!! + "/check_token")
            return rts
        }

        @Throws(Exception::class)
        override fun configure(resources: ResourceServerSecurityConfigurer?) {
            resources!!.resourceId("periscope")
            resources.tokenServices(remoteTokenServices())
        }

        @Throws(Exception::class)
        override fun configure(http: HttpSecurity?) {
            http!!.addFilterAfter(ScimAccountGroupReaderFilter(userDetailsService), AbstractPreAuthenticatedProcessingFilter::class.java).authorizeRequests().antMatchers(API_ROOT_CONTEXT + "/clusters/**").access("#oauth2.hasScope('cloudbreak.stacks') and #oauth2.hasScope('periscope.cluster')").antMatchers(API_ROOT_CONTEXT + "/swagger.json").permitAll().antMatchers(API_ROOT_CONTEXT + "/**").denyAll().and().csrf().disable().headers().contentTypeOptions()
        }
    }

    private class ScimAccountGroupReaderFilter internal constructor(private val userDetailsService: UserDetailsService) : OncePerRequestFilter() {

        @SuppressWarnings("unchecked")
        @Throws(ServletException::class, IOException::class)
        override fun doFilterInternal(request: HttpServletRequest, response: HttpServletResponse, filterChain: FilterChain) {
            if (SecurityContextHolder.getContext().authentication != null) {
                val username = SecurityContextHolder.getContext().authentication.principal as String
                val user = userDetailsService.getDetails(username, UserFilterField.USERNAME)
                request.setAttribute("user", user)
            }
            filterChain.doFilter(request, response)
        }
    }

}
