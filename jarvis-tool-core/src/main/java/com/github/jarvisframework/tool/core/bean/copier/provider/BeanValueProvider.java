package com.github.jarvisframework.tool.core.bean.copier.provider;

import com.github.jarvisframework.tool.core.bean.BeanDesc;
import com.github.jarvisframework.tool.core.bean.BeanDesc.PropDesc;
import com.github.jarvisframework.tool.core.bean.BeanUtils;
import com.github.jarvisframework.tool.core.bean.copier.ValueProvider;
import com.github.jarvisframework.tool.core.util.StringUtils;

import java.lang.reflect.Type;
import java.util.Map;

/**
 * <p>Bean的值提供者</p>
 *
 * @author Doug Wang
 * @since 1.0, 2020-07-29 19:48:03
 */
public class BeanValueProvider implements ValueProvider<String> {

    private final Object source;

    private final boolean ignoreError;

    private final Map<String, BeanDesc.PropDesc> sourcePdMap;

    /**
     * 构造
     *
     * @param bean        Bean
     * @param ignoreCase  是否忽略字段大小写
     * @param ignoreError 是否忽略字段值读取错误
     */
    public BeanValueProvider(Object bean, boolean ignoreCase, boolean ignoreError) {
        this.source = bean;
        this.ignoreError = ignoreError;
        sourcePdMap = BeanUtils.getBeanDesc(source.getClass()).getPropMap(ignoreCase);
    }

    @Override
    public Object value(String key, Type valueType) {
        final PropDesc sourcePd = getPropDesc(key, valueType);

        Object result = null;
        if (null != sourcePd) {
            result = sourcePd.getValueWithConvert(this.source, valueType, this.ignoreError);
        }
        return result;
    }

    @Override
    public boolean containsKey(String key) {
        final PropDesc sourcePd = getPropDesc(key, null);

        // 字段描述不存在或忽略读的情况下，表示不存在
        return null != sourcePd && false == sourcePd.isIgnoreGet();
    }

    /**
     * 获得属性描述
     *
     * @param key       字段名
     * @param valueType 值类型，用于判断是否为Boolean，可以为null
     * @return 属性描述
     */
    private PropDesc getPropDesc(String key, Type valueType) {
        PropDesc sourcePd = sourcePdMap.get(key);
        if (null == sourcePd && (null == valueType || Boolean.class == valueType || boolean.class == valueType)) {
            //boolean类型字段字段名支持两种方式
            sourcePd = sourcePdMap.get(StringUtils.upperFirstAndAddPre(key, "is"));
        }

        return sourcePd;
    }

}
