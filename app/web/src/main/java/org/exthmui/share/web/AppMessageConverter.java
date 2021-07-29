package org.exthmui.share.web;

import com.google.gson.Gson;
import com.yanzhenjie.andserver.annotation.Converter;
import com.yanzhenjie.andserver.framework.MessageConverter;
import com.yanzhenjie.andserver.framework.body.JsonBody;
import com.yanzhenjie.andserver.http.ResponseBody;
import com.yanzhenjie.andserver.util.IOUtils;
import com.yanzhenjie.andserver.util.MediaType;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Type;
import java.nio.charset.Charset;

@Converter
public class AppMessageConverter implements MessageConverter {
    final static Gson gson = new Gson();

    @Override
    public ResponseBody convert(Object output, MediaType mediaType) {
        String data = gson.toJson(output);
        ResponseBody returned = new JsonBody(data);
//        returned.setSuccess(true);
//        returned.setData(data);
        return returned;
    }

    @Override
    public <T> T convert(InputStream stream, MediaType mediaType, Type type) throws IOException {
        Charset charset = mediaType == null ? null : mediaType.getCharset();
        if (charset == null) {
            return gson.fromJson(IOUtils.toString(stream), type);
        }
        return gson.fromJson(IOUtils.toString(stream, charset), type);
    }
}