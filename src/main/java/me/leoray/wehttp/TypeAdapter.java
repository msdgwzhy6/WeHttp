package me.leoray.wehttp;

/**
 * 类型转换器
 * Created by leoray on 2017/7/16.
 */
public interface TypeAdapter {

    /**
     * 字符串转换为对象
     *
     * @param s
     * @param classOfT
     * @param <T>
     * @return
     */
    <T> T from(String s, Class<T> classOfT);

    /*<T> T from(String s, Type type);*/

    /**
     * 对象转换为字符串
     *
     * @param t
     * @param <T>
     * @return
     */
    <T> String to(T t);
}
