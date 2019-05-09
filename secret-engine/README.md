Someone who wants to use the secret module, they should create the proper `@Aspect` with the proper `@Pointcut`. For example:
```
@Component
@Aspect
public class SecretAspects {

    private static final Logger LOGGER = LoggerFactory.getLogger(SecretAspects.class);

    private SecretAspectService secretAspectService;
    
    @Inject
    public SecretAspects(SecretAspectService secretAspectService) {
        this.secretAspectService = secretAspectService;
    }

    @Pointcut("execution(public * com.sequenceiq.cloudbreak.repository..*.save(..)) ")
    public void onRepositorySave() {
    }

    @Around("onRepositorySave()")
    public Object proceedOnRepositorySave(ProceedingJoinPoint proceedingJoinPoint) {
        return secretAspectService.proceedSave(proceedingJoinPoint);
    }
}
```