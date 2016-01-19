package com.willblaschko.android.alexa;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import com.amazon.identity.auth.device.AuthError;
import com.amazon.identity.auth.device.authorization.api.AmazonAuthorizationManager;
import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.google.gson.Gson;
import com.willblaschko.android.alexa.volley.VolleySingleton;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by wblaschko on 8/11/15.
 */
public class TokenManager {

    private final static String TAG = "TokenManager";

    private static String REFRESH_TOKEN;
    private static String ACCESS_TOKEN;

    private final static String ARG_GRANT_TYPE = "grant_type";
    private final static String ARG_CODE = "code";
    private final static String ARG_REDIRECT_URI = "redirect_uri";
    private final static String ARG_CLIENT_ID = "client_id";
    private final static String ARG_CODE_VERIFIER = "code_verifier";
    private final static String ARG_REFRESH_TOKEN = "refresh_token";


    public final static String PREF_ACCESS_TOKEN = "access_token";
    public final static String PREF_REFRESH_TOKEN = "refresh_token";
    public final static String PREF_TOKEN_EXPIRES = "token_expires";

    public static void getAccessToken(final Context context, @NotNull String authCode, @NotNull String codeVerifier, AmazonAuthorizationManager authorizationManager, @Nullable final TokenResponseCallback callback){
        String url = "https://api.amazon.com/auth/O2/token";

        final Map<String, String> arguments = new HashMap<>();
        arguments.put(ARG_GRANT_TYPE, "authorization_code");
        arguments.put(ARG_CODE, authCode);
        try {
            arguments.put(ARG_REDIRECT_URI, authorizationManager.getRedirectUri());
            arguments.put(ARG_CLIENT_ID, authorizationManager.getClientId());
        } catch (AuthError authError) {
            authError.printStackTrace();
        }
        arguments.put(ARG_CODE_VERIFIER, codeVerifier);

        StringRequest request = new StringRequest(Request.Method.POST, url, new Response.Listener<String>() {
            @Override
            public void onResponse(String s) {
                if(BuildConfig.DEBUG) {
                    Log.i(TAG, s);
                }
                TokenResponse tokenResponse = new Gson().fromJson(s, TokenResponse.class);
                saveTokens(context, tokenResponse);

                if(callback != null){
                    callback.onSuccess(tokenResponse);
                }

            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError volleyError) {
                volleyError.printStackTrace();
                if(callback != null){
                    callback.onFailure(volleyError);
                }
            }
        }) {

            @Override
            protected Map<String, String> getParams() throws AuthFailureError {
                return arguments;
            }
        };


        VolleySingleton.getInstance(context).addToRequestQueue(request);
    }

//    public static void getRefreshToken(@NotNull Context context, @NotNull TokenCallback callback) {
//        //TODO make this async
//        callback.onSuccess(REFRESH_TOKEN);
//    }

    public static void getAccessToken(@NotNull AmazonAuthorizationManager authorizationManager, @NotNull Context context, @NotNull TokenCallback callback) {
        SharedPreferences preferences = Util.getPreferences(context);
        if(preferences.contains(PREF_ACCESS_TOKEN)){
            if(preferences.getLong(PREF_TOKEN_EXPIRES, 0) > System.currentTimeMillis()){
                callback.onSuccess(preferences.getString(PREF_ACCESS_TOKEN, null));
                return;
            }else{
                if(preferences.contains(PREF_REFRESH_TOKEN)){
                    getRefreshToken(authorizationManager, context, callback, preferences.getString(PREF_REFRESH_TOKEN, ""));
                    return;
                }
            }
        }

        callback.onFailure(new IllegalStateException("User is not logged in and no refresh token found."));
    }

    private static void getRefreshToken(@NotNull AmazonAuthorizationManager authorizationManager, @NotNull final Context context, @NotNull final TokenCallback callback, String refreshToken){

        String url = "https://api.amazon.com/auth/O2/token";
        final Map<String, String> arguments = new HashMap<>();
        arguments.put(ARG_GRANT_TYPE, "refresh_token");
        arguments.put(ARG_REFRESH_TOKEN, refreshToken);
        try {
            arguments.put(ARG_CLIENT_ID, authorizationManager.getClientId());
        } catch (AuthError authError) {
            authError.printStackTrace();
            callback.onFailure(authError);
        }

        StringRequest request = new StringRequest(Request.Method.POST, url, new Response.Listener<String>() {
            @Override
            public void onResponse(String s) {
                if(BuildConfig.DEBUG) {
                    Log.i(TAG, s);
                }
                TokenResponse tokenResponse = new Gson().fromJson(s, TokenResponse.class);
                saveTokens(context, tokenResponse);

                callback.onSuccess(tokenResponse.access_token);


            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError volleyError) {
                volleyError.printStackTrace();
                callback.onFailure(volleyError);

            }
        }) {

            @Override
            protected Map<String, String> getParams() throws AuthFailureError {
                return arguments;
            }
        };


        VolleySingleton.getInstance(context).addToRequestQueue(request);
    }

    private static void saveTokens(Context context, TokenResponse tokenResponse){
        REFRESH_TOKEN = tokenResponse.refresh_token;
        ACCESS_TOKEN = tokenResponse.access_token;

        SharedPreferences.Editor preferences = Util.getPreferences(context).edit();
        preferences.putString(PREF_ACCESS_TOKEN, ACCESS_TOKEN);
        preferences.putString(PREF_REFRESH_TOKEN, REFRESH_TOKEN);
        preferences.putLong(PREF_TOKEN_EXPIRES, System.currentTimeMillis() + tokenResponse.expires_in);
        preferences.apply();
    }

    public interface TokenResponseCallback {
        void onSuccess(TokenResponse response);
        void onFailure(VolleyError error);
    }
    public static class TokenResponse{
        public String access_token;
        public String refresh_token;
        public String token_type;
        public long expires_in;
    }

    public static interface TokenCallback{
        void onSuccess(String token);
        void onFailure(Throwable e);
    }
}
