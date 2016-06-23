package com.sequenceiq.cloudbreak.conf

import com.sequenceiq.cloudbreak.api.CoreApi.API_ROOT_CONTEXT

import java.io.IOException

import javax.inject.Inject
import javax.inject.Named
import javax.servlet.FilterChain
import javax.servlet.ServletException
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

import org.jasypt.encryption.pbe.PBEStringCleanablePasswordEncryptor
import org.jasypt.encryption.pbe.StandardPBEStringEncryptor
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpMethod
import org.springframework.security.access.expression.method.DefaultMethodSecurityExpressionHandler
import org.springframework.security.access.expression.method.MethodSecurityExpressionHandler
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity
import org.springframework.security.config.annotation.method.configuration.GlobalMethodSecurityConfiguration
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.core.Authentication
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.oauth2.config.annotation.web.configuration.EnableResourceServer
import org.springframework.security.oauth2.config.annotation.web.configuration.ResourceServerConfigurerAdapter
import org.springframework.security.oauth2.config.annotation.web.configurers.ResourceServerSecurityConfigurer
import org.springframework.security.oauth2.provider.OAuth2Authentication
import org.springframework.security.oauth2.provider.token.RemoteTokenServices
import org.springframework.security.web.authentication.preauth.AbstractPreAuthenticatedProcessingFilter
import org.springframework.web.filter.OncePerRequestFilter

import com.sequenceiq.cloudbreak.domain.CbUser
import com.sequenceiq.cloudbreak.service.user.UserDetailsService
import com.sequenceiq.cloudbreak.service.user.UserFilterField

@Configuration
class SecurityConfig {

    @Inject
    private val userDetailsService: UserDetailsService? = null

    @Inject
    private val ownerBasedPermissionEvaluator: OwnerBasedPermissionEvaluator? = null

