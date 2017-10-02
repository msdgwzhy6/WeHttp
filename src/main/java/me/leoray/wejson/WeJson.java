package me.leoray.wejson;

import org.json.JSONArray;
import org.json.JSONObject;

import java.lang.reflect.*;
import java.util.*;

import static java.lang.reflect.Modifier.PUBLIC;
import static java.lang.reflect.Modifier.STATIC;

/**
 * 一个简易的JSON转换工具,基于Android内置的{@link JSONObject}和{@link JSONArray}
 */
public class WeJson {

    private static final String EMPTY_MAP = "{}";
    private static final String EMPTY_ARR = "[]";

    public <T> String toJson(T t) {
        return toJson(t, 0);
    }

    public <T> String toJson(T t, int level) {
        if (t == null) {
            return null;
        }
        StringBuilder builder = new StringBuilder();
        process(builder, t);
        return builder.toString();
    }

    private <T> void process(StringBuilder builder, T t) {
        if (t.getClass().isPrimitive()) { //原生
            builder.append(t);
        } else if (t instanceof String) { //String
            builder.append('"').append(getValidStr((String) t)).append('"');
        } else if (isPrimitivePackageType(t)) { //原生类型的封装类型
            builder.append(t);
        } else if (t.getClass().isArray()) { //数组
            processArr(builder, (Object[]) t);
        } else if (t instanceof Iterable) { //集合
            processIterable(builder, (Iterable) t);
        } else if (t instanceof Map) { //处理Map
            processMap(builder, (Map) t);
        } else {
            processObj(builder, t); //处理普通POJO对象
        }
    }

    /**
     * 字符串处理特殊字符
     *
     * @param t
     * @return
     */
    private String getValidStr(String t) {
        return t.replace("\"", "\\\"")
                .replace("\\", "\\\\")
                .replace("\b", "\\b")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\f", "\\f")
                .replace("\t", "\\t");
    }

    private <T> boolean isPrimitivePackageType(T t) {
        return t instanceof Integer
                || t instanceof Short
                || t instanceof Long
                || t instanceof Byte
                || t instanceof Boolean
                || t instanceof Float
                || t instanceof Double
                || t instanceof Character;
    }

    private void processMap(StringBuilder builder, Map<String, Object> t) {
        if (t.size() == 0) {
            builder.append(EMPTY_MAP);
            return;
        }
        builder.append('{');
        int index = 0;
        int size = t.size();
        for (Map.Entry<String, Object> o : t.entrySet()) {
            Object value = o.getValue();
            String key = o.getKey();
            if (key == null || key.equals("") || value == null) { //无效的键
                continue;
            }
            if (value instanceof String && o.equals("")) {
                continue;
            }
            builder.append('"').append(key).append('"');
            builder.append(':');
            process(builder, value);
            if (index < size - 1) {
                builder.append(',');
            }
            index++;
        }
        builder.append('}');
    }

