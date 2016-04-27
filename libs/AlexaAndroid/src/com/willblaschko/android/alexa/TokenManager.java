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
 * A utility class designed to request, receive, store, and renew Amazon authentication tokens using a Volley interface and the Amazon auth API
 *
 * Some more details here: https://developer.amazon.com/public/solutions/alexa/alexa-voice-service/docs/authorizing-your-alexa-enabled-product-from-a-website
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

    /**
     * Get an access token from the Amazon servers for the current user
     * @param context local/application level context
     * @param authCode the authorization code supplied by the Authorization Manager
     * @param codeVerifier a randomly generated verifier, must be the same every time
     * @param authorizationManager the AuthorizationManager class calling this function
     * @param callback the callback for state changes
     */
    public static void getAccessToken(final Context context, @NotNull String authCode, @NotNull String codeVerifier, AmazonAuthorizationManager authorizationManager, @Nullable final TokenResponseCallback callback){
        //this url shouldn't be hardcoded, but it is, it's the Amazon auth access token endpoint
        String url = "https://api.amazon.com/auth/O2/token";

        //set up our arguments for the api call, these will be the call headers
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

        //send a new Volley string request and parse the response
        StringRequest request = new StringRequest(Request.Method.POST, url, new Response.Listener<String>() {
            @Override
            public void onResponse(String s) {
                if(BuildConfig.DEBUG) {
                    Log.i(TAG, s);
                }
                TokenResponse tokenResponse = new Gson().fromJson(s, TokenResponse.class);
                //save our tokens to local shared preferences
                saveTokens(context, tokenResponse);

                if(callback != null){
                    //bubble up success
                    callback.onSuccess(tokenResponse);
                }

            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError volleyError) {
                volleyError.printStackTrace();
                if(callback != null){
                    //bubble up error
                    callback.onFailure(volleyError);
                }
            }
        }) {

            @Override
            protected Map<String, String> getParams() throws AuthFailureError {
                //set the headers of the StringRequest to the arguments variable we defined above
                return arguments;
            }
        };

        //add the request to our queue
        VolleySingleton.getInstance(context).addToRequestQueue(request);
    }

    /**
     * Check if we have a pre-existing access token, and whether that token is expired. If it is not, return that token, otherwise get a refresh token and then
     * use that to get a new token.
     * @param authorizationManager our AuthManager
     * @param context local/application context
     * @param callback the TokenCallback where we return our tokens when successful
     */
    public static void getAccessToken(@NotNull AmazonAuthorizationManager authorizationManager, @NotNull Context context, @NotNull TokenCallback callback) {
        SharedPreferences preferences = Util.getPreferences(context.getApplicationContext());
        //if we have an access token
        if(preferences.contains(PREF_ACCESS_TOKEN)){

            if(preferences.getLong(PREF_TOKEN_EXPIRES, 0) > System.currentTimeMillis()){
                //if it's not expired, return the existing token
                callback.onSuccess(preferences.getString(PREF_ACCESS_TOKEN, null));
                return;
            }else{
                //if it is expired but we have a refresh token, get a new token
                if(preferences.contains(PREF_REFRESH_TOKEN)){
                    getRefreshToken(authorizationManager, context, callback, preferences.getString(PREF_REFRESH_TOKEN, ""));
                    return;
                }
            }
        }

        //uh oh, the user isn't logged in, we have an IllegalStateException going on!
        callback.onFailure(new IllegalStateException("User is not logged in and no refresh token found."));
    }

    /**
     * Get a new refresh token from the Amazon server to replace the expired access token that we currently have
     * @param authorizationManager
     * @param context
     * @param callback
     * @param refreshToken the refresh token we have stored in local cache (sharedPreferences)
     */
    private static void getRefreshToken(@NotNull AmazonAuthorizationManager authorizationManager, @NotNull final Context context, @NotNull final TokenCallback callback, String refreshToken){
        //this url shouldn't be hardcoded, but it is, it's the Amazon auth access token endpoint
        String url = "https://api.amazon.com/auth/O2/token";

        //set up our arguments for the api call, these will be the call headers
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
                //get our tokens back
                TokenResponse tokenResponse = new Gson().fromJson(s, TokenResponse.class);
                //save our tokens
                saveTokens(context, tokenResponse);
                //we have new tokens!
                callback.onSuccess(tokenResponse.access_token);
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError volleyError) {
                volleyError.printStackTrace();
                //bubble up error
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

    /**
     * Save our new tokens in SharePreferences so we can access them at a later point
     * @param context
     * @param tokenResponse
     */
    private static void saveTokens(Context context, TokenResponse tokenResponse){
        REFRESH_TOKEN = tokenResponse.refresh_token;
        ACCESS_TOKEN = tokenResponse.access_token;

        SharedPreferences.Editor preferences = Util.getPreferences(context.getApplicationContext()).edit();
        preferences.putString(PREF_ACCESS_TOKEN, ACCESS_TOKEN);
        preferences.putString(PREF_REFRESH_TOKEN, REFRESH_TOKEN);
        //comes back in seconds, needs to be milis
        preferences.putLong(PREF_TOKEN_EXPIRES, (System.currentTimeMillis() + tokenResponse.expires_in * 1000));
        preferences.commit();
    }

    public interface TokenResponseCallback {
        void onSuccess(TokenResponse response);
        void onFailure(VolleyError error);
    }

    //for JSON parsing of our token responses
    public static class TokenResponse{
        public String access_token;
        public String refresh_token;
        public String token_type;
        public long expires_in;
    }

    public interface TokenCallback{
        void onSuccess(String token);
        void onFailure(Throwable e);
    }
}
