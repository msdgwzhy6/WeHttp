package me.leoray.wehttp;

import okhttp3.MediaType;

public class WeMediaType {

    public static final MediaType PNG = MediaType.parse("image/png");
    public static final MediaType JPG = MediaType.parse("image/jpg");
    public static final MediaType GIF = MediaType.parse("image/gif");
    public static final MediaType PLAIN = MediaType.parse("text/plain");
    public static final MediaType HTML = MediaType.parse("text/html");
    public static final MediaType XML = MediaType.parse("text/xml");
    public static final MediaType JSON = MediaType.parse("application/json");
    public static final MediaType FORM = MediaType.parse("application/x-www-form-urlencoded");
    public static final MediaType MULTIPART = MediaType.parse("multipart/form-data");
    public static final MediaType OCTET = MediaType.parse("application/octet-stream");

}
