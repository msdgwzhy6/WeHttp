package me.leoray.wehttp;

import okhttp3.CookieJar;

/**
 * Created by leoray on 2017/7/16.
 */
public interface WeCookie extends CookieJar {
    void clearCookie();
}
