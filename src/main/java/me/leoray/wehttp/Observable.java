package me.leoray.wehttp;

/**
 * Created by leoray on 2017/7/17.
 */
public abstract class Observable<T> {
    private WeReq<T> weReq;


    public Observable() {
    }

    public Observable(WeReq<T> weReq) {
        this.weReq = weReq;
    }

    public static <T> Observable<T> error(final int code, final String msg) {
        return new Observable<T>() {
            @Override
            public void subscribe(WeReq.WeCallback<T> callback) {
                callback.onFailed(null, WeReq.WeCallback.TYPE_LOCAL, code, msg, null);
            }
        };
    }

    public abstract void subscribe(WeReq.WeCallback<T> callback);

    public void cancel() {
        if (weReq != null) {
            weReq.cancel();
        }
    }

}
