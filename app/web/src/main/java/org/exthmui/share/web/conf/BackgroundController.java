//package org.exthmui.share.web.conf;
//
//import android.app.AlertDialog;
//import android.app.Dialog;
//import android.content.Context;
//import android.view.View;
//
//import androidx.annotation.NonNull;
//
//import com.google.gson.annotations.SerializedName;
//import com.yanzhenjie.andserver.annotation.GetMapping;
//import com.yanzhenjie.andserver.annotation.PostMapping;
//import com.yanzhenjie.andserver.annotation.RequestBody;
//import com.yanzhenjie.andserver.annotation.RequestMapping;
//import com.yanzhenjie.andserver.annotation.RequestParam;
//import com.yanzhenjie.andserver.annotation.RestController;
//
//import org.exthmui.share.shared.misc.Constants;
//import org.exthmui.share.shared.misc.Utils;
//import org.exthmui.share.web.R;
//import org.exthmui.share.web.exceptions.ApiException;
//import org.exthmui.share.web.exceptions.InvalidRequestException;
//
//@RestController
//@RequestMapping(path = "/media")
//public class BackgroundController {
//
//    static class AuthPBody{
//        @SerializedName("display_name")
//        String displayName;
//        @SerializedName("device_id")
//        String deviceId;
//        @SerializedName("device_type")
//        int deviceType;
//        @SerializedName("auth_code")
//        String authCode;
//    }
//    @NonNull
//    @PostMapping("/auth")
//    String auth(@NonNull @RequestBody AuthPBody data) {
//        String pattern = "^[0-9]{6}$";
//        if (data.deviceId.isEmpty() || data.authCode.isEmpty() || !data.authCode.matches(pattern))
//            throw new InvalidRequestException();
//        if (data.displayName.isEmpty()) data.displayName = "Unknown device";
//        if(!Utils.isInclude(Constants.DeviceType.class, data.deviceType)) data.deviceType = Constants.DeviceType.UNKNOWN.getNumVal();
////        Dialog dialog = generateCodeDialog(data.authCode, ctx);
////        dialog.show();
//        return "true";
//    }
//    @NonNull
//    @GetMapping("/auth")
//    String login(@RequestParam("password") String password) {
//        throw new ApiException(password);
////        Dialog dialog = generateCodeDialog("00000d");
////        dialog.show();
////        return "true";
//    }
//
//    private Dialog generateCodeDialog(String code, Context ctx) {
//        View view = View.inflate(ctx, R.layout.code_auth_dialog, null);
//        // 初始Dialog 里面的内容
//        AlertDialog.Builder builder = new AlertDialog.Builder(ctx);
//        builder.setTitle(R.string.code_title).setView(view);
//        Dialog dialog = builder.create();
//        return dialog;
//    }
//}
