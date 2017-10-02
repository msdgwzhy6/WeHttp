package me.leoray.wehttp;

import android.content.Context;
import android.webkit.CookieManager;
import android.webkit.CookieSyncManager;
import okhttp3.Cookie;
import okhttp3.HttpUrl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 基于WebView的Cookie
 * Created by leoray on 2017/7/16.
 */
public class WeWebViewCookie implements WeCookie {
    private Context ctx;

    public WeWebViewCookie(Context ctx) {
        if (ctx == null) {
            throw new IllegalArgumentException("ctx 不能为null");
        }
        this.ctx = ctx;
        CookieSyncManager.createInstance(ctx.getApplicationContext());
        CookieManager.getInstance();
    }

    @Override
    public void clearCookie() {
        CookieManager.getInstance().removeAllCookie();
    }

    @Override
    public void saveFromResponse(HttpUrl url, List<Cookie> cookies) {
        if (cookies == null || cookies.size() == 0) {
            return;
        }
        for (int i = 0; i < cookies.size(); i++) {
            CookieManager.getInstance().setCookie(url.toString(), cookies.get(i).toString());
        }
        CookieSyncManager.getInstance().sync();
    }

    @Override
    public List<Cookie> loadForRequest(HttpUrl url) {
        String cookie = CookieManager.getInstance().getCookie(url.toString());
        if (cookie != null) {
            final String[] cookies = cookie.split(";");
            List<Cookie> list = new ArrayList<>();
            Cookie c;
            for (String s : cookies) {
                if (s == null) {
                    continue;
                }
                c = Cookie.parse(url, s);
                if (c == null) {
                    continue;
                }
                list.add(c);
            }
            return list;
        }
        return Collections.emptyList();
    }
}
