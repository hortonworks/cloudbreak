# DefaultApi

All URIs are relative to *http://localhost*

Method | HTTP request | Description
------------- | ------------- | -------------
[**appV1ServicesGet**](DefaultApi.md#appV1ServicesGet) | **GET** /app/v1/services | (TBD) List of services running in the cluster.
[**appV1ServicesPost**](DefaultApi.md#appV1ServicesPost) | **POST** /app/v1/services | Create a service
[**appV1ServicesServiceNameComponentsComponentNamePut**](DefaultApi.md#appV1ServicesServiceNameComponentsComponentNamePut) | **PUT** /app/v1/services/{service_name}/components/{component_name} | Flex a component&#39;s number of instances.
[**appV1ServicesServiceNameDelete**](DefaultApi.md#appV1ServicesServiceNameDelete) | **DELETE** /app/v1/services/{service_name} | Destroy a service
[**appV1ServicesServiceNameGet**](DefaultApi.md#appV1ServicesServiceNameGet) | **GET** /app/v1/services/{service_name} | Get details of a service.
[**appV1ServicesServiceNamePut**](DefaultApi.md#appV1ServicesServiceNamePut) | **PUT** /app/v1/services/{service_name} | Update a service or upgrade the binary version of the components of a running service
[**appV1ServicesVersionGet**](DefaultApi.md#appV1ServicesVersionGet) | **GET** /app/v1/services/version | Get current version of the API server.


<a name="appV1ServicesGet"></a>
# **appV1ServicesGet**
> List&lt;Service&gt; appV1ServicesGet()

(TBD) List of services running in the cluster.

Get a list of all currently running services (response includes a minimal projection of the service info). For more details do a GET on a specific service name.

### Example
```java
// Import classes:
//import org.apache.cb.yarn.service.api.ApiException;
//import org.apache.cb.yarn.service.api.impl.DefaultApi;


DefaultApi apiInstance = new DefaultApi();
try {
    List<Service> result = apiInstance.appV1ServicesGet();
    System.out.println(result);
} catch (ApiException e) {
    System.err.println("Exception when calling DefaultApi#appV1ServicesGet");
    e.printStackTrace();
}
```

### Parameters
This endpoint does not need any parameter.

### Return type

[**List&lt;Service&gt;**](Service.md)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: application/json

<a name="appV1ServicesPost"></a>
# **appV1ServicesPost**
> appV1ServicesPost(service)

Create a service

Create a service. The request JSON is a service object with details required for creation. If the request is successful it returns 202 Accepted. A success of this API only confirms success in submission of the service creation request. There is no guarantee that the service will actually reach a RUNNING state. Resource availability and several other factors determines if the service will be deployed in the cluster. It is expected that clients would subsequently call the GET API to get details of the service and determine its state.

### Example
```java
// Import classes:
//import org.apache.cb.yarn.service.api.ApiException;
//import org.apache.cb.yarn.service.api.impl.DefaultApi;


DefaultApi apiInstance = new DefaultApi();
Service service = new Service(); // Service | Service request object
try {
    apiInstance.appV1ServicesPost(service);
} catch (ApiException e) {
    System.err.println("Exception when calling DefaultApi#appV1ServicesPost");
    e.printStackTrace();
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **service** | [**Service**](Service.md)| Service request object |

### Return type

null (empty response body)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: application/json

<a name="appV1ServicesServiceNameComponentsComponentNamePut"></a>
# **appV1ServicesServiceNameComponentsComponentNamePut**
> appV1ServicesServiceNameComponentsComponentNamePut(serviceName, componentName, component)

Flex a component&#39;s number of instances.

Set a component&#39;s desired number of instanes

### Example
```java
// Import classes:
//import org.apache.cb.yarn.service.api.ApiException;
//import org.apache.cb.yarn.service.api.impl.DefaultApi;


DefaultApi apiInstance = new DefaultApi();
String serviceName = "serviceName_example"; // String | Service name
String componentName = "componentName_example"; // String | Component name
Component component = new Component(); // Component | The definition of a component which contains the updated number of instances.
try {
    apiInstance.appV1ServicesServiceNameComponentsComponentNamePut(serviceName, componentName, component);
} catch (ApiException e) {
    System.err.println("Exception when calling DefaultApi#appV1ServicesServiceNameComponentsComponentNamePut");
    e.printStackTrace();
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **serviceName** | **String**| Service name |
 **componentName** | **String**| Component name |
 **component** | [**Component**](Component.md)| The definition of a component which contains the updated number of instances. |

### Return type

null (empty response body)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: application/json

<a name="appV1ServicesServiceNameDelete"></a>
# **appV1ServicesServiceNameDelete**
> appV1ServicesServiceNameDelete(serviceName)

Destroy a service

Destroy a service and release all resources. This API might have to return JSON data providing location of logs (TBD), etc.

### Example
```java
// Import classes:
//import org.apache.cb.yarn.service.api.ApiException;
//import org.apache.cb.yarn.service.api.impl.DefaultApi;


DefaultApi apiInstance = new DefaultApi();
String serviceName = "serviceName_example"; // String | Service name
try {
    apiInstance.appV1ServicesServiceNameDelete(serviceName);
} catch (ApiException e) {
    System.err.println("Exception when calling DefaultApi#appV1ServicesServiceNameDelete");
    e.printStackTrace();
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **serviceName** | **String**| Service name |

### Return type

null (empty response body)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: application/json

<a name="appV1ServicesServiceNameGet"></a>
# **appV1ServicesServiceNameGet**
> Service appV1ServicesServiceNameGet(serviceName)

Get details of a service.

Return the details (including containers) of a running service

### Example
```java
// Import classes:
//import org.apache.cb.yarn.service.api.ApiException;
//import org.apache.cb.yarn.service.api.impl.DefaultApi;


DefaultApi apiInstance = new DefaultApi();
String serviceName = "serviceName_example"; // String | Service name
try {
    Service result = apiInstance.appV1ServicesServiceNameGet(serviceName);
    System.out.println(result);
} catch (ApiException e) {
    System.err.println("Exception when calling DefaultApi#appV1ServicesServiceNameGet");
    e.printStackTrace();
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **serviceName** | **String**| Service name |

### Return type

[**Service**](Service.md)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: application/json

<a name="appV1ServicesServiceNamePut"></a>
# **appV1ServicesServiceNamePut**
> appV1ServicesServiceNamePut(serviceName, service)

Update a service or upgrade the binary version of the components of a running service

Update the runtime properties of a service. Currently the following operations are supported - update lifetime, stop/start a service. The PUT operation is also used to orchestrate an upgrade of the service containers to a newer version of their artifacts (TBD).

### Example
```java
// Import classes:
//import org.apache.cb.yarn.service.api.ApiException;
//import org.apache.cb.yarn.service.api.impl.DefaultApi;


DefaultApi apiInstance = new DefaultApi();
String serviceName = "serviceName_example"; // String | Service name
Service service = new Service(); // Service | The updated service definition. It can contain the updated lifetime of a service or the desired state (STOPPED/STARTED) of a service to initiate a start/stop operation against the specified service
try {
    apiInstance.appV1ServicesServiceNamePut(serviceName, service);
} catch (ApiException e) {
    System.err.println("Exception when calling DefaultApi#appV1ServicesServiceNamePut");
    e.printStackTrace();
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **serviceName** | **String**| Service name |
 **service** | [**Service**](Service.md)| The updated service definition. It can contain the updated lifetime of a service or the desired state (STOPPED/STARTED) of a service to initiate a start/stop operation against the specified service |

### Return type

null (empty response body)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: application/json

<a name="appV1ServicesVersionGet"></a>
# **appV1ServicesVersionGet**
> appV1ServicesVersionGet()

Get current version of the API server.

Get current version of the API server.

### Example
```java
// Import classes:
//import org.apache.cb.yarn.service.api.ApiException;
//import org.apache.cb.yarn.service.api.impl.DefaultApi;


DefaultApi apiInstance = new DefaultApi();
try {
    apiInstance.appV1ServicesVersionGet();
} catch (ApiException e) {
    System.err.println("Exception when calling DefaultApi#appV1ServicesVersionGet");
    e.printStackTrace();
}
```

### Parameters
This endpoint does not need any parameter.

### Return type

null (empty response body)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: application/json

