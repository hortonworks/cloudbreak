## Consul as configurtion source

Spring boot by default provides a mechanism to externalize configuration from source code.
Typical usage is:

```
@Value(“${cb.database.host}”)
private String dbHost;
```

You can customize this value several ways:

- add a java system property `java -jar -Dcb.database.host=mydb.host.com springboot.jar`
- use an environment variable `export CB_DATABASE_HOST=mydb.host.com`
- or same env var but just for one command: `CB_DATABASE_HOST=mydb.host.com java -jar springboot.jar`
- add `cb.database.host=mydb.host.com` to the **application.properties** or **application.yml**
- the application.properties (or yaml) can be:
  - inside the jar file
  - in the same dir as the jar
  - in the `config` dir starting from the dir containes the jar

Further docs about: [Externalized Configuration docs](https://docs.spring.io/spring-boot/docs/current/reference/html/boot-features-external-config.html)

## Cloud Config

Enter [Spring Cloud Config](http://projects.spring.io/spring-cloud/spring-cloud.html#_spring_cloud_config)
Its definition in one sentence:

> Spring Cloud Config provides server and client-side support for externalized configuration in a distributed system.

We don’t need the server side, but the client side id fully integrated into spring’s `Environment` and `PropertySource` abstraction.
There are several Cloud Config backend implementations, but as Cloudbrake environments always contains a Consul server,
the [Spring Cloud Consul ](https://github.com/spring-cloud/spring-cloud-consul) is a natural fit.

## Spring Cloud Consul

Spring Cloud Config implements features such as:
- Distributed configurtion
- Service Registry
- Distributed Events
- Distributed locking and sessions

But we use only the first one, which is based on Consul’s distributed key-value store.


You just need to put the `spring-cloud-starter-consul-config` and related jars to the runtime classpath, and you will be
able to change configurations.

By default all configurations are searched in the `/configuration/application` path of consul’s kv store.

```
@Value(“${cb.database.host}”)
private String dbHost;
```
This can be specified in consul kv as value under the key: `/configuration/application/cb/database/host`


## Consul Server host config

By default consul server is expected to be on `localhost:8500`.
To use a different server addres you can use the `spring.cloud.consul.host` property.

- add java system property: `-Dspring.cloud.consul.host=myconsul.com`
- use an env var: `export SPRING_CLOUD_CONSUL_HOST=myconsul.com`
- or change your `application.yml`
```
spring:
  cloud:
    consul:
      host: 192.168.10.53
```

## Dynamic configuration

`@Value` annotations works just fine, you can put all your configuration into consul’s kv store. Hiever its only read,
when spring builds the bean hierarchy. It means for every value change you have to restart your app (or somehow 
recontruct the spring context).

If you want to have dynamic config changes takingeffect without restart you have to use a different coding approach:

Create a class with related configurations annotated with `@ConfigurationProperties`
```
@ConfigurationProperties("cb")
@Component
public class MyProperties {

	private String customData="customData default";
	private boolean gateway=false;
    // getters and setters ...
}
```

Inject this into the bean needs configuration:
```
@RestController
public class HelloController {
    
    @Autowired
    private MyProperties myProps;

    public String Whatever() {
        if (myProps.isGateway()) {
            return “gateway case”
        } else {
            return myProps.getCustomData()
        }
    }
}

```

In that case you can use the `/configuration/application/cb/gateway` and `/configuration/application/cb/customData` consul kv keys.
