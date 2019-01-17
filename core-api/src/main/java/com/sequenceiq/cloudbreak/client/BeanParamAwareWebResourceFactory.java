package com.sequenceiq.cloudbreak.client;

import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Proxy;
import java.lang.reflect.Type;
import java.security.AccessController;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.ws.rs.BeanParam;
import javax.ws.rs.Consumes;
import javax.ws.rs.CookieParam;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.FormParam;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.HttpMethod;
import javax.ws.rs.MatrixParam;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Cookie;
import javax.ws.rs.core.Form;
import javax.ws.rs.core.GenericEntity;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.MultivaluedMap;

import org.glassfish.jersey.internal.util.ReflectionHelper;

/**
 * Factory for client-side representation of a resource.
 * See the <a href="package-summary.html">package overview</a>
 * for an example on how to use this class.
 *
 * @author Martin Matula
 */
public final class BeanParamAwareWebResourceFactory implements InvocationHandler {

    private static final String[] EMPTY = {};

    private static final MultivaluedMap<String, Object> EMPTY_HEADERS = new MultivaluedHashMap<>();

    private static final Form EMPTY_FORM = new Form();

    private static final List<Class> PARAM_ANNOTATION_CLASSES = Arrays.<Class>asList(PathParam.class, QueryParam.class,
            HeaderParam.class, CookieParam.class, MatrixParam.class, FormParam.class, BeanParam.class);

    private final WebTarget target;

    private final MultivaluedMap<String, Object> headers;

    private final List<Cookie> cookies;

    private final Form form;

    private BeanParamAwareWebResourceFactory(final WebTarget target, final MultivaluedMap<String, Object> headers,
            final List<Cookie> cookies, final Form form) {
        this.target = target;
        this.headers = headers;
        this.cookies = cookies;
        this.form = form;
    }

    /**
     * Creates a new client-side representation of a resource described by
     * the interface passed in the first argument.
     * <p/>
     * Calling this method has the same effect as calling {@code WebResourceFactory.newResource(resourceInterface, rootTarget,
     *false)}.
     *
     * @param <C> Type of the resource to be created.
     * @param resourceInterface Interface describing the resource to be created.
     * @param target WebTarget pointing to the resource or the parent of the resource.
     * @return Instance of a class implementing the resource interface that can
     * be used for making requests to the server.
     */
    public static <C> C newResource(final Class<C> resourceInterface, final WebTarget target) {
        return newResource(resourceInterface, target, false, EMPTY_HEADERS, Collections.<Cookie>emptyList(), EMPTY_FORM);
    }

    /**
     * Creates a new client-side representation of a resource described by
     * the interface passed in the first argument.
     *
     * @param <C> Type of the resource to be created.
     * @param resourceInterface Interface describing the resource to be created.
     * @param target WebTarget pointing to the resource or the parent of the resource.
     * @param ignoreResourcePath If set to true, ignores path annotation on the resource interface (this is used when creating
     * sub-resources)
     * @param headers Header params collected from parent resources (used when creating a sub-resource)
     * @param cookies Cookie params collected from parent resources (used when creating a sub-resource)
     * @param form Form params collected from parent resources (used when creating a sub-resource)
     * @return Instance of a class implementing the resource interface that can
     * be used for making requests to the server.
     */
    @SuppressWarnings("unchecked")
    public static <C> C newResource(final Class<C> resourceInterface,
            final WebTarget target,
            final boolean ignoreResourcePath,
            final MultivaluedMap<String, Object> headers,
            final List<Cookie> cookies,
            final Form form) {

        return (C) Proxy.newProxyInstance(AccessController.doPrivileged(ReflectionHelper.getClassLoaderPA(resourceInterface)),
                new Class[] {resourceInterface},
                new BeanParamAwareWebResourceFactory(ignoreResourcePath ? target : addPathFromAnnotation(resourceInterface, target),
                        headers, cookies, form));
    }

