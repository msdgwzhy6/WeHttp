package me.leoray.wehttp;

/**
 * 默认实现了onStart和onFinish方法的回调
 *
 * @param <T>
 */
public abstract class BaseCallback<T> implements WeReq.WeCallback<T> {
    @Override
    public void onStart(WeReq call) {

    }

    @Override
    public void onFinish() {

    }
}
