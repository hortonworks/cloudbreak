# Authorization
## General
Every controller class should be annotated with `@Controller`.

Every controller should be annotated with `@AuthorizationResource` (or `DisabledCheckPermissions` if no need for authorization on that API).

Every method of a controller should be annotated with one (or more if necessary) of the mentioned annotations below.

## Account level authorization
- you can annotate your API endpoint with `@CheckPermissionByAccount(action = ResourceAction.WRITE)` and permission check will be done only based on tenant, resource will not be taken into consideration in this case ([DefaultPermissionChecker](src/main/java/com/sequenceiq/authorization/service/DefaultPermissionChecker.java))
- you can also annotate your API endpoint with `@DisableCheckPermissions`, but only if the method is used internally

## Resource based authorization
Resource based authorization in Cloudbreak services mean that most of the API endpoints are or should be authorized based on a resource.
Every user who would like to do something regarding a resource should get an ResourceRole in UMS for that resource.

### How can I add resource based authorization for my new API?

1. Add `@Controller` annotation to the Controller class
2. Add `@AuthorizationResource` to the Controller class
3. Add the related UMS right to the UMS code
4. Add the related action to the `AuthorizationResourceAction` enum list
5. Fill in the necessary information about the action in [actions.json](src/main/resources/actions.json)
   - key should be the new value of the enum
   - `right` should be the same value as in UMS code
   - `resourceType` should be a value from `AuthorizationResourceType` which controls, what logic should be called during authorization check to find out the CRN of the resource
   - `actionType` should be a value from `AuthorizationActionType`, which decides, if the action should happen on a resource or in an account
6. Annotate your API method in Controller class with desired annotation, detailed explanation below.
7. If necessary, implement logics needed to find out resource CRNs, detailed explanation below.

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
- add `@CheckPermissionByResourceCrn(action = [a value from AuthorizationResourceAction])` annotation to the method
- annotate the resource CRN method parameter with `@ResourceCrn`, the type of the parameter can be a String
#### Resource name
- add `@CheckPermissionByResourceName(action = [a value from AuthorizationResourceAction])` annotation to the method
- annotate the resource name method parameter with `@ResourceName`, the type of the parameter can be a String
- implement a `@Service` which is a subclass of [ResourceBasedCrnProvider](src/main/java/com/sequenceiq/authorization/service/ResourceBasedCrnProvider.java) and override `getResourceCrnByResourceName` method
#### Resource CRN list
- add `@CheckPermissionByResourceCrnList(action = [a value from AuthorizationResourceAction])` annotation to the method
- annotate the resource CRN list method parameter with `@ResourceCrnList`, the type of the parameter can be a String
#### Resource name list
- add `@CheckPermissionByResourceNameList(action = [a value from AuthorizationResourceAction])` annotation to the method
- annotate the resource name list method parameter with `@ResourceNameList`, the type of the parameter can be a String
- implement a `@Service` which is a subclass of [ResourceBasedCrnProvider](src/main/java/com/sequenceiq/authorization/service/ResourceBasedCrnProvider.java) and override `getResourceCrnListByResourceNameList` method
#### Resource object
- add `@CheckPermissionByResourceObject` annotation to the method
- annotate the resource object method parameter with `@ResourceObject`, the type of the parameter can be any Object
- annotate any field of object with `@ResourceObjectField`
#### environment name
- add `@CheckPermissionByEnvironmentName(action = [a value from AuthorizationResourceAction])` annotation to the method
- annotate the environment name method parameter with `@EnvironmentName`, the type of the parameter can be a String
- implement a `@Service` which is a subclass of [ResourceBasedCrnProvider](src/main/java/com/sequenceiq/authorization/service/ResourceBasedCrnProvider.java) and override `getResourceCrnByEnvironmentName` method
#### environment CRN
- add `@CheckPermissionByEnvironmentCrn(action = [a value from AuthorizationResourceAction])` annotation to the method
- annotate the environment CRN method parameter with `@EnvironmentCrn`, the type of the parameter can be a String
- implement a `@Service` which is a subclass of [ResourceBasedCrnProvider](src/main/java/com/sequenceiq/authorization/service/ResourceBasedCrnProvider.java) and override `getResourceCrnByEnvironmentCrn` method

#### Notes
- `action` parameter should defined explicitly every time
- enum value of `action` refers to the `right` in actions.json used for checkRight call to UMS, please check [AuthorizationResourceAction](../authorization-common-api/src/main/java/com/sequenceiq/authorization/resource/AuthorizationResourceAction.java) and [actions.json](src/main/resources/actions.json)

### Special case: list API endpoints

In case of list API methods, we have to query the list of resources first, then filter it based on permissions.
To do this, you need:
- annotate controller method with `@FilterListBasedOnPermissions`
- if necessary, refactor your list method to return with one of these types: `List`, `Set` or `AuthorizationFilterableResponseCollection`
- element of list should implement `ResourceCrnAwareApiModel` to be able to get resource CRN of the given element
- within your webservice, implement a `@Service` which is a subclass of [ResourceBasedCrnProvider](src/main/java/com/sequenceiq/authorization/service/ResourceBasedCrnProvider.java) and override `getResourceCrnsInAccount` method

With these, authorization framework can filter result based on permissions. For details, please check: [ListPermissionChecker](src/main/java/com/sequenceiq/authorization/service/ListPermissionChecker.java)

### How authorization is happening under the hood

We are calling UMS checkRight using these informations:
- CRN of user (which is present in ThreadLocal)
- UMS right (which is extracted based of the enum value of the method's annotation)
- CRN of resource (which is extracted from method parameter, based on method's annotation)

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

### I have some default resources and I want to exclude them from the auth authorization process. How can I do that?

You need to implement the [DefaultResourceChecker](src/main/java/com/sequenceiq/authorization/service/DefaultResourceChecker.java) interface where you can mark different resources as defaults so that the framework won't go to UMS.