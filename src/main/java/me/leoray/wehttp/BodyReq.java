package me.leoray.wehttp;

import okhttp3.*;
import okhttp3.internal.Util;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import static java.lang.reflect.Modifier.PUBLIC;
import static java.lang.reflect.Modifier.STATIC;

/**
 * 有请求体的请求
 * Created by leoray on 2017/7/15.
 */
public class BodyReq<T> extends BaseReq<T, BodyReq> {
    public static final String POST = "POST";
    public static final String PUT = "PUT";
    public static final String PATCH = "PATCH";
    public static final String DELETE = "DELETE";
    private RequestBody body; //生成的body
    private File singleFile;
    private Map<String, MultiPart> fileMap = new HashMap<>();
    private Map<String, String> queryMap = new HashMap<>();
    private MediaType contentType;

    public BodyReq(WeOkHttp config, String method, String url) {
        super(config, method, url);
    }

    public static class MultiPart {
        public String name;
        public String fileName;
        public File file;

        public MediaType mediaType;

        public MultiPart(String name, String fileName, File file, MediaType mediaType) {
            this.name = name;
            this.fileName = fileName;
            this.file = file;
            this.mediaType = mediaType;
        }

        public static MultiPart create(String name, File file, MediaType mediaType) {
            return new MultiPart(name, null, file, mediaType);
        }

        public static MultiPart create(String name, String fileName, File file, MediaType mediaType) {
            return new MultiPart(name, fileName, file, mediaType);
        }

    }

    /**
     * 设置Body内容的媒体类型
     *
     * @param mediaType
     * @return
     */
    /*public BodyReq contentType(MediaType mediaType) {
        this.contentType = mediaType;
        return this;
    }*/

    /**
     * 执行类型是multiPart类型
     * 如果执行了{@link #addPart(String, File)}则会自动设置类型
     *
     * @return
     */
    public BodyReq<T> multiPart() {
        this.contentType = WeMediaType.MULTIPART;
        return this;
    }

    @Override
    protected Call getCall() {
        if (isMultiPart()) { //multipart/form-data
            MultipartBody.Builder builder = new MultipartBody.Builder();
            Iterator<Map.Entry<String, String>> iterator = queryMap.entrySet().iterator();
            while (iterator.hasNext()) {
                Map.Entry<String, String> next = iterator.next();
                builder.addFormDataPart(next.getKey(), next.getValue());
            }
            Iterator<Map.Entry<String, MultiPart>> fileIterator = fileMap.entrySet().iterator();
            while (fileIterator.hasNext()) {
                Map.Entry<String, MultiPart> next = fileIterator.next();
                MultiPart value = next.getValue();
                builder.addFormDataPart(next.getKey(), value.fileName, RequestBody.create(getMediaType(value.file), value.file));
            }
            this.body = builder.build();
        } else if (contentType == null) { //application/x-www-form-urlencoded
            if (queryMap.size() > 0) {
                this.contentType = WeMediaType.FORM;
                this.body = RequestBody.create(WeMediaType.FORM, getUrlEncodedQuery(queryMap));
            } else {
                this.body = Util.EMPTY_REQUEST;
            }
        } else if (body == null) { //file
            this.body = RequestBody.create(this.contentType, singleFile);
        } else { //其他:json,text等

        }
        Request.Builder builder = new Request.Builder().url(getUrl().build());
        if (method == null) {
            method = POST;
        }
        builder.method(method, body);
        return weHttp.client().newCall(builder.build());
    }

    public BodyReq<T> addPart(String name, File file, MediaType type) {
        if (file == null) {
            throw new IllegalArgumentException("file cannot be null");
        }
        multiPart();
        fileMap.put(name, MultiPart.create(name, file.getName(), file, type));
        return this;
    }

    public BodyReq<T> addPart(String name, File file) {
        addPart(name, file, getMediaType(file));
        return this;
    }