    @Override
    @SuppressWarnings("unchecked")
    public Object invoke(final Object proxy, final Method method, final Object[] args) throws Throwable {
        if (args == null && method.getName().equals("toString")) {
            return toString();
        }

        // get the interface describing the resource
        final Class<?> proxyIfc = proxy.getClass().getInterfaces()[0];

        // response type
        final Class<?> responseType = method.getReturnType();

        // determine method name
        String httpMethod = getHttpMethod(method);

        // create a new UriBuilder appending the @Path attached to the method
        WebTarget newTarget = addPathFromAnnotation(method, target);
        checkTarget(responseType, httpMethod, newTarget);

        // process method params (build maps of (Path|Form|Cookie|Matrix|Header..)Params
        // and extract entity type
        final MultivaluedHashMap<String, Object> headers = new MultivaluedHashMap<String, Object>(this.headers);
        final LinkedList<Cookie> cookies = new LinkedList<>(this.cookies);
        final Form form = new Form();
        form.asMap().putAll(this.form.asMap());
        final Annotation[][] paramAnns = method.getParameterAnnotations();
        Object entity = null;
        Type entityType = null;
        for (int i = 0; i < paramAnns.length; i++) {
            final Map<Class, Annotation> anns = getAnnotationsMap(paramAnns[i]);
            Object value = args[i];
            if (!hasAnyParamAnnotation(anns)) {
                entityType = method.getGenericParameterTypes()[i];
                entity = value;
            } else {
                newTarget = setupParameter(method.getParameterTypes()[i], anns, headers, cookies,
                        form, newTarget, value);
            }
        }

        if (httpMethod == null) {
            // the method is a subresource locator
            return BeanParamAwareWebResourceFactory.newResource(responseType, newTarget, true, headers, cookies, form);
        }

        // accepted media types
        final String[] accepts = getAcceptedMediaTypes(method, proxyIfc);

        // determine content type
        String contentType = getContentType(method, proxyIfc, headers, entity);
        // this resets all headers so do this first
        // if @Produces is defined, propagate values into Accept header; empty array is NO-OP
        Invocation.Builder builder = newTarget.request()
                .headers(headers)
                .accept(accepts);

        for (final Cookie c : cookies) {
            builder = builder.cookie(c);
        }

        if (entity == null && !form.asMap().isEmpty()) {
            entity = form;
            contentType = MediaType.APPLICATION_FORM_URLENCODED;
        } else {
            contentType = getContentType(form, entity, contentType);
        }

        return getResponse(method, httpMethod, entity, entityType, contentType, builder);
    }

    private String getContentType(Form form, Object entity, String contentType) {
        if (contentType == null) {
            contentType = MediaType.APPLICATION_OCTET_STREAM;
        }
        if (!form.asMap().isEmpty()) {
            if (entity instanceof Form) {
                ((Form) entity).asMap().putAll(form.asMap());
            }
        }
        return contentType;
    }

    private Object getResponse(Method method, String httpMethod, Object entity, Type entityType, String contentType, Invocation.Builder builder) {
        final GenericType responseGenericType = new GenericType(method.getGenericReturnType());
        if (entity != null) {
            if (entityType instanceof ParameterizedType) {
                entity = new GenericEntity(entity, entityType);
            }
            return builder.method(httpMethod, Entity.entity(entity, contentType), responseGenericType);
        } else {
            return builder.method(httpMethod, responseGenericType);
        }
    }

    private String[] getAcceptedMediaTypes(Method method, Class<?> proxyIfc) {
        Produces produces = method.getAnnotation(Produces.class);
        if (produces == null) {
            produces = proxyIfc.getAnnotation(Produces.class);
        }
        return (produces == null) ? EMPTY : produces.value();
    }

    private String getHttpMethod(Method method) {
        String httpMethod = getHttpMethodName(method);
        if (httpMethod == null) {
            for (final Annotation ann : method.getAnnotations()) {
                httpMethod = getHttpMethodName(ann.annotationType());
                if (httpMethod != null) {
                    return httpMethod;
                }
            }
        }
        return httpMethod;
    }

    private void checkTarget(Class<?> responseType, String httpMethod, WebTarget newTarget) {
        if (httpMethod == null) {
            if (newTarget == target) {
                // no path annotation on the method -> fail
                throw new UnsupportedOperationException("Not a resource method.");
            } else if (!responseType.isInterface()) {
                // the method is a subresource locator, but returns class,
                // not interface - can't help here
                throw new UnsupportedOperationException("Return type not an interface");
            }
        }
    }

