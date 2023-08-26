//package org.exthmui.share.web.body;
//
//import com.google.gson.Gson;
//import com.yanzhenjie.andserver.framework.body.JsonBody;
//
//public class Result<T> {
//    public int code;
//    public boolean success;
//    public T data;
//
//    public Result(T data) {
//        this.data = data;
//    }
//
//    public boolean isSuccess() {
//        return success;
//    }
//
//    public void setSuccess(boolean success) {
//        this.success = success;
//    }
//
//    public T getData() {
//        return data;
//    }
//
//    public void setData(T data) {
//        this.data = data;
//    }
//
//    public String toJson() {
//        return new Gson().toJson(this);
//    }
//
//    public JsonBody toBody() {
//        return new JsonBody(this.toJson());
//    }
//}
