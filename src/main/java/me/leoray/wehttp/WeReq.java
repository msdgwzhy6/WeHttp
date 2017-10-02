package me.leoray.wehttp;

import java.io.IOException;

/**
 * T 代表请求拿到的数据
 * Created by leoray on 2017/7/14.
 */
public interface WeReq<T> {

    WeConfig context();

    /**
     * 同步请求
     *
     * @return
     */
    T execute(Class<T> clazzOfReturn);

    /**
     * 异步订阅
     *
     * @param classOfReturn
     * @return
     */
    Observable subscribe(final Class<T> classOfReturn);

    /**
     * 异步请求
     *
     * @param callback
     * @return
     */
    WeReq execute(Class<T> classOfReturn, WeCallback<T> callback);

    /**
     * 取消该请求
     */
    void cancel();

    /**
     * 注意,除了onStart()以外,所有回调都在主线程执行,onStart()在请求调用所在线程执行
     *
     * @param <T>
     */
    interface WeCallback<T> {
        int TYPE_SERVER = 0; //代表code是服务器返回的错误(服务器请求成功)
        int TYPE_LOCAL = 1; //代表是客户端本身的错误,错误码也是本地定义的错误码
        int TYPE_NETWORK = 2; //代表code是HTTP请求的错误码

        void onStart(WeReq call);

        void onFinish();

        void onFailed(WeReq call, int type, int code, String msg, IOException e);

        void onSuccess(WeReq call, T data);
    }
}