    private String getContentType(Method method, Class<?> proxyIfc, MultivaluedHashMap<String, Object> headers, Object entity) {
        String contentType = null;
        if (entity != null) {
            final List<Object> contentTypeEntries = headers.get(HttpHeaders.CONTENT_TYPE);
            if ((contentTypeEntries != null) && (!contentTypeEntries.isEmpty())) {
                contentType = contentTypeEntries.get(0).toString();
            } else {
                Consumes consumes = method.getAnnotation(Consumes.class);
                if (consumes == null) {
                    consumes = proxyIfc.getAnnotation(Consumes.class);
                }
                if (consumes != null && consumes.value().length > 0) {
                    contentType = consumes.value()[0];
                }
            }
        }
        return contentType;
    }

    private boolean hasAnyParamAnnotation(final Map<Class, Annotation> anns) {
        for (final Class paramAnnotationClass : PARAM_ANNOTATION_CLASSES) {
            if (anns.containsKey(paramAnnotationClass)) {
                return true;
            }
        }
        return false;
    }

    private Object[] convert(final Collection value) {
        return value.toArray();
    }

    private static WebTarget addPathFromAnnotation(final AnnotatedElement ae, WebTarget target) {
        final Path p = ae.getAnnotation(Path.class);
        if (p != null) {
            target = target.path(p.value());
        }
        return target;
    }

    @Override
    public String toString() {
        return target.toString();
    }

    private static String getHttpMethodName(final AnnotatedElement ae) {
        final HttpMethod a = ae.getAnnotation(HttpMethod.class);
        return a == null ? null : a.value();
    }

    private WebTarget setupParameter(final Class<?> paramType, final Map<Class, Annotation> anns, final MultivaluedHashMap<String, Object> headers,
            final LinkedList<Cookie> cookies, final Form form, WebTarget newTarget, Object value)
            throws IllegalAccessException, InvocationTargetException, IntrospectionException {
        if (value == null && anns.get(DefaultValue.class) != null) {
            value = ((DefaultValue) anns.get(DefaultValue.class)).value();
        }

        if (value != null) {
            newTarget = handleParams(paramType, anns, headers, cookies, form, newTarget, value);
        }
        return newTarget;
    }

    private WebTarget handleParams(Class<?> paramType, Map<Class, Annotation> anns, MultivaluedHashMap<String, Object> headers, LinkedList<Cookie> cookies,
            Form form, WebTarget newTarget, Object value) throws IllegalAccessException, InvocationTargetException, IntrospectionException {
        if (anns.get(PathParam.class) != null) {
            newTarget = newTarget.resolveTemplate(((PathParam) anns.get(PathParam.class)).value(), value);
        } else if (anns.get(QueryParam.class) != null) {
            newTarget = handleQueryParam(anns, newTarget, value);
        } else if (anns.get(HeaderParam.class) != null) {
            handleHeaderParam(anns, headers, value);
        } else if (anns.get(CookieParam.class) != null) {
            handleCookieParam(anns, cookies, value);
        } else if (anns.get(MatrixParam.class) != null) {
            newTarget = handleMatrixParam(anns, newTarget, value);
        } else if (anns.get(FormParam.class) != null) {
            handleFormParam(anns, form, value);
        } else if (anns.get(BeanParam.class) != null) {
            newTarget = extractParamsFromBeanParamClass(paramType, headers, cookies, form, newTarget, value);
        }
        return newTarget;
    }

    private void handleFormParam(Map<Class, Annotation> anns, Form form, Object value) {
        if (value instanceof Collection) {
            for (final Object v : (Collection) value) {
                form.param(((FormParam) anns.get(FormParam.class)).value(), v.toString());
            }
        } else {
            form.param(((FormParam) anns.get(FormParam.class)).value(), value.toString());
        }
    }

    private WebTarget handleMatrixParam(Map<Class, Annotation> anns, WebTarget newTarget, Object value) {
        if (value instanceof Collection) {
            newTarget = newTarget.matrixParam(((MatrixParam) anns.get(MatrixParam.class)).value(), convert((Collection) value));
        } else {
            newTarget = newTarget.matrixParam(((MatrixParam) anns.get(MatrixParam.class)).value(), value);
        }
        return newTarget;
    }

