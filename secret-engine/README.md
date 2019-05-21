Someone who wants to use the secret module first of all they have to enable the configuration with `vault.config.enabled=true` and add the following properties to the application.yml or as run parameters for example:
```
secret:
  application: env/shared
  engine: "com.sequenceiq.cloudbreak.service.secret.vault.VaultKvV2Engine"

vault:
  addr: vault.service.consul
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