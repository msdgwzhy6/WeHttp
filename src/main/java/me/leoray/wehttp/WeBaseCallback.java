package me.leoray.wehttp;

import java.io.IOException;

/**
 * Created by leoray on 2017/7/16.
 */
public abstract class WeBaseCallback<T> extends BaseCallback<Resp<T>> {
    private static final int CODE_SUCCESS = 0;
    private int success = CODE_SUCCESS;

    /**
     * 设置成功码
     *
     * @param success
     */
    public void success(int success) {
        this.success = success;
    }

    @Override
    public void onStart(WeReq call) {

    }

    @Override
    public void onFinish() {

    }

    @Override
    public void onFailed(WeReq call, int type, int code, String msg, IOException e) {
        failed(call, type, code, msg, e);
    }

    @Override
    public void onSuccess(WeReq call, Resp<T> data) {
        if (data.getCode() == success) {
            success(call, data.getResult());
        } else {
            onFailed(call, TYPE_SERVER, data.getCode(), data.getMsg(), null);
        }
    }

    public abstract void success(WeReq call, T t);

    public abstract void failed(WeReq call, int type, int code, String msg, IOException e);
}