    private void handleCookieParam(Map<Class, Annotation> anns, LinkedList<Cookie> cookies, Object value) {
        final String name = ((CookieParam) anns.get(CookieParam.class)).value();
        Cookie c;
        if (value instanceof Collection) {
            for (final Object v : (Collection) value) {
                if (!(v instanceof Cookie)) {
                    c = new Cookie(name, v.toString());
                } else {
                    c = (Cookie) v;
                    if (!name.equals(((Cookie) v).getName())) {
                        // is this the right thing to do? or should I fail? or ignore the difference?
                        c = new Cookie(name, c.getValue(), c.getPath(), c.getDomain(), c.getVersion());
                    }
                }
                cookies.add(c);
            }
        } else {
            if (!(value instanceof Cookie)) {
                cookies.add(new Cookie(name, value.toString()));
            } else {
                c = (Cookie) value;
                if (!name.equals(((Cookie) value).getName())) {
                    // is this the right thing to do? or should I fail? or ignore the difference?
                    cookies.add(new Cookie(name, c.getValue(), c.getPath(), c.getDomain(), c.getVersion()));
                }
            }
        }
    }

    private void handleHeaderParam(Map<Class, Annotation> anns, MultivaluedHashMap<String, Object> headers, Object value) {
        if (value instanceof Collection) {
            headers.addAll(((HeaderParam) anns.get(HeaderParam.class)).value(), convert((Collection) value));
        } else {
            headers.addAll(((HeaderParam) anns.get(HeaderParam.class)).value(), value);
        }
    }

    private WebTarget handleQueryParam(Map<Class, Annotation> anns, WebTarget newTarget, Object value) {
        if (value instanceof Collection) {
            newTarget = newTarget.queryParam(((QueryParam) anns.get(QueryParam.class)).value(), convert((Collection) value));
        } else {
            newTarget = newTarget.queryParam(((QueryParam) anns.get(QueryParam.class)).value(), value);
        }
        return newTarget;
    }

    private WebTarget extractParamsFromBeanParamClass(final Class<?> beanParamType, final MultivaluedHashMap<String, Object> headers,
            final LinkedList<Cookie> cookies, final Form form, WebTarget newTarget, final Object bean)
            throws IllegalAccessException, InvocationTargetException, IntrospectionException {
        Field[] fields = AccessController.doPrivileged(ReflectionHelper.getAllFieldsPA(beanParamType));
        for (Field field : fields) {
            final Map<Class, Annotation> anns = getAnnotationsMap(field.getAnnotations());

            if (hasAnyParamAnnotation(anns)) {
                if (!field.isAccessible()) {
                    field.setAccessible(true);
                }
                newTarget = setupParameter(field.getType(), anns, headers, cookies, form, newTarget, field.get(bean));
            }
        }

        PropertyDescriptor[] propertyDescriptors = Introspector.getBeanInfo(beanParamType, Introspector.USE_ALL_BEANINFO).getPropertyDescriptors();
        for (PropertyDescriptor propertyDesc : propertyDescriptors) {
            Method beanSetterMethod = propertyDesc.getWriteMethod();
            if (beanSetterMethod != null) {
                final Map<Class, Annotation> anns = getAnnotationsMap(beanSetterMethod.getAnnotations());

                if (hasAnyParamAnnotation(anns)) {
                    Method beanGetterMethod = propertyDesc.getReadMethod();
                    if (!beanGetterMethod.isAccessible()) {
                        beanGetterMethod.setAccessible(true);
                    }
                    newTarget = setupParameter(beanGetterMethod.getReturnType(), anns, headers, cookies, form, newTarget, beanGetterMethod.invoke(bean));
                }
            }
        }

        return newTarget;
    }

    private Map<Class, Annotation> getAnnotationsMap(Annotation[] annotations) {
        final Map<Class, Annotation> anns = new HashMap<>();
        for (final Annotation ann : annotations) {
            anns.put(ann.annotationType(), ann);
        }
        return anns;
    }
}
