package com.github.jarvisframework.tool.core.bean;

import com.github.jarvisframework.tool.core.annotation.AnnotationUtils;
import com.github.jarvisframework.tool.core.annotation.PropIgnore;
import com.github.jarvisframework.tool.core.convert.Convert;
import com.github.jarvisframework.tool.core.lang.Assert;
import com.github.jarvisframework.tool.core.map.CaseInsensitiveMap;
import com.github.jarvisframework.tool.core.util.*;

import java.beans.Transient;
import java.io.Serializable;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Bean信息描述做为BeanInfo替代方案，此对象持有JavaBean中的setters和getters等相关信息描述<br>
 * 查找Getter和Setter方法时会：
 *
 * <pre>
 * 1. 忽略字段和方法名的大小写
 * 2. Getter查找getXXX、isXXX、getIsXXX
 * 3. Setter查找setXXX、setIsXXX
 * 4. Setter忽略参数值与字段值不匹配的情况，因此有多个参数类型的重载时，会调用首次匹配的
 * </pre>
 *
 * @author Doug Wang
 * @since 1.0, 2020-07-29 18:11:13
 */
public class BeanDesc implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * Bean类
     */
    private final Class<?> beanClass;
    /**
     * 属性Map
     */
    private final Map<String, PropDesc> propMap = new LinkedHashMap<>();

    /**
     * 构造
     *
     * @param beanClass Bean类
     */
    public BeanDesc(Class<?> beanClass) {
        Assert.notNull(beanClass);
        this.beanClass = beanClass;
        init();
    }

    /**
     * 获取Bean的全类名
     *
     * @return Bean的类名
     */
    public String getName() {
        return this.beanClass.getName();
    }

    /**
     * 获取Bean的简单类名
     *
     * @return Bean的类名
     */
    public String getSimpleName() {
        return this.beanClass.getSimpleName();
    }

    /**
     * 获取字段名-字段属性Map
     *
     * @param ignoreCase 是否忽略大小写，true为忽略，false不忽略
     * @return 字段名-字段属性Map
     */
    public Map<String, PropDesc> getPropMap(boolean ignoreCase) {
        return ignoreCase ? new CaseInsensitiveMap<>(1, this.propMap) : this.propMap;
    }

    /**
     * 获取字段属性列表
     *
     * @return {@link PropDesc} 列表
     */
    public Collection<PropDesc> getProps() {
        return this.propMap.values();
    }

    /**
     * 获取属性，如果不存在返回null
     *
     * @param fieldName 字段名
     * @return {@link PropDesc}
     */
    public PropDesc getProp(String fieldName) {
        return this.propMap.get(fieldName);
    }

    /**
     * 获得字段名对应的字段对象，如果不存在返回null
     *
     * @param fieldName 字段名
     * @return 字段值
     */
    public Field getField(String fieldName) {
        final PropDesc desc = this.propMap.get(fieldName);
        return null == desc ? null : desc.getField();
    }

    /**
     * 获取Getter方法，如果不存在返回null
     *
     * @param fieldName 字段名
     * @return Getter方法
     */
    public Method getGetter(String fieldName) {
        final PropDesc desc = this.propMap.get(fieldName);
        return null == desc ? null : desc.getGetter();
    }

    /**
     * 获取Setter方法，如果不存在返回null
     *
     * @param fieldName 字段名
     * @return Setter方法
     */
    public Method getSetter(String fieldName) {
        final PropDesc desc = this.propMap.get(fieldName);
        return null == desc ? null : desc.getSetter();
    }

    // ------------------------------------------------------------------------------------------------------ Private method start

    /**
     * 初始化<br>
     * 只有与属性关联的相关Getter和Setter方法才会被读取，无关的getXXX和setXXX都被忽略
     *
     * @return this
     */
    private BeanDesc init() {
        for (Field field : ReflectUtils.getFields(this.beanClass)) {
            if (false == ModifierUtils.isStatic(field)) {
                //只针对非static属性
                this.propMap.put(ReflectUtils.getFieldName(field), createProp(field));
            }
        }
        return this;
    }

    /**
     * 根据字段创建属性描述<br>
     * 查找Getter和Setter方法时会：
     *
     * <pre>
     * 1. 忽略字段和方法名的大小写
     * 2. Getter查找getXXX、isXXX、getIsXXX
     * 3. Setter查找setXXX、setIsXXX
     * 4. Setter忽略参数值与字段值不匹配的情况，因此有多个参数类型的重载时，会调用首次匹配的
     * </pre>
     *
     * @param field 字段
     * @return {@link PropDesc}
     * @since 4.0.2
     */
    private PropDesc createProp(Field field) {
        final String fieldName = field.getName();
        final Class<?> fieldType = field.getType();
        final boolean isBooeanField = BooleanUtils.isBoolean(fieldType);

        Method getter = null;
        Method setter = null;

        String methodName;
        Class<?>[] parameterTypes;
        for (Method method : ReflectUtils.getMethods(this.beanClass)) {
            parameterTypes = method.getParameterTypes();
            if (parameterTypes.length > 1) {
                // 多于1个参数说明非Getter或Setter
                continue;
            }

            methodName = method.getName();
            if (parameterTypes.length == 0) {
                // 无参数，可能为Getter方法
                if (isMatchGetter(methodName, fieldName, isBooeanField)) {
                    // 方法名与字段名匹配，则为Getter方法
                    getter = method;
                }
            } else if (isMatchSetter(methodName, fieldName, isBooeanField)) {
                // 只有一个参数的情况下方法名与字段名对应匹配，则为Setter方法
                setter = method;
            }
            if (null != getter && null != setter) {
                // 如果Getter和Setter方法都找到了，不再继续寻找
                break;
            }
        }
        return new PropDesc(field, getter, setter);
    }

    /**
     * 方法是否为Getter方法<br>
     * 匹配规则如下（忽略大小写）：
     *
     * <pre>
     * 字段名    -》 方法名
     * isName  -》 isName
     * isName  -》 isIsName
     * isName  -》 getIsName
     * name     -》 isName
     * name     -》 getName
     * </pre>
     *
     * @param methodName    方法名
     * @param fieldName     字段名
     * @param isBooeanField 是否为Boolean类型字段
     * @return 是否匹配
     */
    private boolean isMatchGetter(String methodName, String fieldName, boolean isBooeanField) {
        // 全部转为小写，忽略大小写比较
        methodName = methodName.toLowerCase();
        fieldName = fieldName.toLowerCase();

        if (false == methodName.startsWith("get") && false == methodName.startsWith("is")) {
            // 非标准Getter方法
            return false;
        }
        if ("getclass".equals(methodName)) {
            //跳过getClass方法
            return false;
        }

        // 针对Boolean类型特殊检查
        if (isBooeanField) {
            if (fieldName.startsWith("is")) {
                // 字段已经是is开头
                if (methodName.equals(fieldName)
                        || methodName.equals("get" + fieldName)
                        || methodName.equals("is" + fieldName)
                ) {
                    return true;
                }
            } else if (methodName.equals("is" + fieldName)) {
                // 字段非is开头， name -》 isName
                return true;
            }
        }

        // 包括boolean的任何类型只有一种匹配情况：name -》 getName
        return methodName.equals("get" + fieldName);
    }

    /**
     * 方法是否为Setter方法<br>
     * 匹配规则如下（忽略大小写）：
     *
     * <pre>
     * 字段名    -》 方法名
     * isName  -》 setName
     * isName  -》 setIsName
     * name     -》 setName
     * </pre>
     *
     * @param methodName    方法名
     * @param fieldName     字段名
     * @param isBooeanField 是否为Boolean类型字段
     * @return 是否匹配
     */
    private boolean isMatchSetter(String methodName, String fieldName, boolean isBooeanField) {
        // 全部转为小写，忽略大小写比较
        methodName = methodName.toLowerCase();
        fieldName = fieldName.toLowerCase();

        // 非标准Setter方法跳过
        if (false == methodName.startsWith("set")) {
            return false;
        }

        // 针对Boolean类型特殊检查
        if (isBooeanField && fieldName.startsWith("is")) {
            // 字段是is开头
            if (methodName.equals("set" + StringUtils.removePrefix(fieldName, "is"))
                    || methodName.equals("set" + fieldName)
            ) {
                return true;
            }
        }

        // 包括boolean的任何类型只有一种匹配情况：name -》 setName
        return methodName.equals("set" + fieldName);
    }
    // ------------------------------------------------------------------------------------------------------ Private method end

    /**
     * 属性描述
     */
    public static class PropDesc {

        /**
         * 字段
         */
        private final Field field;
        /**
         * Getter方法
         */
        private Method getter;
        /**
         * Setter方法
         */
        private Method setter;

        /**
         * 构造<br>
         * Getter和Setter方法设置为默认可访问
         *
         * @param field  字段
         * @param getter get方法
         * @param setter set方法
         */
        public PropDesc(Field field, Method getter, Method setter) {
            this.field = field;
            this.getter = ClassUtils.setAccessible(getter);
            this.setter = ClassUtils.setAccessible(setter);
        }

        /**
         * 获取字段名，如果存在Alias注解，读取注解的值作为名称
         *
         * @return 字段名
         */
        public String getFieldName() {
            return ReflectUtils.getFieldName(this.field);
        }

        /**
         * 获取字段名称
         *
         * @return 字段名
         * @since 5.1.6
         */
        public String getRawFieldName() {
            return null == this.field ? null : this.field.getName();
        }

        /**
         * 获取字段
         *
         * @return 字段
         */
        public Field getField() {
            return this.field;
        }

        /**
         * 获得字段类型<br>
         * 先获取字段的类型，如果字段不存在，则获取Getter方法的返回类型，否则获取Setter的第一个参数类型
         *
         * @return 字段类型
         */
        public Type getFieldType() {
            if (null != this.field) {
                return TypeUtils.getType(this.field);
            }
            return findPropType(getter, setter);
        }

        /**
         * 获得字段类型<br>
         * 先获取字段的类型，如果字段不存在，则获取Getter方法的返回类型，否则获取Setter的第一个参数类型
         *
         * @return 字段类型
         */
        public Class<?> getFieldClass() {
            if (null != this.field) {
                return TypeUtils.getClass(this.field);
            }
            return findPropClass(getter, setter);
        }

        /**
         * 获取Getter方法，可能为{@code null}
         *
         * @return Getter方法
         */
        public Method getGetter() {
            return this.getter;
        }

        /**
         * 获取Setter方法，可能为{@code null}
         *
         * @return {@link Method}Setter 方法对象
         */
        public Method getSetter() {
            return this.setter;
        }

        /**
         * 获取属性值<br>
         * 首先调用字段对应的Getter方法获取值，如果Getter方法不存在，则判断字段如果为public，则直接获取字段值
         *
         * @param bean Bean对象
         * @return 字段值
         * @since 4.0.5
         */
        public Object getValue(Object bean) {
            if (null != this.getter) {
                return ReflectUtils.invoke(bean, this.getter);
            } else if (ModifierUtils.isPublic(this.field)) {
                return ReflectUtils.getFieldValue(bean, this.field);
            }
            return null;
        }

        /**
         * 获取属性值，自动转换属性值类型<br>
         * 首先调用字段对应的Getter方法获取值，如果Getter方法不存在，则判断字段如果为public，则直接获取字段值
         *
         * @param bean        Bean对象
         * @param valueType   返回属性值类型，null表示不转换
         * @param ignoreError 是否忽略错误，包括转换错误和注入错误
         * @return this
         * @since 5.4.2
         */
        public Object getValueWithConvert(Object bean, Type valueType, boolean ignoreError) {
            Object result = null;
            try {
                result = getValue(bean);
            } catch (Exception e) {
                if (false == ignoreError) {
                    throw new BeanException(e, "Get value of [{}] error!", getFieldName());
                }
            }

            if (null != result && null != valueType) {
                // 尝试将结果转换为目标类型，如果转换失败，返回原类型。
                final Object convertValue = Convert.convertWithCheck(valueType, result, null, ignoreError);
                if (null != convertValue) {
                    result = convertValue;
                }
            }
            return result;
        }

        /**
         * 设置Bean的字段值<br>
         * 首先调用字段对应的Setter方法，如果Setter方法不存在，则判断字段如果为public，则直接赋值字段值
         *
         * @param bean  Bean对象
         * @param value 值，必须与字段值类型匹配
         * @return this
         * @since 4.0.5
         */
        public PropDesc setValue(Object bean, Object value) {
            if (null != this.setter) {
                ReflectUtils.invoke(bean, this.setter, value);
            } else if (ModifierUtils.isPublic(this.field)) {
                ReflectUtils.setFieldValue(bean, this.field, value);
            }
            return this;
        }

        /**
         * 设置属性值，可以自动转换字段类型为目标类型
         *
         * @param bean        Bean对象
         * @param value       属性值，可以为任意类型
         * @param ignoreNull  是否忽略{@code null}值，true表示忽略
         * @param ignoreError 是否忽略错误，包括转换错误和注入错误
         * @return this
         * @since 5.4.2
         */
        public PropDesc setValueWithConvert(Object bean, Object value, boolean ignoreNull, boolean ignoreError) {
            if (ignoreNull && null == value) {
                return this;
            }

            // 当类型不匹配的时候，执行默认转换
            if (null != value) {
                final Class<?> propClass = getFieldClass();
                if (false == propClass.isInstance(value)) {
                    value = Convert.convertWithCheck(propClass, value, null, ignoreError);
                }
            }

            // 属性赋值
            if (null != value || false == ignoreNull) {
                try {
                    this.setValue(bean, value);
                } catch (Exception e) {
                    if (false == ignoreError) {
                        throw new BeanException(e, "Set value of [{}] error!", getFieldName());
                    }
                    // 忽略注入失败
                }
            }

            return this;
        }

        /**
         * 字段和Getter方法是否为Transient关键字修饰的
         *
         * @return 是否为Transient关键字修饰的
         * @since 5.3.11
         */
        public boolean isTransient() {
            boolean isTransient = ModifierUtils.hasModifier(this.field, ModifierUtils.ModifierType.TRANSIENT);

            // 检查Getter方法
            if (false == isTransient && null != this.getter) {
                isTransient = ModifierUtils.hasModifier(this.getter, ModifierUtils.ModifierType.TRANSIENT);

                // 检查注解
                if (false == isTransient) {
                    isTransient = AnnotationUtils.hasAnnotation(this.getter, Transient.class);
                }
            }

            return isTransient;
        }

        /**
         * 检查字段是否被忽略读，通过{@link PropIgnore} 注解完成，规则为：
         * <pre>
         *     1. 在字段上有{@link PropIgnore} 注解
         *     2. 在getXXX方法上有{@link PropIgnore} 注解
         * </pre>
         *
         * @return 是否忽略读
         * @since 5.4.2
         */
        public boolean isIgnoreGet() {
            return AnnotationUtils.hasAnnotation(this.field, PropIgnore.class)
                    || AnnotationUtils.hasAnnotation(this.getter, PropIgnore.class);
        }

        /**
         * 检查字段是否被忽略写，通过{@link PropIgnore} 注解完成，规则为：
         * <pre>
         *     1. 在字段上有{@link PropIgnore} 注解
         *     2. 在setXXX方法上有{@link PropIgnore} 注解
         * </pre>
         *
         * @return 是否忽略写
         * @since 5.4.2
         */
        public boolean isIgnoreSet() {
            return AnnotationUtils.hasAnnotation(this.field, PropIgnore.class)
                    || AnnotationUtils.hasAnnotation(this.setter, PropIgnore.class);
        }

        //------------------------------------------------------------------------------------ Private method start

        /**
         * 通过Getter和Setter方法中找到属性类型
         *
         * @param getter Getter方法
         * @param setter Setter方法
         * @return {@link Type}
         */
        private Type findPropType(Method getter, Method setter) {
            Type type = null;
            if (null != getter) {
                type = TypeUtils.getReturnType(getter);
            }
            if (null == type && null != setter) {
                type = TypeUtils.getParamType(setter, 0);
            }
            return type;
        }

        /**
         * 通过Getter和Setter方法中找到属性类型
         *
         * @param getter Getter方法
         * @param setter Setter方法
         * @return {@link Type}
         */
        private Class<?> findPropClass(Method getter, Method setter) {
            Class<?> type = null;
            if (null != getter) {
                type = TypeUtils.getReturnClass(getter);
            }
            if (null == type && null != setter) {
                type = TypeUtils.getFirstParamClass(setter);
            }
            return type;
        }
        //------------------------------------------------------------------------------------ Private method end
    }
}
