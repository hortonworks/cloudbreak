# Authorization
## General
Every controller class should be annotated with `@Controller`.

Every controller should be annotated with `@AuthorizationResource` (or `DisabledCheckPermissions` if no need for authorization on that API).

Every method of a controller should be annotated with one of the mentioned annotations below.

## Account level authorization
- you can annotate your API endpoint with `@CheckPermissionByAccount(action = ResourceAction.WRITE)` and permission check will be done only based on tenant, resource will not be taken into consideration in this case ([DefaultPermissionChecker](src/main/java/com/sequenceiq/authorization/service/DefaultPermissionChecker.java))
- you can also annotate your API endpoint with `@DisableCheckPermissions`, but only if the method is used internally ([DisabledPermissionChecker](src/main/java/com/sequenceiq/authorization/service/DisabledPermissionChecker.java))

## Resource based authorization
Resource based authorization in Cloudbreak services mean that most of the API endpoints are or should be authorized based on a resource.
Every user who would like to do something regarding a resource should get an ResourceRole in UMS for that resource.

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
- resource CRN
- resource name
- resource CRN list
- resource name list
- resource object
- environment CRN
- environment name

### Follow these steps to add authorization for the method in controller class
#### Resource CRN
- add `@CheckPermissionByResourceCrn(action = ResourceAction.WRITE)` annotation to the method
- annotate the resource CRN method parameter with `@ResourceCrn`, the type of the parameter can be a String
#### Resource name
- add `@CheckPermissionByResourceName(action = ResourceAction.WRITE)` annotation to the method
- annotate the resource name method parameter with `@ResourceName`, the type of the parameter can be a String
- implement a `@Service` which is a subclass of [ResourceBasedCrnProvider](src/main/java/com/sequenceiq/authorization/service/ResourceBasedCrnProvider.java) and override `getResourceCrnByResourceName` method
#### Resource CRN list
- add `@CheckPermissionByResourceCrnList(action = ResourceAction.WRITE)` annotation to the method
- annotate the resource CRN list method parameter with `@ResourceCrnList`, the type of the parameter can be a String
#### Resource name list
- add `@CheckPermissionByResourceNameList(action = ResourceAction.WRITE)` annotation to the method
- annotate the resource name list method parameter with `@ResourceNameList`, the type of the parameter can be a String
- implement a `@Service` which is a subclass of [ResourceBasedCrnProvider](src/main/java/com/sequenceiq/authorization/service/ResourceBasedCrnProvider.java) and override `getResourceCrnListByResourceNameList` method
#### Resource object
- add `@CheckPermissionByResourceObject` annotation to the method
- annotate the resource object method parameter with `@ResourceObject`, the type of the parameter can be any Object
- annotate any field of object with `@ResourceObjectField`
#### environment name
- add `@CheckPermissionByEnvironmentName(action = ResourceAction.WRITE)` annotation to the method
- annotate the environment name method parameter with `@EnvironmentName`, the type of the parameter can be a String
- implement a `@Service` which is a subclass of [ResourceBasedCrnProvider](src/main/java/com/sequenceiq/authorization/service/ResourceBasedCrnProvider.java) and override `getResourceCrnByEnvironmentName` method
#### environment CRN
- add `@CheckPermissionByEnvironmentCrn(action = ResourceAction.WRITE)` annotation to the method
- annotate the environment CRN method parameter with `@EnvironmentCrn`, the type of the parameter can be a String
- implement a `@Service` which is a subclass of [ResourceBasedCrnProvider](src/main/java/com/sequenceiq/authorization/service/ResourceBasedCrnProvider.java) and override `getResourceCrnByEnvironmentCrn` method

#### Notes
- `action` parameter should defined explicitly everytime
- `action` is used to define the second part of the `right` during UMS permission check:
```
[resource]/write
```
### Special case: list API endpoints

In case of list API methods, we have to query the list of resources first, then filter it based on permissions.
To do this, you need:
- annotate controller method with `@FilterListBasedOnPermissions`
- if necessary, refactor your list method to return with one of these types: `List`, `Set` or `AuthorizationFilterableResponseCollection`
- element of list should implement `ResourceCrnAwareApiModel` to be able to get resource CRN of the given element
- within your webservice, implement a `@Service` which is a subclass of [ResourceBasedCrnProvider](src/main/java/com/sequenceiq/authorization/service/ResourceBasedCrnProvider.java) and override `getResourceCrnsInAccount` method

With these, authorization framework can filter result based on permissions. For details, please check: [ListPermissionChecker](src/main/java/com/sequenceiq/authorization/service/ListPermissionChecker.java)

### How authorization is happening under the hood

In every case, these informations are available:
- right: concatenation of 
  - the type of the resource from annotation on controller class 
  - the action from annotation on controller method
- userCrn (from auth header)

Everytime we are using these and resourceCrn to call UMS checkRight for authorization.

#### Resource CRN

[ResourceCrnPermissionChecker](src/main/java/com/sequenceiq/authorization/service/ResourceCrnPermissionChecker.java)

#### Resource Name

In this case we are calling the method `getResourceCrnByResourceName` of the current implementation of `ResourceBasedCrnProvider` to get resource CRN by resource name.

[ResourceNamePermissionChecker](src/main/java/com/sequenceiq/authorization/service/ResourceNamePermissionChecker.java)

#### Resource CRN list

[ResourceCrnListPermissionChecker](src/main/java/com/sequenceiq/authorization/service/ResourceCrnListPermissionChecker.java)

#### Resource Name list

In this case we are calling the method `getResourceCrnListByResourceNameList` of the current implementation of `ResourceBasedCrnProvider` to get resource CRN list by resource name list.

[ResourceNameListPermissionChecker](src/main/java/com/sequenceiq/authorization/service/ResourceNameListPermissionChecker.java)

#### Resource Object

In this case we are checking annotated fields of the objects and do permission check based on the parameters of the annotation.

[ResourceObjectPermissionChecker](src/main/java/com/sequenceiq/authorization/service/ResourceObjectPermissionChecker.java)

#### Environment Name

In this case we are calling the method `getResourceCrnByEnvironmentName` of the current implementation of `ResourceBasedCrnProvider` to get resource CRN by environment name.

[EnvironmentNamePermissionChecker](src/main/java/com/sequenceiq/authorization/service/EnvironmentNamePermissionChecker.java)

#### Environment CRN

In this case we are calling the method `getResourceCrnByEnvironmentCrn` of the current implementation of `ResourceBasedCrnProvider` to get resource CRN by environment CRN.

[EnvironmentCrnPermissionChecker](src/main/java/com/sequenceiq/authorization/service/EnvironmentCrnPermissionChecker.java)