    private <T> void processObj(StringBuilder builder, T model) {
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
            builder.append(EMPTY_MAP);
            return;
        }
        Map<String, Object> map = new HashMap<>();
        Field f;
        try {
            for (int i = 0; i < fs.length; i++) {
                f = fs[i];
                int mod = f.getModifiers();
                if ((mod & STATIC) != 0) { //静态字段不管
                    continue;
                }
                String name = f.getName();
                if (name.contains("$")) {
                    continue;
                }
                if ((mod & PUBLIC) != 0) { //public字段,直接取值
                    Object v = f.get(model);
                    if (v != null) {
                        map.put(name, v);

                    }
                } else { //通过getter方法取值
                    Method m = model.getClass().getMethod("get" + name.substring(0, 1).toUpperCase() + (name.length() == 1 ? "" : name.substring(1)));
                    if (m != null) {
                        Object v = m.invoke(model);
                        if (v != null) {
                            map.put(name, v);
                        }
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        builder.append('{');
        int index = 0;
        int count = map.size();
        for (Map.Entry<String, Object> item : map.entrySet()) {
            index++;
            String key = item.getKey();
            Object value = item.getValue();
            if (value instanceof String && value.equals("")) {
                continue;
            }
            builder.append('"').append(key).append('"').append(':');
            process(builder, value);
            if (index < count) {
                builder.append(',');
            }
        }
        builder.append('}');
    }

    private void processIterable(StringBuilder builder, Iterable t) {
        Iterator iterator = t.iterator();
        builder.append('[');
        int count = 0;
        while (iterator.hasNext()) {
            count++;
            Object next = iterator.next();
            process(builder, next);
            builder.append(',');
        }
        if (count > 0) {
            builder.deleteCharAt(builder.length() - 1);
        }
        builder.append(']');
    }

    private <T> void processArr(StringBuilder builder, Object[] t) {
        if (t.length == 0) {
            builder.append(EMPTY_ARR);
            return;
        }
        builder.append('[');
        for (int i = 0; i < t.length; i++) {
            process(builder, t[i]);
            if (i < t.length - 1) {
                builder.append(',');
            }
        }
        builder.append(']');
    }

    public <T> T fromJson(String json, Class<T> classOfT) {
        if (json == null) {
            return null;
        }
        if (classOfT == null) {
            throw new RuntimeException("必须指定classOfT");
        }
        String j = json.trim();
        if (j.startsWith("[")) { //数组
            try {
                JSONArray arr = new JSONArray(j);
                T x = fromJsonArr(arr, classOfT, null);
                if (x != null) return x;
            } catch (Exception e) {
                throw new RuntimeException("json 解析错误" + e.getMessage());
            }

        } else if (j.startsWith("{")) {
            try {
                JSONObject obj = new JSONObject(j);
                return fromJsonObj(obj, classOfT, null);
            } catch (Exception e) {
                throw new RuntimeException("json 解析错误:" + e.getMessage());
            }

        }
        throw new RuntimeException("classOfT 指定错误");
    }

    private <T> T fromJsonArr(JSONArray arr, Class<T> classOfT, Class<?> itemType) throws Exception {
        if (classOfT.equals(List.class)) { //集合
            return (T) fromList(arr, (Class<List>) classOfT, itemType);
        } else if (classOfT.isArray()) { //数组
            return fromArr(arr, classOfT);
        } else {
            throw new UnsupportedOperationException("json 解析错误:不支持的类型:" + classOfT.getName());
        }
    }

    private <T> T fromJsonObj(JSONObject obj, Class<T> classOfT, Class<?> itemType) throws Exception {
        if (classOfT.equals(Map.class)) { //map
            return (T) fromMap(obj, (Class<Map>) classOfT, itemType);
        } else { //pojo
            return fromPojo(obj, classOfT);
        }
    }

    private List fromList(JSONArray arr, Class<List> classOfT, Class<?> itemType) throws Exception {
        if (itemType == null) {
            throw new WeJsonException("无法确定列表项的类型");
        }
        List list;
        if (classOfT.getName().equals("java.util.List")) {
            list = new ArrayList();
        } else {
            list = classOfT.newInstance();
        }
        for (int i = 0; i < arr.length(); i++) {
            Object o = arr.get(i);
            if (o instanceof JSONArray) {
                list.add(fromJsonArr((JSONArray) o, classOfT, itemType));
            } else if (o instanceof JSONObject) {
                list.add(fromJsonObj((JSONObject) o, classOfT, itemType));
            } else {
                list.add(o);
            }
        }
        return list;

    }

    private Map fromMap(JSONObject obj, Class<Map> classOfT, Class<?> entryType) throws Exception {
        if (entryType == null) {
            throw new WeJsonException("无法确定列表项的类型");
        }
        Map<String, Object> map;
        if (classOfT.getName().equals("java.util.Map")) {
            map = new HashMap<>();
        } else {
            map = classOfT.newInstance();
        }
        Iterator keys = obj.keys();
        while (keys.hasNext()) {
            String name = (String) keys.next();
            Object o = obj.get(name);
            if (o != null) {
                map.put(name, fromJsonData(o, classOfT, entryType));
            } else {
                map.put(name, null);
            }
        }
        return map;
    }

    private Object fromJsonData(Object o, Class<?> classOfT, Class<?> itemType) throws Exception {
        if (o instanceof JSONArray) {
            return fromJsonArr((JSONArray) o, classOfT, itemType);
        } else if (o instanceof JSONObject) {
            return fromJsonObj((JSONObject) o, classOfT, itemType);
        } else {
            return o;
        }
    }

    private <T> T fromArr(JSONArray arr, Class<T> classOfT) throws Exception {
        Class<?> componentType = classOfT.getComponentType();//数组元素类型
        Object array = Array.newInstance(componentType, arr.length());
        for (int i = 0; i < arr.length(); i++) {
            Object o = arr.get(i);
            Array.set(array, i, fromJsonData(o, componentType, componentType));
        }
        return (T) arr;
    }

    private <T> T fromPojo(JSONObject obj, Class<T> classOfT) throws Exception {
        Field[] declaredFields = classOfT.getDeclaredFields(); //当前类字段
        Field[] superFields = classOfT.getSuperclass().getDeclaredFields(); //父类字段
        Field[] fs = new Field[declaredFields.length + superFields.length];
        for (int i = 0; i < declaredFields.length; i++) {
            fs[i] = declaredFields[i];
        }
        for (int i = declaredFields.length; i < fs.length; i++) {
            fs[i] = superFields[i - declaredFields.length];
        }
        if (fs == null || fs.length == 0) {
            return null;
        }
        Object r = classOfT.newInstance();
        Method m;
        for (int i = 0; i < fs.length; i++) {
            Field f = fs[i];
            String fn = f.getName();
            if (fn.contains("$")) { //如this$0,$change等特殊变量
                continue;
            }
            int modifiers = f.getModifiers();


            Object v = obj.opt(fn);

            if (v == null) {
                continue;
            }
            Object o = fromJsonData(v, getMemberType(classOfT, f), getSubType(classOfT, f, v));
            if (JSONObject.NULL.equals(o)) {
                o = null;
            }
            if ((modifiers & Modifier.PUBLIC) != 0) { //public,直接赋值
                f.set(r, o);
            } else { //set方法
                String mn = "set" + fn.substring(0, 1).toUpperCase() + (fn.length() == 1 ? "" : fn.substring(1)); //set方法名
                Class<?> ft = f.getType();
                try {
                    m = classOfT.getMethod(mn, ft);
                } catch (NoSuchMethodException e) {
                    e.printStackTrace();
                    continue;
                }
                m.invoke(r, o);
            }
        }
        return (T) r;
    }

    private <T> Class<?> getMemberType(Class<T> classOfT, Field f) throws WeJsonException {
        if (f.getGenericType() instanceof TypeVariable) { //泛型类型
            TypeVariable<Class<T>>[] parameters = classOfT.getTypeParameters();
            Type superclass = classOfT.getGenericSuperclass();
            if (superclass instanceof ParameterizedType) {
                Type type = ((ParameterizedType) superclass).getActualTypeArguments()[0]; //泛型实际对象
                if (type instanceof Class) {
                    return (Class<?>) type;
                } else {
                    throw new WeJsonException("不支持嵌套泛型");
                }
            } else {
                throw new WeJsonException("缺少泛型信息:" + classOfT);
            }
        } else {
            return f.getType();
        }
    }

    /**
     * 获取成员变量如果是Map,List这些他们包含元素的类型
     *
     * @param classOfT 当前类型
     * @param f        成员变量
     * @param v        值
     * @param <T>
     * @return
     */
    private <T> Class<?> getSubType(Class<T> classOfT, Field f, Object v) throws Exception {
        if (f.getGenericType() instanceof TypeVariable) { //泛型类型
            TypeVariable<Class<T>>[] parameters = classOfT.getTypeParameters();
            Type superclass = classOfT.getGenericSuperclass();
            if (superclass instanceof ParameterizedType) {
                Type type = ((ParameterizedType) superclass).getActualTypeArguments()[0]; //泛型实际对象
                if (type instanceof Class) {
                    return (Class<?>) type;
                } else {
                    throw new WeJsonException("不支持嵌套泛型");
                }
            } else {
                throw new WeJsonException("缺少泛型信息:" + classOfT);
            }
        } else if (f.getType().equals(List.class)) { //参数化类型
            Type genericType = f.getGenericType();
            if (genericType instanceof ParameterizedType) {
                Type type = ((ParameterizedType) genericType).getActualTypeArguments()[0];
                if (type instanceof Class) {
                    return (Class<?>) type;
                } else {
                    throw new WeJsonException("不支持嵌套泛型:" + f.getName());
                }
            } else {
                throw new WeJsonException("缺少泛型类型声明:" + f.getName());
            }
        } else if (f.getType().equals(Map.class)) {
            Type genericType = f.getGenericType();
            if (genericType instanceof ParameterizedType) {
                Type type = ((ParameterizedType) genericType).getActualTypeArguments()[1];
                if (type instanceof Class) {
                    return (Class<?>) type;
                } else {
                    throw new WeJsonException("不支持嵌套泛型:" + f.getName());
                }
            } else {
                throw new WeJsonException("缺少泛型类型声明:" + f.getName());
            }
        } else { //其他类型
            return f.getType();
        }
    }

}
