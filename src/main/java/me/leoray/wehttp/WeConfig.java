package me.leoray.wehttp;

import android.content.Context;
import android.util.Log;
import okhttp3.Credentials;
import okhttp3.OkHttpClient;
import okio.ByteString;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.X509TrustManager;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.security.KeyManagementException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * 缓存
 * Created by leoray on 2017/7/15.
 */
public class WeConfig {

    private OkHttpClient.Builder httpConfig;
    private WeCookie weCookie;
    private OkHttpClient okHttpClient;
    private Map<String, String> headers = new HashMap<>();
    private Map<String, String> params = new HashMap<>();
    private String baseUrl;
    private volatile TypeAdapter adapter;
    private X509TrustManager x509TrustManager;
    private SSLSocketFactory sslSocketFactory;
    private List<byte[]> pins = new ArrayList<>();

    /**
     * 设置默认的对象转换器
     *
     * @param adapter
     */
    public WeConfig adapter(TypeAdapter adapter) {
        this.adapter = adapter;
        return this;
    }

    /**
     * 配置代理
     *
     * @param ip       代理IP
     * @param port     代理端口
     * @param userName 代理用户名
     * @param password 代理密码
     * @return
     */
    public WeConfig proxy(String ip, int port, String userName, String password) {
        clientConfig().proxy(new Proxy(Proxy.Type.HTTP, new InetSocketAddress(ip, port)));
        header("Proxy-Authorization", Credentials.basic(userName, password));
        return this;
    }

    public TypeAdapter adapter() {
        if (adapter == null) {
            synchronized (this) {
                if (adapter == null) {
                    adapter = new WeTypeAdapter();
                }
            }
        }
        return adapter;
    }

    /**
     * 快速设置连接,读,写的时间,单位秒
     *
     * @param connectTimeout
     * @param readTimeout
     * @param writeTimeout
     * @return
     */
    public WeConfig timeout(long connectTimeout, long readTimeout, long writeTimeout) {
        clientConfig().connectTimeout(connectTimeout, TimeUnit.SECONDS).readTimeout(readTimeout, TimeUnit.SECONDS).writeTimeout(writeTimeout, TimeUnit.SECONDS);
        return this;
    }

    /**
     * 添加pin,默认不校验pin
     *
     * @param pin
     * @return
     */
    public WeConfig addPin(String... pin) {
        if (pin == null || pin.length == 0) {
            return this;
        }
        for (String p : pin) {
            if (p == null) {
                continue;
            }
            pins.add(ByteString.decodeHex(p).toByteArray());
        }
        return this;
    }

    /**
     * OkHttp客户端配置
     *
     * @return
     */
    public OkHttpClient.Builder clientConfig() {
        if (httpConfig == null) {
            httpConfig = new OkHttpClient.Builder();
        }
        return httpConfig;
    }

    public Map<String, String> getHeaders() {
        return headers;
    }

    public Map<String, String> getParams() {
        return params;
    }

    /**
     * 配置头部,如果key相同会覆盖
     *
     * @param key
     * @param value
     * @return
     */
    public WeConfig header(String key, String value) {
        this.headers.put(key, value);
        return this;
    }

    public WeConfig header(Map<String, String> headers) {
        this.headers.putAll(headers);
        return this;
    }

    /**
     * 配置日志级别
     *
     * @param level
     * @return
     */
    public WeConfig log(WeLog.Level level) {
        WeLog loggingInterceptor = new WeLog();
        loggingInterceptor.setLevel(level);
        clientConfig().addInterceptor(loggingInterceptor);
        return this;
    }

    /**
     * 配置封装的Cookie策略
     *
     * @return
     */
    public WeConfig cookie(WeCookie cookie) {
        this.weCookie = cookie;
        clientConfig().cookieJar(this.weCookie);
        return this;
    }

    public WeCookie cookie() {
        return this.weCookie;
    }

    /**
     * 将Cookie写入到WebView
     *
     * @return
     */
    public WeConfig cookieWebView(Context ctx) {
        this.weCookie = new WeWebViewCookie(ctx);
        clientConfig().cookieJar(this.weCookie);
        return this;
    }

    /**
     * 公共请求参数
     *
     * @return
     */
    public WeConfig params(String key, String value) {
        this.params.put(key, value);
        return this;
    }

    public WeConfig params(Map<String, String> params) {
        this.params.putAll(params);
        return this;
    }

    /**
     * 设置baseUrl
     *
     * @param baseUrl
     * @return
     */
    public WeConfig baseUrl(String baseUrl) {
        if (baseUrl != null && !baseUrl.endsWith("/")) {
            baseUrl = baseUrl + "/";
        }
        this.baseUrl = baseUrl;
        return this;
    }

    public String getUrl(String url) {
        if (url == null) {
            throw new NullPointerException("url 不能为空");
        }
        if (url.startsWith("https://") || url.startsWith("http://")) {
            return url;
        }
        if (url.startsWith("/")) {
            url = url.substring(1);
        }
        return baseUrl + url;
    }

    public OkHttpClient client() {
        if (okHttpClient == null) {
            synchronized (WeConfig.class) {
                if (okHttpClient == null) {
                    SSLSocketFactory factory = getSSLFactory();
                    clientConfig().sslSocketFactory(factory, x509TrustManager);
                    okHttpClient = clientConfig().build();
                }
            }

        }
        return okHttpClient;
    }

    private SSLSocketFactory getSSLFactory() {
        try {
            SSLContext context = SSLContext.getInstance("TLS");
            x509TrustManager = new X509TrustManager() {
                @Override
                public void checkClientTrusted(X509Certificate[] x509Certificates, String s) throws CertificateException {

                }

                @Override
                public void checkServerTrusted(X509Certificate[] x509Certificates, String s) throws CertificateException {
                    if (WeConfig.this.pins != null && WeConfig.this.pins.size() > 0) { //设置了pin才会校验
                        checkPinTrust(x509Certificates);
                    }
                }

                @Override
                public X509Certificate[] getAcceptedIssuers() {
                    return new X509Certificate[0];
                }
            };
            context.init(null, new X509TrustManager[]{x509TrustManager}, null);
            this.sslSocketFactory = context.getSocketFactory();
            return this.sslSocketFactory;
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (KeyManagementException e) {
            e.printStackTrace();
        }

        return null;
    }

    private void checkPinTrust(X509Certificate[] chain)
            throws CertificateException {
        for (X509Certificate certificate : chain) {
            if (isValidPin(certificate)) {
                return;
            }
        }
        throw new CertificateException("No valid pins found in chain!");
    }

    private boolean isValidPin(X509Certificate certificate) throws CertificateException {
        try {
            final MessageDigest digest = MessageDigest.getInstance("SHA1");
            final byte[] spki = certificate.getPublicKey().getEncoded();
            final byte[] pin = digest.digest(spki);

            Log.i("OkHttp", "server pin:" + ByteString.of(pin).hex());

            for (byte[] validPin : this.pins) {
                Log.i("OkHttp", "local Pin:" + ByteString.of(validPin).hex());

                if (Arrays.equals(validPin, pin)) {
                    return true;
                }
            }

            return false;
        } catch (NoSuchAlgorithmException nsae) {
            throw new CertificateException(nsae);
        }
    }
}
