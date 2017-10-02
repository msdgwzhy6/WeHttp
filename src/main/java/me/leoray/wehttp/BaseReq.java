package me.leoray.wehttp;

import okhttp3.*;

import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * 基础请求类
 * Created by leoray on 2017/7/15.
 */
public abstract class BaseReq<T, R extends BaseReq> implements WeReq<T> {
    protected String method;
    protected String url; //请求配置的URL
    protected Map<String, String> params; //请求配置的url query
    protected WeOkHttp weHttp; //http对象
    protected Request.Builder requestBuilder; //请求构造器
    private Call call; //存储的调用类

    public BaseReq(WeOkHttp weHttp, String method, String url) {
        this.weHttp = weHttp;
        this.method = method;
        this.url = url;
        this.requestBuilder = new Request.Builder();
        //添加公共header
        addHeaders(this.requestBuilder, weHttp.config().getHeaders());
    }


    protected final Request.Builder builder() {
        return requestBuilder;
    }

    public final R tag(Object tag) {
        requestBuilder.tag(tag);
        return (R) this;
    }

    public final R header(String key, String value) {
        requestBuilder.header(key, value);
        return (R) this;
    }

    public final R param(Map<String, String> params) {
        return (R) this;
    }

    public final R param(String key, String value) {
        if (params == null) {
            params = new HashMap<>();
        }
        return (R) this;
    }

    protected final HttpUrl.Builder getUrl() {
        String url = weHttp.config().getUrl(this.url);
        HttpUrl httpUrl = HttpUrl.parse(url);
        HttpUrl.Builder builder = httpUrl.newBuilder();
        //添加公共Query
        addParams(builder, weHttp.config().getParams());
        //添加当前请求的Query
        HttpUrl.Builder newBuilder = addParams(builder, this.params);
        return newBuilder;
    }


    @Override
    public T execute(Class<T> classOfReturn) {
        Call call = getWeCall();
        if (classOfReturn == Call.class) {
            return (T) call;
        }
        try {
            Response r = call.execute();
            if (classOfReturn == Response.class) {
                return (T) r;
            } else if (classOfReturn == String.class) {
                return (T) r.body().string();
            } else if (classOfReturn == Object.class) { //未指定
                return (T) r;
            } else {
                return weHttp.config().adapter().from(r.body().string(), classOfReturn);
            }
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    @Override
    public Observable subscribe(final Class<T> classOfReturn) {
        Observable<T> observable = new Observable<T>(this) {
            @Override
            public void subscribe(WeCallback<T> callback) {
                execute(classOfReturn, callback);
            }
        };
        return observable;
    }

    @Override
    public WeReq execute(final Class<T> classOfReturn, final WeCallback<T> callback) {
        Call call = getWeCall();
        callback.onStart(this);
        call.enqueue(new Callback() {
            @Override
            public void onFailure(final Call call, final IOException e) {
                final int errorCode = getErrorCode(e);
                final String errorMsg = getErrorMsg(e);
                WeOkHttp.runUi(new Runnable() {
                    @Override
                    public void run() {
                        callback.onFailed(BaseReq.this, WeCallback.TYPE_NETWORK, errorCode, errorMsg, e);
                        callback.onFinish();
                    }
                });
            }

            @Override
            public void onResponse(Call call, final Response response) throws IOException {
                try {
                    final T t;
                    if (classOfReturn == Response.class) {
                        t = (T) response;
                    } else {
                        if (response.code() < 200 || response.code() >= 300) { //网络请求失败
                            WeOkHttp.runUi(new Runnable() {
                                @Override
                                public void run() {
                                    callback.onFailed(BaseReq.this, WeCallback.TYPE_SERVER, response.code(), response.message(), null);
                                    callback.onFinish();
                                }
                            });
                            return;
                        }
                        if (classOfReturn == String.class) {
                            t = (T) response.body().string();
                        } else if (classOfReturn == Object.class) { //未指定
                            t = (T) response;
                        } else {
                            t = weHttp.config().adapter().from(response.body().string(), classOfReturn);
                        }
                    }
                    WeOkHttp.runUi(new Runnable() {
                        @Override
                        public void run() {
                            callback.onSuccess(BaseReq.this, t);
                            callback.onFinish();
                        }
                    });
                } catch (IOException e) {
                    onFailure(call, e);
                }
            }
        });
        return this;
    }

    private String getErrorMsg(IOException e) {
        return e.getMessage();
    }

    private int getErrorCode(IOException e) {
        return 0;
    }


    @Override
    public void cancel() {
        getWeCall().cancel();
    }

    private Call getWeCall() {
        if (this.call == null) {
            this.call = getCall();
        }
        return this.call;
    }

    @Override
    public WeConfig context() {
        return weHttp.config();
    }

    /**
     * 生成请求
     *
     * @return
     */
    protected abstract Call getCall();

    private void addHeaders(Request.Builder requestBuilder, Map<String, String> headers) {
        if (headers == null || headers.size() == 0) {
            return;
        }
        for (Map.Entry<String, String> entry : headers.entrySet()) {
            requestBuilder.header(entry.getKey(), entry.getValue());
        }

    }

    private HttpUrl.Builder addParams(HttpUrl.Builder builder, Map<String, String> params) {
        if (params == null || params.size() == 0) {
            return builder;
        }
        Iterator<Map.Entry<String, String>> iterator = params.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<String, String> next = iterator.next();
            builder.addQueryParameter(next.getKey(), next.getValue());
        }
        return builder;
    }

}


