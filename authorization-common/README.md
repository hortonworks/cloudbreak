# Authorization
## Resource based authorization
Resource based authorization in Cloudbreak services mean that most of the API endpoints are authorized based on environment.
Every user who would like to do something within an environment should get an ResourceRole in UMS for that environment.

[example:SdxController](../datalake/src/main/java/com/sequenceiq/datalake/controller/sdx/SdxController.java)

### How can I add resource based authorization for my new API?

Every time you are introducing a new API, you are creating a new `@Controller` class.
Controller class should be annotated with this:
``` 
@AuthorizationResource(type = AuthorizationResource.DATALAKE) 
```
This specifies the type of the API's resource and used for defining the first part of the `right` during UMS permission check:
```
datalake/[action]
```

### How can I add resource based authorization for my new API endpoint?

When you are planning an API endpoint you should specify one of these parameter for that endpoint.
- environment CRN (this is the best, you should add this if it is possible)
- environment name
- resource CRN
- resource name

### Follow these steps to add authorization for the method in controller class
#### Environment CRN
- add `@CheckPermissionByEnvironmentCrn(action = ResourceAction.WRITE)` annotation to the method
- annotate the environment CRN method parameter with `@EnvironmentCrn`, the type of the parameter can be a String or a subclass of [EnvironmentCrnAwareApiModel](../authorization-common-api/src/main/java/com/sequenceiq/authorization/api/EnvironmentCrnAwareApiModel.java)
#### Environment name
- add `@CheckPermissionByEnvironmentName(action = ResourceAction.WRITE)` annotation to the method
- annotate the environment name method parameter with `@EnvironmentName`, the type of the parameter can be a String or a subclass of [EnvironmentNameAwareApiModel](../authorization-common-api/src/main/java/com/sequenceiq/authorization/api/EnvironmentNameAwareApiModel.java)
- include environment api module and prepare your service to call Environment Service (details later)
#### Resource CRN
- add `@CheckPermissionByResourceCrn(action = ResourceAction.WRITE, relatedResourceClass = SomeResource.class)` annotation to the method
- annotate the resource CRN method parameter with `@ResourceCrn`, the type of the parameter can be a String
- implement a `@Service` which is a subclass of [ResourceBasedEnvironmentCrnProvider](src/main/java/com/sequenceiq/authorization/service/ResourceBasedEnvironmentCrnProvider.java) and override `getEnvironmentCrnByResourceCrn` method
#### Resource name
- add `@CheckPermissionByResourceName(action = ResourceAction.WRITE, relatedResourceClass = SomeResource.class)` annotation to the method
- annotate the resource name method parameter with `@ResourceName`, the type of the parameter can be a String
- implement a `@Service` which is a subclass of [ResourceBasedEnvironmentCrnProvider](src/main/java/com/sequenceiq/authorization/service/ResourceBasedEnvironmentCrnProvider.java) and override `getEnvironmentCrnByResourceName` method
 
#### Notes
- `action` parameter of the method annotations is `READ` by default, you do not need to specifiy it in case of `READ` action
- the generic type of your `ResourceBasedEnvironmentCrnProvider` subclass should be the same as the class defined in the `relatedResourceClass` parameter of method annotation (in case of `CheckPermissionByResourceName` and `CheckPermissionByResourceCrn`)
- `action` is used to define the second part of the `right` during UMS permission check:
```
[resource]/write
```

### How authorization is happening under the hood

In every case, these informations are available:
- right: concatenation of 
  - the type of the resource from annotation on controller class 
  - the action from annotation on controller method
- userCrn (from auth header)

Everytime we are using these and environmentCrn to call UMS checkRight for authorization.

#### Environment CRN

In this case there are no additional step, environmentCrn available from a method parameter.

[EnvironmentCrnPermissionChecker](src/main/java/com/sequenceiq/authorization/service/EnvironmentCrnPermissionChecker.java)

#### Environment Name

In this case we are trying to call environment service and get environment CRN by name.

[EnvironmentNamePermissionChecker](src/main/java/com/sequenceiq/authorization/service/EnvironmentNamePermissionChecker.java)

#### Resource CRN

In this case we are calling the method `getEnvironmentCrnByResourceCrn` of an implementation of the `ResourceBasedEnvironmentCrnProvider` based on the class provided in `CheckPermissionByResourceCrn` annotation to get environment CRN by resource CRN.

[ResourceCrnPermissionChecker](src/main/java/com/sequenceiq/authorization/service/ResourceCrnPermissionChecker.java)

#### Resource Name

In this case we are calling the method `getEnvironmentCrnByResourceName` of an implementation of the `ResourceBasedEnvironmentCrnProvider` based on the class provided in `CheckPermissionByResourceName` annotation to get environment CRN by resource name.

[ResourceNamePermissionChecker](src/main/java/com/sequenceiq/authorization/service/ResourceNamePermissionChecker.java)

### Open questions
- how should we authorize list endpoints? one possible solution could be to call UMS hasRights method to check every related environment and right in one call and sort the result of the list call by the result of the UMS call
- is it possible to replace every environmentName parameter to environmentCrn? if yes we can remove the permission check logic by environment name (which requires integration with environment service)

## Account level authorization
### Open questions
- how should we authorize endpoint without environment CRN? Which Role should be used for this in UMS?