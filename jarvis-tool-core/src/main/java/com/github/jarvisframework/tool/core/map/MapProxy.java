package com.github.jarvisframework.tool.core.map;

import com.github.jarvisframework.tool.core.convert.Convert;
import com.github.jarvisframework.tool.core.getter.OptNullBasicTypeFromObjectGetter;
import com.github.jarvisframework.tool.core.util.ArrayUtils;
import com.github.jarvisframework.tool.core.util.BooleanUtils;
import com.github.jarvisframework.tool.core.util.ClassLoaderUtils;
import com.github.jarvisframework.tool.core.util.StringUtils;

import java.io.Serializable;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Collection;
import java.util.Map;
import java.util.Set;

/**
 * <p>Map代理，提供各种getXXX方法，并提供默认值支持</p>
 *
 * @author Doug Wang
 * @since 1.0, 2020-07-30 10:10:20
 */
public class MapProxy implements Map<Object, Object>, OptNullBasicTypeFromObjectGetter<Object>, InvocationHandler, Serializable {
    private static final long serialVersionUID = 1L;

    @SuppressWarnings("rawtypes")
    Map map;

    /**
     * 创建代理Map<br>
     * 此类对Map做一次包装，提供各种getXXX方法
     *
     * @param map 被代理的Map
     * @return {@link MapProxy}
     */
    public static MapProxy create(Map<?, ?> map) {
        return (map instanceof MapProxy) ? (MapProxy) map : new MapProxy(map);
    }

    /**
     * 构造
     *
     * @param map 被代理的Map
     */
    public MapProxy(Map<?, ?> map) {
        this.map = map;
    }

    @Override
    public Object getObject(Object key, Object defaultValue) {
        final Object value = map.get(key);
        return null != value ? value : defaultValue;
    }

    @Override
    public int size() {
        return map.size();
    }

    @Override
    public boolean isEmpty() {
        return map.isEmpty();
    }

    @Override
    public boolean containsKey(Object key) {
        return map.containsKey(key);
    }

    @Override
    public boolean containsValue(Object value) {
        return map.containsValue(value);
    }

    @Override
    public Object get(Object key) {
        return map.get(key);
    }

    @SuppressWarnings("unchecked")
    @Override
    public Object put(Object key, Object value) {
        return map.put(key, value);
    }

    @Override
    public Object remove(Object key) {
        return map.remove(key);
    }

    @SuppressWarnings({"unchecked", "NullableProblems"})
    @Override
    public void putAll(Map<?, ?> m) {
        map.putAll(m);
    }

    @Override
    public void clear() {
        map.clear();
    }

    @SuppressWarnings({"unchecked", "NullableProblems"})
    @Override
    public Set<Object> keySet() {
        return map.keySet();
    }

    @SuppressWarnings({"unchecked", "NullableProblems"})
    @Override
    public Collection<Object> values() {
        return map.values();
    }

    @SuppressWarnings({"unchecked", "NullableProblems"})
    @Override
    public Set<Entry<Object, Object>> entrySet() {
        return map.entrySet();
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) {
        final Class<?>[] parameterTypes = method.getParameterTypes();
        if (ArrayUtils.isEmpty(parameterTypes)) {
            final Class<?> returnType = method.getReturnType();
            if (void.class != returnType) {
                // 匹配Getter
                final String methodName = method.getName();
                String fieldName = null;
                if (methodName.startsWith("get")) {
                    // 匹配getXXX
                    fieldName = StringUtils.removePreAndLowerFirst(methodName, 3);
                } else if (BooleanUtils.isBoolean(returnType) && methodName.startsWith("is")) {
                    // 匹配isXXX
                    fieldName = StringUtils.removePreAndLowerFirst(methodName, 2);
                } else if ("hashCode".equals(methodName)) {
                    return this.hashCode();
                } else if ("toString".equals(methodName)) {
                    return this.toString();
                }

                if (StringUtils.isNotBlank(fieldName)) {
                    if (false == this.containsKey(fieldName)) {
                        // 驼峰不存在转下划线尝试
                        fieldName = StringUtils.toUnderlineCase(fieldName);
                    }
                    return Convert.convert(method.getGenericReturnType(), this.get(fieldName));
                }
            }

        } else if (1 == parameterTypes.length) {
            // 匹配Setter
            final String methodName = method.getName();
            if (methodName.startsWith("set")) {
                final String fieldName = StringUtils.removePreAndLowerFirst(methodName, 3);
                if (StringUtils.isNotBlank(fieldName)) {
                    this.put(fieldName, args[0]);
                }
            } else if ("equals".equals(methodName)) {
                return this.equals(args[0]);
            }
        }

        throw new UnsupportedOperationException(method.toGenericString());
    }

    /**
     * 将Map代理为指定接口的动态代理对象
     *
     * @param <T>            代理的Bean类型
     * @param interfaceClass 接口
     * @return 代理对象
     * @since 4.5.2
     */
    @SuppressWarnings("unchecked")
    public <T> T toProxyBean(Class<T> interfaceClass) {
        return (T) Proxy.newProxyInstance(ClassLoaderUtils.getClassLoader(), new Class<?>[]{interfaceClass}, this);
    }
}
