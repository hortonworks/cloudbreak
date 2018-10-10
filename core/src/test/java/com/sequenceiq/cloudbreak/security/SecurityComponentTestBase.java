package com.sequenceiq.cloudbreak.security;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.HashSet;

import javax.inject.Inject;

import org.junit.Before;
import org.junit.runner.RunWith;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.context.annotation.FilterType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.provider.OAuth2Authentication;
import org.springframework.security.oauth2.provider.OAuth2Request;
import org.springframework.security.oauth2.provider.token.ResourceServerTokenServices;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;

import com.sequenceiq.cloudbreak.aspect.HasPermissionAspects;
import com.sequenceiq.cloudbreak.aspect.HasPermissionService;
import com.sequenceiq.cloudbreak.common.model.user.CloudbreakUser;
import com.sequenceiq.cloudbreak.common.service.user.UserFilterField;
import com.sequenceiq.cloudbreak.conf.SecurityConfig;
import com.sequenceiq.cloudbreak.service.AuthorizationService;
import com.sequenceiq.cloudbreak.service.CrudRepositoryLookupService;
import com.sequenceiq.cloudbreak.service.security.OwnerBasedPermissionEvaluator;
import com.sequenceiq.cloudbreak.service.security.ScimAccountGroupReaderFilter;
import com.sequenceiq.cloudbreak.service.user.CachedUserDetailsService;

@RunWith(SpringRunner.class)
@TestPropertySource(properties = {
        "profile=dev"
})
public abstract class SecurityComponentTestBase {

    public static final String ACCOUNT_A = "accountA";

    public static final String ACCOUNT_B = "accountB";

    public static final String USER_A_ID = "userA";

    public static final String USER_B_ID = "userBId";

    public static final String PERMISSION_READ = "READ";

    public static final String PERMISSION_WRITE = "WRITE";

    @Inject
    private AuthenticationManager authenticationManager;

    @Inject
    private ResourceServerTokenServices resourceServerTokenServices;

    @Inject
    private OAuth2Request oAuth2Request;

    @Inject
    private CachedUserDetailsService cachedUserDetailsService;

    @Before
    public void before() {
        setupSpringSecurityForFakeLoggedInUser();
    }

    private void setupSpringSecurityForFakeLoggedInUser() {
        OAuth2Authentication auth = mock(OAuth2Authentication.class);
        when(auth.getUserAuthentication()).thenReturn(auth);
        when(auth.getPrincipal()).thenReturn("");
        when(auth.getOAuth2Request()).thenReturn(oAuth2Request);

        SecurityContext ctx = SecurityContextHolder.createEmptyContext();
        SecurityContextHolder.setContext(ctx);
        ctx.setAuthentication(auth);
        when(authenticationManager.authenticate(any())).thenReturn(auth);
        when(resourceServerTokenServices.loadAuthentication(anyString())).thenReturn(auth);
    }

    protected void setupLoggedInUser(CloudbreakUser loggedInUser) {
        when(cachedUserDetailsService.getDetails(anyString(), any(UserFilterField.class))).thenReturn(loggedInUser);
    }

    protected CloudbreakUser getOwner(String... scopes) {
        addScopes(scopes);
        return new CloudbreakUser(USER_A_ID, "", ACCOUNT_A);
    }

    private void addScopes(String[] scopes) {
        when(oAuth2Request.getScope()).thenReturn(new HashSet<>(Arrays.asList(scopes)));
    }

    @Configuration
    @EnableAspectJAutoProxy(proxyTargetClass = true)
    @ComponentScan(basePackages =
            {"com.sequenceiq.cloudbreak"},
            useDefaultFilters = false,
            includeFilters = {
                    @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, value = {
                            AuthorizationService.class,
                            OwnerBasedPermissionEvaluator.class,
                            ScimAccountGroupReaderFilter.class,
                            SecurityConfig.class
                    })
            })

    public static class SecurityComponentTestBaseConfig {

        @MockBean
        private ResourceServerTokenServices resourceServerTokenServices;

        @MockBean
        private CachedUserDetailsService cachedUserDetailsService;

        @MockBean
        private CrudRepositoryLookupService repositoryLookupService;

        @MockBean
        private AuthenticationManager authenticationManager;

        @MockBean
        private OAuth2Request oAuth2Request;

        @Bean
        public HasPermissionAspects hasPermissionAspects() {
            return new HasPermissionAspectForMockitoTest();
        }

        @Bean
        public HasPermissionService hasPermissionService() {
            return new HasPermissionServiceForMockitoTest();
        }
    }
}
