package org.exthmui.share.web;

import androidx.annotation.NonNull;

import com.yanzhenjie.andserver.annotation.Resolver;
import com.yanzhenjie.andserver.error.HttpException;
import com.yanzhenjie.andserver.framework.ExceptionResolver;
import com.yanzhenjie.andserver.http.HttpRequest;
import com.yanzhenjie.andserver.http.HttpResponse;
import com.yanzhenjie.andserver.http.StatusCode;

import org.exthmui.share.web.body.Error;
import org.exthmui.share.web.exceptions.AbstractApiException;
import org.exthmui.share.web.exceptions.ApiException;

@Resolver
public class AppExceptionResolver implements ExceptionResolver {
    @Override
    public void onResolve(@NonNull HttpRequest httpRequest, @NonNull HttpResponse httpResponse, @NonNull Throwable throwable) {
        AbstractApiException e;
        if (throwable instanceof AbstractApiException) {
            e = (AbstractApiException) throwable;
        } else {
            e = new ApiException(StatusCode.SC_INTERNAL_SERVER_ERROR, throwable);
            e.setDescription(throwable.getMessage());
            if (throwable instanceof HttpException) {
                HttpException ex = (HttpException) throwable;
                e.setStatusCode(ex.getStatusCode());
            }
        }

        Error returned = new Error(e, httpRequest);

        if (!BuildConfig.DEBUG) returned.getError().setStackTrace(null);

        httpResponse.setBody(returned.toBody());
    }
}
