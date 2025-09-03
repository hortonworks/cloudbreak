Someone who wants to use the secret module first of all they have to enable the configuration with `vault.config.enabled=true` and add the following properties to the application.yml or as run parameters for example:
```
secret:
  application: env/shared
  engine: "com.sequenceiq.cloudbreak.service.secret.vault.VaultKvV2Engine"

vault:
  addr: localhost
  port: 8200
  ssl.enabled: false
  kv.engine.v2.path: secret
  config.enabled: true
  auth:
    type: "token"
    kubernetes:
      service.account.token.path: /var/run/secrets/kubernetes.io/serviceaccount/token
      mount.path: "dps-dev"
      login.role: "cloudbreak.default"
```

Futhermore, you should create the proper `@Aspect` with the proper `@Pointcut`. For example:
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

Caching:
- secret caching is based on vault path and version of the given path. 
- this way we can ensure cache invalidation in case of secret rotation, thus we are eliminating possible bugs between different deployments.

Rotation: during secret rotation, we need to:
1. get current vault json stored in database (with path and version)
2. execute update in vault and retrieve new version for the given vault path
3. set new vault json for the given entity
4. update database with new vault json

For these steps we have implementation in VaultRotationExecutor using reflection.
For given secret in entity class we need to annotate:
- getter method with `@SecretGetter`
- setter method with `@SecretSetter`
- both of them we need to define a marker from `SecretMarker` enum to be able to match getter and setter and execute steps above for the given entity instance.
- corresponding JPA repository also should implement `VaultRotationAwareRepository` interface to help reflection logic to find the corresponding JPA repository class by entity