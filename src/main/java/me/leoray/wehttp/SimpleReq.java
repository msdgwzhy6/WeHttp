package me.leoray.wehttp;

import okhttp3.Call;

/**
 * Get请求
 * Created by leoray on 2017/7/14.
 */
public class SimpleReq<T> extends BaseReq<T, SimpleReq> {
    public static final String HEAD = "HEAD";
    public static final String GET = "GET";

    public SimpleReq(WeOkHttp config, String method, String url) {
        super(config, method, url);
    }

    protected Call getCall() {
        requestBuilder.url(getUrl().build()).method(method, null);
        return weHttp.client().newCall(requestBuilder.build());
    }
}
