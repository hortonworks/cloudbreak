# authorization-common

Authorization framework used in cloudbreak services. The framework provides different ways to authorize resources on API level by annotating `Controller` classes. The authorization happens as an aspect of the API implementation.

__Table of content__

* [Usage](#usage)
    + [Authorization annotations on `Controller`](#authorization-annotations-on--controller-)
    + [How to make an API internally callable](#how-to-make-an-api-internally-callable)
    + [Rules for annotation usage](#rules-for-annotation-usage)
        - [DisableCheckPermissions](#disablecheckpermissions)
        - [InternalOnly](#internalonly)
        - [CustomPermissionCheck](#custompermissioncheck)
        - [CheckPermissionByAccount](#checkpermissionbyaccount)
        - [FilterListBasedOnPermissions](#filterlistbasedonpermissions)
        - [CheckPermissionByResourceCrn](#checkpermissionbyresourcecrn)
        - [CheckPermissionByResourceName](#checkpermissionbyresourcename)
        - [CheckPermissionByResourceCrnList](#checkpermissionbyresourcecrnlist)
        - [CheckPermissionByResourceNameList](#checkpermissionbyresourcenamelist)
        - [CheckPermissionByRequestProperty](#checkpermissionbyrequestproperty)
    + [List filtering](#list-filtering)
        - [Notes on performance](#notes-on-performance)
    + [Example](#example)
* [Introduction of new resources](#introduction-of-new-resources)
* [Introduction of new service](#introduction-of-new-service)
* [How does it work internally](#how-does-it-work-internally)
    + [Resource based authorization internals](#resource-based-authorization-internals)
    + [Example authorization](#example-authorization)
        - [In the new resource based authorization](#in-the-new-resource-based-authorization)

## Usage

Authorization can be introduced by annotating `Controller` classes and methods. The framework internally creates the necessary authorization method, calls UMS and finally evaluates the response. 

You can set up the following authorizations:

- authorize on __account__ level,
- authorize on __resource__ level,
- filter resource lists,
- allow only internal actors,
- do custom authorization in the service layer, 
- not authorize at all (be carefull here).

You can combine the above methods in certain ways.

### Authorization annotations on `Controller`

```java
@Controller
//ClassAnnotation
public class MyController {

    //MethodAnnotation
    public ReturnType method(/* MethodArgAnnotation */) { /*...*/ }

} 
```

Where the following grammar rules should apply.

```ebnf
ClassAnnotation
    = @DisableCheckPermissions
    | @InternalOnly;

MethodAnnotation 
    = @DisableCheckPermissions
    | @InternalOnly
    | @CustomPermissionCheck
    | [@CheckPermissionByAccount], [ResourceBasedAuthorization];

ResourceBasedAuthorization 
    = [@FilterListBasedOnPermissions]
    | [@CheckPermissionByResourceCrn], [@CheckPermissionByResourceName], [@CheckPermissionByResourceCrnList], [@CheckPermissionByResourceNameList], [@CheckPermissionByRequestProperty];

MethodArgAnnotation
    = [@ResourceCrn], [@ResourceName], [@ResourceCrnList], [@ResourceNameList], [@RequestObject], [@AccountId], [@InitiatorUserCrn];
```

### How to make an API internally callable

Lower layers often use the account id, but the internal crn doesn't contain that. There are three possible annotations to help the framework in figuring out the account id, and you must use one of them on one of the API method's argument.

You should use one of the following annotations on parameter of controller's method to make an API internally callable:

- `@ResourceCrn` - on resource crn parameter,
- `@RequestObject` - on request object parameter where the proper crn field of the object also annotated with `@ResourceCrn`,
- `@AccountId` - on an account id 
- `@InitiatorUserCrn` - on an initiator user crn parameter, and the service operations will be done in the name of the given user.

**Please note that authorization-related annotations should be applied to Controller classes, while validation-related annotations should be used on Endpoint classes.**

If you are sure, that your new API and the underlying service logic does not require account id to behave correctly, you can indicate this fact and annotate the controller method with `@AccountIdNotNeeded`.

Please be aware: if you are about to make an old, workspace related CB API internally callable, you should use `getRequestedWorkspaceId()` method from `CloudbreakRestRequestThreadLocalService` in Controller instead of the workspace id method parameter, because we are changing workspace id first (and that belongs to default workspace of `altus` tenant), then we are changing internal actor CRN.

### Rules for annotation usage

#### DisableCheckPermissions

Disables all authorization on class level or on method level.

**Please note that you have to make sure only methods which don't provide any information about and don't take actions on any resource should be annotated with `@DisabledCheckPermissions`**

#### InternalOnly

Makes API endpoints internal only on class level or on method level. No additional authorization will happen, but the API must be internally callable (see details above).

#### CustomPermissionCheck

Disables all authorization on API level, and the developer can provide custom authorization in the service layer.

---

For all below rules you should specify the `AuthorizationResourceAction` that the framework will use as the right for the authorization.

#### CheckPermissionByAccount

Checks whether the user has the specified right in the account.

#### FilterListBasedOnPermissions

To support list filtering you need to extend the `AbstractAuthorizationFiltering` and use that in your controller. See details in the list filtering section.

#### CheckPermissionByResourceCrn

The method's arguments should contain exactly one `@ResourceCrn` annotated `String` argument (valid Crn).

If hierarchical authorization is needed for resource type (like for datalake), then implementation of `AuthorizationEnvironmentCrnProvider` is needed.

#### CheckPermissionByResourceName

The method's arguments should conatin extactly one `@ResourceName` annotated `String` argument.

For the corresponding resource type the service should have an implementation of `AuthoriationResourceCrnProvider` interface.
If hierarchical authorization is needed for resource type (like for datalake), then implementation of `AuthorizationEnvironmentCrnProvider` is also needed.

#### CheckPermissionByResourceCrnList

The method's arguments should conatin extactly one `@ResourceCrnList` annotated `Collection<String>` argument (valid Crns).

If hierarchical authorization is needed for resource type (like for datalake), then implementation of `AuthorizationEnvironmentCrnListProvider` is needed.

#### CheckPermissionByResourceNameList

The method's arguments should conatin extactly one `@ResourceNameList` annotated `Collection<String>` argument.

For the corresponding resource type the service should have an implementation of `AuthoriationResourceCrnListProvider` interface.
If hierarchical authorization is needed for resource type (like for datalake), then implementation of `AuthorizationEnvironmentCrnListProvider` is also needed.

#### CheckPermissionByRequestProperty

This can be used mulitple times on any method. The method's arguments should conatin extactly one `@RequestObject` annotated `T` arbitrary argument.

Where:

1. `T` should contain a valid field path defined by `CheckPermissionByRequestProperty.path`,
2. the field can be `null` based on `CheckPermissionByRequestProperty.skipOnNull`,
3. the field will be handled as crn, name, crn list or name list based on `CheckPermissionByRequestProperty.type`

Additional interface implementation needed based on `CheckPermissionByRequestProperty.type`:
- crn: `AuthorizationEnvironmentCrnProvider` if hierarchical authorization is needed for resource type
- name: `AuthorizationResourceCrnProvider` is needed always and `AuthorizationEnvironmentCrnProvider` if hierarchical authorization is needed for resource type
- crn list: `AuthorizationEnvironmentCrnListProvider` if hierarchical authorization is needed for resource type
- name list: `AuthorizationResourceCrnListProvider` is needed always and `AuthorizationEnvironmentCrnListProvider` if hierarchical authorization is needed for resource type

### List filtering

Required entitlement: `PERSONAL_VIEW_CB_BY_RIGHT`.

To support list filtering you need to extend `AbstractAuthorizationFiltering` class and use it in your controller.

With that the authorization will follow the below logic:

1. `AbstractAuthorizationFiltering.getAllResources`: read a list of (id, resourceCrn, optional(parentResourceCrn)) items from the database,
2. call UMS and find out the ids where the user has the given right,
3. `AbstractAuthorizationFiltering.filterByIds`: read the necessary items from the database, `SELECT * FROM myresource WHERE id IN (:authorizedIds)`.

If the user is a machine user, or an internal user then we will return all resources from the account based on the `AbstractAuthorizationFiltering.getAll` implementation.

#### Notes on performance

The primary key based lookup `SELECT ... WHERE id IN (:ids)` will be fast even with many items.

:exclamation: The `AbstractAuthorizationFiltering.getAllResources` implementation should read the least amount of data from the database, potentially those that can be served from indices. Reading a list of `Stack` objects and converting them to `AuthorizationResource` objects with lambdas and streams is a poor solution and will hurt response time.

### Example

```java
@Controller
public class MyResourceController {

    @Inject
    private MyResourceFiltering myResourceFiltering;

    @CheckPermissionByAccount(action = AuthorizationResourceAction.CREATE_MY_RESOURCE)
    public void create() {}
    
    @CheckPermissionByResourceCrn(action = AuthorizationResourceAction.GET_RESOURCE)
    public MyResource getByCrn(@ResourceCrn String crn) {}

    @CheckPermissionByResourceName(action = AuthorizationResourceAction.GET_RESOURCE)
    public MyResource getByName(@ResourceName String name) {}

    @FilterListBasedOnPermissions
    public List<MyResource> list(@FilterParam(MyResourceFiltering.QUERY_PARAM) String queryParam) {
      return myResourceFiltering.filterResources(Crn.safeFromString(ThreadBasedUserCrnProvider.getUserCrn()),
        AuthorizationResourceAction.GET_RESOURCE, Map.of(MyResourceFiltering.QUERY_PARAM, queryParam));
    }

    @CheckPermissionByResourceCrnList(action = AuthorizationResourceAction.DELETE_RESOURCE)
    public void deleteMultipleByCrn(@ResourceCrnList Set<String> crns) {}

    @CheckPermissionByResourceNameList(action = AuthorizationResourceAction.DELETE_RESOURCE)
    public void deleteMultipleByName(@ResourceNameList Set<String> names) {}

    @CheckPermissionByAccount(action = AuthorizationResourceAction.CREATE_MY_RESOURCE)
    @CheckPermissionByResourceCrn(action = AuthorizationResourceAction.GET_RESOURCE)
    @CheckPermissionByRequestProperty(path = "name", type = AuthorizationVariableType.NAME, action = AuthorizationResourceAction.GET_RESOURCE)
    @CheckPermissionByRequestProperty(path = "subReq.crn", type = AuthorizationVariableType.CRN, action = AuthorizationResourceAction.GET_OTHER_RESOURCE, skipOnNull = true)
    public void doSomethingThatRequiresComplicatedAuthorization(@ResourceCrn String crn, @RequestObject MyRequest request) {}

    @InternalOnly
    public void doSomethingInternalStaffThatNormalUsersShouldNotBeAbleToDo(String crn) {}

    @CustomPermissionCheck
    public void doSomethingThatIsComplicatedAndWeCanAuthorizeItInTheServiceLayer(String crn) {}

    @DisableCheckPermissions
    public String notSensitiveAtAll() {}
}
```


```java
@Component
public class MyResourceFiltering extends AbstractAuthorizationFiltering<List<MyResource>> {
    
    public static final String QUERY_PARAM = "QUERY_PARAM";

    @Inject
    private MyResourceService myResourceService;

    @Override
    public List<AuthorizationResource> getAllResources(Map<String, Object> args) {
        String accountId = ThreadBasedUserCrnProvider.getAccountId();
        String queryParam = getQueryParam(args);
        return myResourceService.findAllAuthorizationResources(accountId, queryParam);
    }

    @Override
    public List<MyResource> filterByIds(List<Long> authorizedResourceIds, Map<String, Object> args) {
        return myResourceService.findAllById(authorizedResourceIds);
    }

    @Override
    public List<MyResource> getAll(Map<String, Object> args) {
        String accountId = ThreadBasedUserCrnProvider.getAccountId();
        String queryParam = getQueryParam(args);
        return myResourceService.findAllInAccount(accountId, queryParam);
    }

    private String getQueryParam(Map<String, Object> args) {
        return (String) args.get(QUERY_PARAM);
    }
}
```

## Introduction of new resources

You can support authorization on new resources, and you can specify the rights as well.

1. Define new resource types in `AuthorizationResourceType`,
2. define new rights by extending `AuthorizationResourceAction` enum with new (String right, AuthorizationResourceType type) values (the new right should be defined in UMS previously),
3. implement necessary interface as described in `Rules for annotation usage section`
4. if certain resources should be handled as default resources (for example default image catalog), and the authorization shouldn't call UMS at all, implement `DefaultResourceChecker` interface.

Useful interfaces:

`HierarchyAuthResourcePropertyProvider`: if hierarchical authorization needed, then this can be useful, since this will require all related methods' implementation

`CompositeAuthResourcePropertyProvider`: if there is no need for hierarchical authorization, this can be a good choice for the new resource (requires basic methods' implenentation like getting CRN by name, getting CRN list by name list)

## Introduction of new service

When you are creating a new module for a new microservice, you should:
* import `authorization-common` gradle module for authorization logics
* add enforce unit tests in order to ensure proper authorization implementations: EnforceAuthorizationAnnotationsTest

## How does it work internally

The framework based on the API annotations tries to gather the following information:

- user crn,
- resource crn (not needed in case of `@CheckPermissionByAccount`),
- necessary right.

Then it goes to UMS and does an acccount level or resource level authorization. A simplified logic can be seen below:

```
IF diabled THEN
    proceed with real API

ELSE IF internalCrn THEN
    proceed with real API

ELSE
    IF needAccountLevelAuthorization THEN
        do account level authorization

    IF hasAnnotation @FilterListBasedOnPermissions THEN
        proceed with real API then filter the result

    do resource based authorization

    proceed with real API
```

### Resource based authorization internals

The resource based authorization happens in three steps:

1. the framework collects all the details about how to authorize and what for based on a list of `AuthorizationFactory` implementations those that transforms the API annotations into `AuthorizationRule` implementations,
2. the framework creates the necessry right checks and sends it to UMS,
3. based on the response which is a list of boolen values the framework evaluates the original condition and succeeds or throws access denied exception with the failed part of the condition.

Basic implementations of the `AuthorizationRule` interface:

- `HasRight`,
- `HasRightOnAll`,
- `HasRightOnAny`,
- `AllMatch`,
- `AnyMatch`.

__Note:__ `AuthorizationFactory` implementations can throw access denied exception, this is usefull for default resources.

You can create additional authorization related annotations and implement `AuthorizationFactory` / `AuthorizationRule`-s to support them.

### Example authorization

```java
@Controller
public class AController {

    @CheckPermissionByAccount(action = AuthorizationResourceAction.A_ACTION)
    @CheckPermissionByResourceCrn(action = AuthorizationResourceAction.A_ACTION_ON_RESOURCE)
    public void aMethod(@ResourceCrn String crn) {
        // Do something here
    }
}

public enum AuthorizationResourceAction {
    A_ACTION("a/action", A),
    A_ACTION_ON_RESOURCE("a/action_on_resource", A);
}
```

Let's assume this resource has an environment.

#### In the new resource based authorization

1. Account authorization -> has this user "a/action" right in the acccount? - UMS says yes
2. Resource authorization -> 
    - has this user "a/action_on_resource" right on the resource, or it's environment? (`new HasRightOnAny(A_ACTION_ON_RESOURCE, List.of("crn:...:a:...", "crn:...:environment:..."))`)
    - UMS says `[false, true]`
    - Evaluation result: authorization succeeds since user has right on the environment.