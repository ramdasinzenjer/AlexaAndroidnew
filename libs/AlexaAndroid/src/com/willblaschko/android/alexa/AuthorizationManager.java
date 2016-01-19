package com.willblaschko.android.alexa;

import android.content.Context;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Base64;
import android.util.Log;

import com.amazon.identity.auth.device.AuthError;
import com.amazon.identity.auth.device.authorization.BuildConfig;
import com.amazon.identity.auth.device.authorization.api.AmazonAuthorizationManager;
import com.amazon.identity.auth.device.authorization.api.AuthorizationListener;
import com.amazon.identity.auth.device.authorization.api.AuthzConstants;
import com.android.volley.VolleyError;
import com.willblaschko.android.alexa.callbacks.AsyncCallback;
import com.willblaschko.android.alexa.callbacks.AuthorizationCallback;

import org.jetbrains.annotations.NotNull;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Random;

/**
 * Created by wblaschko on 8/13/15.
 */
public class AuthorizationManager {

    private final static String TAG = "AuthorizationHandler";

    private Context mContext;
    private String mProductId;
    private AmazonAuthorizationManager mAuthManager;
    private static final String[] APP_SCOPES= {"alexa:all"};
    private AuthorizationCallback mCallback;


    private static final String CODE_VERIFIER = "code_verifier";

    public AuthorizationManager(@NotNull Context context, @NotNull String productId){
        mContext = context;
        mProductId = productId;

        try {
            mAuthManager = new AmazonAuthorizationManager(mContext, Bundle.EMPTY);
        }catch(IllegalArgumentException e){
            //This error will be thrown if the main project doesn't have the assets/api_key.txt file in it--this contains the security credentials from Amazon
            Util.showAuthToast(mContext, "APIKey is incorrect or does not exist.");
            Log.e(TAG, "Unable to Use Amazon Authorization Manager. APIKey is incorrect or does not exist. Does assets/api_key.txt exist in the main application?", e);
        }
    }


    //todo eventually check to see whether we have a saved refresh key and try that first
    public void checkLoggedIn(Context context, final AsyncCallback<Boolean, Throwable> callback){
        TokenManager.getAccessToken(mAuthManager, context, new TokenManager.TokenCallback() {
            @Override
            public void onSuccess(String token) {
                callback.success(true);
            }

            @Override
            public void onFailure(Throwable e) {
                callback.success(false);
                callback.failure(e);
            }
        });
    }

    public void authorizeUser(AuthorizationCallback callback){
        mCallback = callback;

        String PRODUCT_DSN = Settings.Secure.getString(mContext.getContentResolver(),
                Settings.Secure.ANDROID_ID);

        Bundle options = new Bundle();
        String scope_data = "{\"alexa:all\":{\"productID\":\"" + mProductId +
                "\", \"productInstanceAttributes\":{\"deviceSerialNumber\":\"" +
                PRODUCT_DSN + "\"}}}";
        options.putString(AuthzConstants.BUNDLE_KEY.SCOPE_DATA.val, scope_data);

        options.putBoolean(AuthzConstants.BUNDLE_KEY.GET_AUTH_CODE.val, true);
        options.putString(AuthzConstants.BUNDLE_KEY.CODE_CHALLENGE.val, getCodeChallenge());
        options.putString(AuthzConstants.BUNDLE_KEY.CODE_CHALLENGE_METHOD.val, "S256");

        mAuthManager.authorize(APP_SCOPES, options, authListener);
    }

    private AuthorizationListener authListener = new AuthorizationListener() {
        /**
         * Authorization was completed successfully.
         * Display the profile of the user who just completed authorization
         * @param response bundle containing authorization response. Not used.
         */
        @Override
        public void onSuccess(Bundle response) {
            String authCode = response.getString(AuthzConstants.BUNDLE_KEY.AUTHORIZATION_CODE.val);

            if(BuildConfig.DEBUG) {
                Log.i(TAG, "Authorization successful");
                Util.showAuthToast(mContext, "Authorization successful.");
            }

            TokenManager.getAccessToken(mContext, authCode, getCodeVerifier(), mAuthManager, new TokenManager.TokenResponseCallback() {
                @Override
                public void onSuccess(TokenManager.TokenResponse response) {
                    if(mCallback != null){
                        mCallback.onSuccess();
                    }
                }

                @Override
                public void onFailure(VolleyError error) {
                    if(mCallback != null){
                        mCallback.onError(error);
                    }
                }
            });

        }


        /**
         * There was an error during the attempt to authorize the application.
         * Log the error, and reset the profile text view.
         * @param ae the error that occurred during authorize
         */
        @Override
        public void onError(AuthError ae) {
            if(BuildConfig.DEBUG) {
                Log.e(TAG, "AuthError during authorization", ae);
                Util.showAuthToast(mContext, "Error during authorization.  Please try again.");
            }
            if(mCallback != null){
                mCallback.onError(ae);
            }
        }

        /**
         * Authorization was cancelled before it could be completed.
         * A toast is shown to the user, to confirm that the operation was cancelled, and the profile text view is reset.
         * @param cause bundle containing the cause of the cancellation. Not used.
         */
        @Override
        public void onCancel(Bundle cause) {
            if(BuildConfig.DEBUG) {
                Log.e(TAG, "User cancelled authorization");
                Util.showAuthToast(mContext, "Authorization cancelled");
            }

            if(mCallback != null){
                mCallback.onCancel();
            }
        }
    };



    private String getCodeVerifier(){
        if(Util.getPreferences(mContext).contains(CODE_VERIFIER)){
            return Util.getPreferences(mContext).getString(CODE_VERIFIER, "");
        }
        String verifier = createCodeVerifier();
        Util.getPreferences(mContext).edit().putString(CODE_VERIFIER, verifier).apply();
        return verifier;
    }

    private String getCodeChallenge(){
        String verifier = getCodeVerifier();
        return base64UrlEncode(getHash(verifier));
    }

    public static String createCodeVerifier() {
        char[] chars = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ1234567890".toCharArray();
        StringBuilder sb = new StringBuilder();
        Random random = new Random();
        for (int i = 0; i < 128; i++) {
            char c = chars[random.nextInt(chars.length)];
            sb.append(c);
        }
        String verifier = sb.toString();
        return verifier;
    }


    public static String base64UrlEncode(byte[] arg)
    {
        String s = Base64.encodeToString(arg, 0); // Regular base64 encoder
        s = s.split("=")[0]; // Remove any trailing '='s
        s = s.replace('+', '-'); // 62nd char of encoding
        s = s.replace('/', '_'); // 63rd char of encoding
        return s;
    }

    public static byte[] getHash(String password) {
        MessageDigest digest=null;
        try {
            digest = MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException e1) {
            e1.printStackTrace();
        }
        digest.reset();
        byte[] response = digest.digest(password.getBytes());

        return response;
    }


    public AmazonAuthorizationManager getAmazonAuthorizationManager(){
        return mAuthManager;
    }

}
