package com.sequenceiq.cloudbreak.aspect;

import java.util.Optional;

import javax.inject.Inject;

import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.common.model.user.IdentityUser;
import com.sequenceiq.cloudbreak.common.service.user.UserFilterField;
import com.sequenceiq.cloudbreak.domain.organization.User;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.service.user.CachedUserDetailsService;
import com.sequenceiq.cloudbreak.service.user.UserService;

@Component
@Aspect
public class UserInStackSetterAspects {

    @Inject
    private CachedUserDetailsService cachedUserDetailsService;

    @Inject
    private UserService userService;

    @Pointcut("execution(public * com.sequenceiq.cloudbreak.repository.StackRepository+.*(..))")
    public void interceptStackMethod() {
    }

    @AfterReturning(pointcut = "com.sequenceiq.cloudbreak.aspect.UserInStackSetterAspects.interceptStackMethod()", returning = "result")
    public void setUserInStack(Object result) {
        Stack stack = getStackByResult(result);
        if (stack != null && stack.getCreator() == null) {
            IdentityUser identityUser = cachedUserDetailsService.getDetails(stack.getOwner(),
                    UserFilterField.USERID);
            User user = userService.getOrCreate(identityUser);
            stack.setCreator(user);
        }
    }

    private Stack getStackByResult(Object result) {
        if (result instanceof Optional<?> && ((Optional<?>) result).isPresent()
                && ((Optional<?>) result).get() instanceof Stack) {
            return (Stack) ((Optional<?>) result).get();
        }
        if (result instanceof Iterable<?> && ((Iterable<?>) result).iterator().hasNext()) {
            Object object = ((Iterable<?>) result).iterator().next();
            return object instanceof Stack ? (Stack) object : null;
        }
        if (result instanceof Stack) {
            return (Stack) result;
        }
        return null;
    }

}
