package me.leoray.wehttp;

import android.content.Context;
import okhttp3.OkHttpClient;

import java.util.concurrent.TimeUnit;

/**
 * 默认的MyOkHttp
 * Created by leoray on 2017/7/16.
 */
public class WeHttp {
    private static WeOkHttp myOkHttp = new WeOkHttp();


    /**
     * @return
     */
    public static WeConfig init() {
        return myOkHttp.init();
    }

    public static <T> SimpleReq<T> get(String url) {
        return myOkHttp.get(url);
    }

    /**
     * 一键快速初始化
     * 设置超时时间,设置日志级别
     *
     * @param ctx
     * @param baseUrl
     * @param debug   TRUE则开启网络日志
     * @param pins    如果不校验可以不传
     */
    public static WeConfig init(Context ctx, boolean debug, String baseUrl, String... pins) {
        if (ctx == null) {
            throw new IllegalArgumentException("ctx must not be null");
        }
        //拿到OkHttp的配置对象进行配置
        WeHttp.config().clientConfig().connectTimeout(20, TimeUnit.SECONDS).readTimeout(20, TimeUnit.SECONDS);
        //WeHttp封装的配置
        WeHttp.config()
                //PIN
                .addPin(pins)
                .log(debug ? WeLog.Level.BODY : WeLog.Level.NONE)
                //使用系统的CookieManager进行Cookie管理
                .cookieWebView(ctx.getApplicationContext())
                //JSON序列化和反序列化
                .adapter(new WeTypeAdapter())
                //base url
                .baseUrl(baseUrl);
        return WeHttp.config();
    }

    public static <T> SimpleReq<T> head(String url) {
        return myOkHttp.head(url);
    }

    public static <T> BodyReq<T> post(String url) {
        return myOkHttp.post(url);
    }

    public static <T> BodyReq<T> put(String url) {
        return myOkHttp.put(url);
    }

    public static <T> BodyReq<T> delete(String url) {
        return myOkHttp.delete(url);
    }

    public static <T> BodyReq<T> patch(String url) {
        return myOkHttp.patch(url);
    }

    /**
     * 拿到配置对象
     *
     * @return
     */
    public static WeConfig config() {
        return myOkHttp.config();
    }

    /**
     * 拿到OkHttpClient对象
     *
     * @return
     */
    public static OkHttpClient client() {
        return myOkHttp.client();
    }

    /**
     * 取消指定tag的请求
     *
     * @param tag
     */
    public static void cancel(Object tag) {
        myOkHttp.cancel(tag);
    }

}
