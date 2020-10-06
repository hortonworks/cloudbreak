# Authorization
## General
Every controller class should be annotated with `@Controller`.

Every method of a controller should be annotated with one (or more if necessary) of the mentioned annotations below.

## Account level authorization
- you can annotate your API endpoint with `@CheckPermissionByAccount(action = ResourceAction.WRITE)` and permission check will be done only based on tenant, resource will not be taken into consideration in this case ([DefaultPermissionChecker](src/main/java/com/sequenceiq/authorization/service/DefaultPermissionChecker.java))

## Resource based authorization
Resource based authorization in Cloudbreak services mean that most of the API endpoints are or should be authorized based on a resource.
Every user who would like to do something regarding a resource should get an ResourceRole in UMS for that resource.

### How can I add resource based authorization for my new API?

1. Add `@Controller` annotation to the Controller class
2. Add the related UMS right to the UMS code
3. Add the related action to the `AuthorizationResourceAction` enum list
4. For the new enum value, please define
   - the right (used in UMS)
   - the resourceType, which should be a value from `AuthorizationResourceType` which controls, what logic should be called during authorization check to find out the CRN of the resource
5. Fill in the necessary information about the action in [legacyRights.json](src/main/resources/legacyRights.json)
   - add new entry to the map
   - key should be the same value used for the desired right as in UMS code
   - value should be the right (defined in UMS) used in the legacy authorization (where everything were checked on account level with read or write rights)
6. Annotate your API method in Controller class with desired annotation, detailed explanation below.
7. If necessary, implement logics needed to find out resource CRNs, detailed explanation below.

### How can I add resource based authorization for my new API endpoint?

When you are planning an API endpoint you should specify one of these parameter for that endpoint.
- resource CRN
- resource name
- resource CRN list
- resource name list
- resource object

If it is possible, please use CRN of a resource instead of name, since generally CRN is used in the whole platform to identify a resource and also during authorization it requires additional database query to find out the CRN based on the name, because we need CRN to authorize a call in UMS.

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
- note: by default framework check only 1st level of request object's hierarchy, if you need to authorize some field on deeper level in the request object class' hierarchy, you need to add `deepSearchNeeded = true` to `@CheckPermissionByResourceObject` method annotation and also you need to annotate every parent field in the hierarchy with `@ResourceObjectFieldHolder`, please check [DistroxV1Request](../core-api/src/main/java/com/sequenceiq/distrox/api/v1/distrox/model/DistroXV1Request.java) for example

### Special case: list API endpoints (not used currently)

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

### I have some default resources and I want to exclude them from the auth authorization process. How can I do that?

You need to implement the [DefaultResourceChecker](src/main/java/com/sequenceiq/authorization/service/DefaultResourceChecker.java) interface where you can mark different resources as defaults so that the framework won't go to UMS.

### Internal calls

Some APIs are used internal only between services, in this case we are using internal actor to call the given API. To restrict a method for internal actors only, you can annotate it `@InternalOnly`.

#### But what about account id?

We are using account id in service layer heavily, but internal actor CRN doesn't have that information, so there are specific requirements for APIs which can be called with internal actor:

Your method in the Controller class should have at least one parameter:
- which is some kind of resource CRN -> annotate the parameter with `@TenantAwareParam`, which will extract the account id from CRN parameter and use that in lower layers
- or it is an account id parameter -> annotate the parameter with `@AccountId` and we'll use that in lower layers. Please note that `@AccountId` is also a validation annotation, thus you need to place it on the API interface method's parameter.
- or it is an initator user crn parameter -> annotate the initiator user CRN parameter with `@InitiatorUserCrn` and we'll use that in lower layers to find out account id

### APIs without authorization

Some APIs are used to generate service informations, in this case you can call it without authorization. To do so, you can annotate it with `@DisabledCheckPermissions`.

## Class level annotations for special cases

There are some controller class which used to give service informations (info page, swagger, client version, etc.) and also some APIs used internally only.

If every method of a controller class used internally only, you only need to annotate the class with `@InternalOnly` and every method will be restricted for internal actor.

**Be aware: if your API is internal only, then you have to make sure that every method (which is internal only) can be called with proper parameter and annotated correctly as described in `Internal calls` chapter.**

Same applies for API's methods which can called without authorization, in that case you need to annotate the class only with `@DisabledCheckPermissions`.

**Please note that you have to make sure that only methods which don't provide any informations about and don't take actions on any resource should be annotated with `@DisabledCheckPermissions`**