    @Bean internal fun expressionHandler(): MethodSecurityExpressionHandler {
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

        @Value("${cb.client.id}")
        private val clientId: String? = null

        @Value("${cb.client.secret}")
        private val clientSecret: String? = null

        @Inject
        @Named("identityServerUrl")
        private val identityServerUrl: String? = null

        @Inject
        private val userDetailsService: UserDetailsService? = null

        @Bean internal fun remoteTokenServices(): RemoteTokenServices {
            val rts = RemoteTokenServices()
            rts.setClientId(clientId)
            rts.setClientSecret(clientSecret)
            rts.setCheckTokenEndpointUrl(identityServerUrl!! + "/check_token")
            return rts
        }

        @Bean internal fun encryptor(): PBEStringCleanablePasswordEncryptor {
            val encryptor = StandardPBEStringEncryptor()
            encryptor.setPassword(clientSecret)
            return encryptor
        }

        @Throws(Exception::class)
        override fun configure(resources: ResourceServerSecurityConfigurer?) {
            resources!!.resourceId("cloudbreak")
            resources.tokenServices(remoteTokenServices())
        }

        @Throws(Exception::class)
        override fun configure(http: HttpSecurity?) {
            http!!.addFilterAfter(ScimAccountGroupReaderFilter(userDetailsService), AbstractPreAuthenticatedProcessingFilter::class.java).authorizeRequests().antMatchers(HttpMethod.GET, *BLUEPRINT_URL_PATTERNS).access("#oauth2.hasScope('cloudbreak.blueprints.read') or #oauth2.hasScope('cloudbreak.blueprints')").antMatchers(HttpMethod.GET, *STACK_TEMPLATE_URL_PATTERNS).access("#oauth2.hasScope('cloudbreak.stacks.read') or #oauth2.hasScope('cloudbreak.stacks')").antMatchers(HttpMethod.GET, *TEMPLATE_URL_PATTERNS).access("#oauth2.hasScope('cloudbreak.templates.read') or #oauth2.hasScope('cloudbreak.templates')").antMatchers(HttpMethod.GET, *CREDENTIAL_URL_PATTERNS).access("#oauth2.hasScope('cloudbreak.credentials.read') or #oauth2.hasScope('cloudbreak.credentials')").antMatchers(HttpMethod.GET, *RECIPE_URL_PATTERNS).access("#oauth2.hasScope('cloudbreak.recipes.read') or #oauth2.hasScope('cloudbreak.recipes')").antMatchers(HttpMethod.GET, *NETWORK_URL_PATTERNS).access("#oauth2.hasScope('cloudbreak.networks.read') or #oauth2.hasScope('cloudbreak.networks')").antMatchers(HttpMethod.GET, *SECURITYGROUP_URL_PATTERNS).access("#oauth2.hasScope('cloudbreak.securitygroups.read') or #oauth2.hasScope('cloudbreak.securitygroups')").antMatchers(HttpMethod.GET, *STACK_URL_PATTERNS).access("#oauth2.hasScope('cloudbreak.stacks.read') or #oauth2.hasScope('cloudbreak.stacks')" + " or #oauth2.hasScope('cloudbreak.autoscale')").antMatchers("/users/**").access("#oauth2.hasScope('openid')").antMatchers(*BLUEPRINT_URL_PATTERNS).access("#oauth2.hasScope('cloudbreak.blueprints')").antMatchers(*TEMPLATE_URL_PATTERNS).access("#oauth2.hasScope('cloudbreak.templates')").antMatchers(*CREDENTIAL_URL_PATTERNS).access("#oauth2.hasScope('cloudbreak.credentials')").antMatchers(*RECIPE_URL_PATTERNS).access("#oauth2.hasScope('cloudbreak.recipes')").antMatchers(*NETWORK_URL_PATTERNS).access("#oauth2.hasScope('cloudbreak.networks')").antMatchers(*SECURITYGROUP_URL_PATTERNS).access("#oauth2.hasScope('cloudbreak.securitygroups')").antMatchers(*STACK_URL_PATTERNS).access("#oauth2.hasScope('cloudbreak.stacks') or #oauth2.hasScope('cloudbreak.autoscale')").antMatchers(*STACK_TEMPLATE_URL_PATTERNS).access("#oauth2.hasScope('cloudbreak.stacks')").antMatchers(API_ROOT_CONTEXT + "/stacks/ambari", API_ROOT_CONTEXT + "/stacks/*/certificate").access("#oauth2.hasScope('cloudbreak.autoscale')").antMatchers(API_ROOT_CONTEXT + "/events").access("#oauth2.hasScope('cloudbreak.events')").antMatchers(API_ROOT_CONTEXT + "/usages/account/**").access("#oauth2.hasScope('cloudbreak.usages.account')").antMatchers(API_ROOT_CONTEXT + "/usages/user/**").access("#oauth2.hasScope('cloudbreak.usages.user')").antMatchers(API_ROOT_CONTEXT + "/usages/**").access("#oauth2.hasScope('cloudbreak.usages.global')").antMatchers(API_ROOT_CONTEXT + "/subscriptions").access("#oauth2.hasScope('cloudbreak.subscribe')").antMatchers(API_ROOT_CONTEXT + "/accountpreferences/**").access("#oauth2.hasScope('cloudbreak.templates') and #oauth2.hasScope('cloudbreak.stacks')").antMatchers(API_ROOT_CONTEXT + "/constraints/**").access("#oauth2.hasScope('cloudbreak.stacks') or #oauth2.hasScope('cloudbreak.autoscale')").antMatchers(API_ROOT_CONTEXT + "/topologies/**").access("#oauth2.hasScope('cloudbreak.stacks') or #oauth2.hasScope('cloudbreak.autoscale')").antMatchers(API_ROOT_CONTEXT + "/settings/**").access("#oauth2.hasScope('cloudbreak.stacks') or #oauth2.hasScope('cloudbreak.recipes')").antMatchers(API_ROOT_CONTEXT + "/sssd/**").access("#oauth2.hasScope('cloudbreak.stacks') or #oauth2.hasScope('cloudbreak.recipes')").antMatchers(API_ROOT_CONTEXT + "/util/**").access("#oauth2.hasScope('cloudbreak.stacks')").antMatchers(API_ROOT_CONTEXT + "/swagger.json").permitAll().antMatchers(API_ROOT_CONTEXT + "/connectors/**").permitAll().antMatchers(API_ROOT_CONTEXT + "/**").denyAll()

            http.csrf().disable()

            http.headers().contentTypeOptions()
        }

        companion object {
            private val BLUEPRINT_URL_PATTERNS = arrayOf(API_ROOT_CONTEXT + "/blueprints/**")
            private val TEMPLATE_URL_PATTERNS = arrayOf(API_ROOT_CONTEXT + "/templates/**")
            private val CREDENTIAL_URL_PATTERNS = arrayOf(API_ROOT_CONTEXT + "/credentials/**")
            private val RECIPE_URL_PATTERNS = arrayOf(API_ROOT_CONTEXT + "/recipes/**")
            private val NETWORK_URL_PATTERNS = arrayOf(API_ROOT_CONTEXT + "/networks/**")
            private val SECURITYGROUP_URL_PATTERNS = arrayOf(API_ROOT_CONTEXT + "/securitygroups/**")
            private val STACK_URL_PATTERNS = arrayOf(API_ROOT_CONTEXT + "/stacks/**")
            private val STACK_TEMPLATE_URL_PATTERNS = arrayOf(API_ROOT_CONTEXT + "/clustertemplates/**")
        }
    }

    private class ScimAccountGroupReaderFilter internal constructor(private val userDetailsService: UserDetailsService) : OncePerRequestFilter() {

        @SuppressWarnings("unchecked")
        @Throws(ServletException::class, IOException::class)
        override fun doFilterInternal(request: HttpServletRequest, response: HttpServletResponse, filterChain: FilterChain) {
            val authentication = SecurityContextHolder.getContext().authentication
            if (authentication != null) {
                val oauth = authentication as OAuth2Authentication?
                if (oauth.getUserAuthentication() != null) {
                    val username = authentication.principal as String
                    val user = userDetailsService.getDetails(username, UserFilterField.USERNAME)
                    request.setAttribute("user", user)
                }
            }
            filterChain.doFilter(request, response)
        }
    }

    companion object {

        private val LOGGER = LoggerFactory.getLogger(SecurityConfig::class.java)
    }

}
