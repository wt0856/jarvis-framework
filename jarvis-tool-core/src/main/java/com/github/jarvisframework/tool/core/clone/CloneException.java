package com.github.jarvisframework.tool.core.clone;

import com.github.jarvisframework.tool.core.exception.ExceptionUtils;
import com.github.jarvisframework.tool.core.util.StringUtils;

/**
 * <p>克隆异常</p>
 *
 * @author Doug Wang
 * @since 1.0, 2020-07-29 17:17:26
 */
public class CloneException extends RuntimeException {

    private static final long serialVersionUID = 6774837422188798989L;

    public CloneException(Throwable e) {
        super(ExceptionUtils.getMessage(e), e);
    }

    public CloneException(String message) {
        super(message);
    }

    public CloneException(String messageTemplate, Object... params) {
        super(StringUtils.format(messageTemplate, params));
    }

    public CloneException(String message, Throwable throwable) {
        super(message, throwable);
    }

    public CloneException(Throwable throwable, String messageTemplate, Object... params) {
        super(StringUtils.format(messageTemplate, params), throwable);
    }
}
