package me.leoray.wehttp;

import android.os.Handler;
import android.os.Looper;
import okhttp3.Call;
import okhttp3.OkHttpClient;

import java.util.List;

/**
 * 后面再考虑静态方法
 * Created by leoray on 2017/7/14.
 */
public class WeOkHttp {

    private static Handler uiHandler = new Handler(Looper.getMainLooper()); //主线程回调

    private WeConfig weConfig;

    public WeConfig init() {
        return config();
    }

    public <T> SimpleReq<T> get(String url) {
        return new SimpleReq(this, SimpleReq.GET, url);
    }


    public <T> SimpleReq<T> head(String url) {
        return new SimpleReq(this, SimpleReq.HEAD, url);
    }

    public <T> BodyReq<T> post(String url) {
        return new BodyReq(this, BodyReq.POST, url);
    }

    public <T> BodyReq<T> put(String url) {
        return new BodyReq(this, BodyReq.PUT, url);
    }

    public <T> BodyReq<T> delete(String url) {
        return new BodyReq(this, BodyReq.DELETE, url);
    }

    public <T> BodyReq<T> patch(String url) {
        return new BodyReq(this, BodyReq.PATCH, url);
    }

    /**
     * 拿到配置对象
     *
     * @return
     */
    public WeConfig config() {
        if (weConfig == null) {
            weConfig = new WeConfig();
        }
        return weConfig;
    }

    /**
     * 拿到OkHttpClient对象
     *
     * @return
     */
    public OkHttpClient client() {
        return weConfig.client();
    }

    /**
     * 取消指定tag的请求
     *
     * @param tag
     */
    public void cancel(Object tag) {
        if (tag == null) {
            client().dispatcher().cancelAll();
            return;
        }
        List<Call> runningCalls = weConfig.client().dispatcher().runningCalls();
        cancelCall(tag, runningCalls);
        List<Call> enqueuedCalls = weConfig.client().dispatcher().queuedCalls();
        cancelCall(tag, enqueuedCalls);
    }

    private void cancelCall(Object tag, List<Call> runningCalls) {
        for (int i = 0; i < runningCalls.size(); i++) {
            Call call = runningCalls.get(i);
            if (tag != null && tag.equals(call.request().tag())) {
                call.cancel();
            }
        }
    }


    public static void runUi(Runnable runnable) {
        if (runnable != null) {
            uiHandler.post(runnable);
        }
    }
}
