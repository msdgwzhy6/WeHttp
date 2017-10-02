package me.leoray.wehttp;

import me.leoray.wejson.WeJson;

public class WeTypeAdapter implements TypeAdapter {
    private WeJson weJson = new WeJson();

    @Override
    public <T> T from(String s, Class<T> classOfT) {
        return weJson.fromJson(s, classOfT);
    }

    @Override
    public <T> String to(T t) {
        return weJson.toJson(t);
    }
}
