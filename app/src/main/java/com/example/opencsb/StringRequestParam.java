package com.example.opencsb;

import androidx.annotation.Nullable;
import com.android.volley.AuthFailureError;
import com.android.volley.NetworkResponse;
import com.android.volley.Response;
import com.android.volley.toolbox.HttpHeaderParser;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.Response.ErrorListener;
import com.android.volley.Response.Listener;

import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Map;

public class StringRequestParam extends StringRequest {

    private Map<String, String> params;

    public StringRequestParam(
            int method,
            String url,
            Listener<String> listener,
            @Nullable ErrorListener errorListener,
            Map<String, String> params) {
        super(method, url, listener, errorListener);
        this.params = params;
    }


    @Override
    protected Map<String, String> getParams() throws AuthFailureError {
        return params;
    }
/*
    @Override
    @SuppressWarnings("DefaultCharset")
    protected Response<String> parseNetworkResponse(NetworkResponse response) {
        String parsed;
        try {
            parsed = new String(response.data, HttpHeaderParser.parseCharset(response.headers));
        } catch (UnsupportedEncodingException e) {
            // Since minSdkVersion = 8, we can't call
            // new String(response.data, Charset.defaultCharset())
            // So suppress the warning instead.
            parsed = new String(response.data);
        }
        String headers = response.headers.toString();
        return Response.success(headers + parsed, HttpHeaderParser.parseCacheHeaders(response));
    }
*/
}