    /**
     * 发送一个实体对象
     * 该方法通过反射读取每个字段的值,如果不是public的则需要实现get方法
     * 但是如果其中包含其他的实体类则默认是转换为字符串
     * 同时会查找父类中的字段
     *
     * @param model 如果model中有文件类型的成员变量则请求为part类型,否则为JSON类型
     * @return
     */
    public BodyReq<T> body(Object model) {
        if (model == null) {
            return bodyJson("");
        }
        Field[] declaredFields = model.getClass().getDeclaredFields(); //当前类字段
        Field[] superFields = model.getClass().getSuperclass().getDeclaredFields(); //父类字段
        Field[] fs = new Field[declaredFields.length + superFields.length];
        for (int i = 0; i < declaredFields.length; i++) {
            fs[i] = declaredFields[i];
        }
        for (int i = declaredFields.length; i < fs.length; i++) {
            fs[i] = superFields[i - declaredFields.length];
        }
        if (fs == null || fs.length == 0) {
            return bodyJson("");
        }
        Map<String, Object> map = new HashMap<>();
        boolean hasFile = false;
        Field f;
        try {
            for (int i = 0; i < fs.length; i++) {
                f = fs[i];
                int mod = f.getModifiers();
                if ((mod & STATIC) != 0) { //静态字段不管
                    continue;
                }
                String name = f.getName();
                if ((mod & PUBLIC) != 0) { //public字段,直接取值
                    Object v = f.get(model);
                    if (v != null) {
                        map.put(name, v);
                        if (f.getType().equals(File.class)) { //文件类型
                            hasFile = true;
                        }
                    }
                } else { //通过getter方法取值
                    Method m = model.getClass().getMethod("get" + name.substring(0, 1).toUpperCase() + (name.length() == 1 ? "" : name.substring(1)));
                    if (m != null) {
                        Object v = m.invoke(mod);
                        if (v != null) {
                            map.put(name, v);
                            if (f.getType().equals(File.class)) { //文件类型
                                hasFile = true;
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (!hasFile) {
            return bodyJson(map);
        }
        for (Map.Entry<String, Object> item : map.entrySet()) {
            if (item.getValue() instanceof File) {
                addPart(item.getKey(), (File) item.getValue());
            } else {
                addBodyQuery(item.getKey(), String.valueOf(item.getValue()));
            }
        }
        return this;
    }

    /**
     * 如果当前是multiPart类型,则这些Query数据会是multiPart的一部分
     * 如果当前还没有指定类型,且queryMap不为空则数据类型时{@link WeMediaType#FORM}
     *
     * @param name
     * @param value
     * @return
     */
    public BodyReq<T> addBodyQuery(String name, String value) {
        queryMap.put(name, value);
        return this;
    }

    /**
     * @param map
     * @return
     * @see #addBodyQuery(String, String)
     */
    public BodyReq<T> addBodyQuery(Map<String, String> map) {
        queryMap.putAll(map);
        return this;
    }

    /**
     * 传送单独的文件,如图片文件和其他文件
     * 这里会根据后缀名判断是图片(image/*)类型还是其他类型(application/octet-stream)
     *
     * @param file
     * @return
     */
    public BodyReq<T> bodyFile(File file) {
        MediaType mediaType = getMediaType(file);
        return bodyFile(file, mediaType);
    }

    public BodyReq<T> bodyFile(File file, MediaType type) {
        if (file == null) {
            throw new IllegalArgumentException("file cannot be null.");
        }
        if (type == null) {
            return bodyFile(file);
        } else {
            this.contentType = type;
            this.singleFile = file;
            return this;
        }
    }

    /**
     * 设置body内容为文本
     *
     * @param text
     * @return
     */
    public BodyReq<T> bodyText(String text) {
        this.contentType = WeMediaType.PLAIN;
        body = RequestBody.create(WeMediaType.PLAIN, text);
        return this;
    }

    public BodyReq<T> bodyJson(String json) {
        this.contentType = WeMediaType.JSON;
        body = RequestBody.create(WeMediaType.JSON, json);
        return this;
    }

    public BodyReq<T> bodyJson(Map<String, Object> json) {
        JSONObject object = new JSONObject();
        if (json != null || json.size() > 0) {
            for (Map.Entry<String, Object> item : json.entrySet()) {
                try {
                    object.put(item.getKey(), item.getValue());
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }
        return bodyJson(object.toString());
    }

    public BodyReq<T> bodyJson(JSONObject object) {
        if (object == null) {
            throw new IllegalArgumentException("object 不能为null");
        }
        return bodyJson(object.toString());
    }

    public BodyReq<T> bodyJson(JSONArray array) {
        if (array == null) {
            throw new IllegalArgumentException("array 不能为null");
        }
        return bodyJson(array.toString());
    }

    /**
     * 将一个对象转换为JSON字符串,依赖于{@link TypeAdapter}的实现
     *
     * @param t
     * @return
     */
    public BodyReq<T> bodyJson(Object t) {
        if (t == null) {
            return bodyJson("");
        }
        TypeAdapter adapter = weHttp.config().adapter();
        if (adapter == null) {
            return body(t);
        }
        return bodyJson(adapter.to(t));
    }


    private MediaType getMediaType(File file) {
        if (file == null) {
            throw new IllegalArgumentException("file 不能为null");
        }
        MediaType mediaType;
        String name = file.getName();
        if (name.endsWith(".png")) {
            mediaType = WeMediaType.PNG;
        } else if (name.endsWith(".jpg") || name.endsWith(".jpeg")) {
            mediaType = WeMediaType.JPG;
        } else if (name.endsWith(".gif")) {
            mediaType = WeMediaType.GIF;
        } else { //二进制类型
            mediaType = WeMediaType.OCTET;
        }
        return mediaType;
    }

    private String getUrlEncodedQuery(Map<String, String> queryMap) {
        if (queryMap == null || queryMap.size() == 0) {
            return "";
        }
        final StringBuilder result = new StringBuilder();
        for (Map.Entry<String, String> entry : queryMap.entrySet()) {
            result.append(entry.getKey());
            result.append("=");
            try {
                result.append(URLEncoder.encode(entry.getValue(), "utf-8"));
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
            result.append('&');
        }
        String s = result.toString();
        if (s.endsWith("&")) {
            s = s.substring(0, s.length() - 1);
        }
        return s;
    }


    private boolean isMultiPart() {
        return contentType == WeMediaType.MULTIPART;
    }

}
