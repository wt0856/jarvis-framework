package com.jarvisframework.tool.ctypto.digest;

import java.io.File;
import java.io.InputStream;
import java.nio.charset.Charset;

/**
 * <p>MD5算法</p>
 *
 * @author 王涛
 * @since 1.0, 2020-07-22 10:44:04
 */
public class MD5 extends Digester {

    /**
     * 序列化ID
     */
    private static final long serialVersionUID = -1298434009717441074L;

    /**
     * 创建MD5实例
     *
     * @return MD5
     * @since 4.6.0
     */
    public static MD5 create() {
        return new MD5();
    }

    /**
     * 构造
     */
    public MD5() {
        super(DigestAlgorithmEnum.MD5);
    }

    /**
     * 构造
     *
     * @param salt 盐值
     */
    public MD5(byte[] salt) {
        this(salt, 0, 1);
    }

    /**
     * 构造
     *
     * @param salt        盐值
     * @param digestCount 摘要次数，当此值小于等于1,默认为1。
     */
    public MD5(byte[] salt, int digestCount) {
        this(salt, 0, digestCount);
    }

    /**
     * 构造
     *
     * @param salt         盐值
     * @param saltPosition 加盐位置，即将盐值字符串放置在数据的index数，默认0
     * @param digestCount  摘要次数，当此值小于等于1,默认为1。
     */
    public MD5(byte[] salt, int saltPosition, int digestCount) {
        this();
        this.salt = salt;
        this.saltPosition = saltPosition;
        this.digestCount = digestCount;
    }

    /**
     * 生成16位MD5摘要
     *
     * @param data    数据
     * @param charset 编码
     * @return 16位MD5摘要
     * @since 4.6.0
     */
    public String digestHex16(String data, Charset charset) {
        return DigestUtils.md5HexTo16(digestHex(data, charset));
    }

    /**
     * 生成16位MD5摘要
     *
     * @param data 数据
     * @return 16位MD5摘要
     * @since 4.5.1
     */
    public String digestHex16(String data) {
        return DigestUtils.md5HexTo16(digestHex(data));
    }

    /**
     * 生成16位MD5摘要
     *
     * @param data 数据
     * @return 16位MD5摘要
     * @since 4.5.1
     */
    public String digestHex16(InputStream data) {
        return DigestUtils.md5HexTo16(digestHex(data));
    }

    /**
     * 生成16位MD5摘要
     *
     * @param data 数据
     * @return 16位MD5摘要
     */
    public String digestHex16(File data) {
        return DigestUtils.md5HexTo16(digestHex(data));
    }

    /**
     * 生成16位MD5摘要
     *
     * @param data 数据
     * @return 16位MD5摘要
     * @since 4.5.1
     */
    public String digestHex16(byte[] data) {
        return DigestUtils.md5HexTo16(digestHex(data));
    }
}
