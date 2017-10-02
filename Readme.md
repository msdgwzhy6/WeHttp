一个基于OkHttp3的面向Android开发的网络库封装,另外包含了一个简易的JSON转换库.

# 使用方法

## 全局配置
```
 /**
 * 初始化WeHttp
 */
public void initWeHttp() {
    /**
     * 全局配置
     */

    //自定义JSON转换器:默认WeJson
    TypeAdapter adapter = new TypeAdapter() {
        @Override
        public <T> T from(String s, Class<T> classOfT) {
            return null;
        }

        @Override
        public <T> String to(T t) {
            return null;
        }
    };
    //自定义Cookie管理策略,默认不管理Cookie
    WeCookie weCookie = new WeCookie() {
        @Override
        public void clearCookie() {

        }

        @Override
        public void saveFromResponse(HttpUrl url, List<Cookie> cookies) {

        }

        @Override
        public List<Cookie> loadForRequest(HttpUrl url) {
            return null;
        }
    };
    WeHttp.init()
            //自定义JSON转换器,默认使用的是内置的WeJson
            .adapter(adapter)
            //定义全局header
            .header("world", "world")
            .header("hello", "hello")
            //定义全局的param,就是url中的键值对参数
            .params("key", "value")
            //定义PIN校验
            .addPin("", "")
            //设置base url
            .baseUrl("...")
            //定义Cookie保存策略
            .cookie(weCookie)
            //也可以选择使用内置的Android WebView管理Cookie
            .cookieWebView(null)
            //设置代理
            .proxy("ip", 80, "username", "password")
            //设置日志级别
            .log(WeLog.Level.NONE)
            //设置超时时间
            .timeout(1000, 1000, 1000)
            //拿到OkHttp的配置对象进行配置
            .clientConfig().retryOnConnectionFailure(true);


}
```

## 发送请求

```
public void send() {
    //这里定义的返回类型都是String(为了例子简单,不想多定义其他类了),开发者可以设置为自己的POJO对象
    //这里都是演示调用,无法执行
    //同步GET请求
    WeHttp.<String>get("get url")
            //单个Query
            .param("key", "value")
            //批量添加Query
            .param(new HashMap<String, String>())
            //设置tag
            .tag("tag")
            //执行
            .execute(String.class);
    //POST 异步请求
    WeHttp.<String>post("post url")
            //post请求体有多种选择
            .bodyJson("{}") //json
            .bodyText("text") //plain text
            .bodyFile(new File("")) //文件
            //传入一个POJO对象, 如果POJO中有有效文件则为MultiPart请求,否则为JSON请求
            .body(null)
            //一旦传入下面两个方法,请求会自动转换为MultiPart类型
            .addPart("name", new File(""), WeMediaType.PNG)
            .addBodyQuery("name", "value")
            //回调都在主线程,onStart()在execute()调用所在线程
            .execute(String.class, new WeReq.WeCallback<String>() {
                @Override
                public void onStart(WeReq call) {

                }

                @Override
                public void onFinish() {

                }

                @Override
                public void onFailed(WeReq call, int type, int code, String msg, IOException e) {

                }

                @Override
                public void onSuccess(WeReq call, String data) {

                }
            })
    ;
    //其他请求,delete,put,patch等
    WeHttp.delete("delete url");
    //取消请求:通过前面定义的tag
    WeHttp.cancel("tag");
    //类似RxJava的subscribe回调方式
    Observable subscribe = WeHttp.<String>get("").subscribe(String.class);
    subscribe.subscribe(new WeReq.WeCallback<String>() {
        @Override
        public void onStart(WeReq call) {

        }

        @Override
        public void onFinish() {

        }

        @Override
        public void onFailed(WeReq call, int type, int code, String msg, IOException e) {

        }

        @Override
        public void onSuccess(WeReq call, String data) {

        }
    });
    //返回其他类型,将不会进行JSON转换操作
    //返回类型为Response,直接返回OkHttp的Response对象.这在框架不能满足的时候调用
    WeHttp.<Response>post("").execute(Response.class);
    //返回类型为String会直接将Response中的结果转换为字符串返回
    WeHttp.<String>post("").execute(String.class);


}
```

## JSON转换
```
public void testJson(){
    new WeJson().toJson(null);
    new WeJson().fromJson("", String.class);
}
```