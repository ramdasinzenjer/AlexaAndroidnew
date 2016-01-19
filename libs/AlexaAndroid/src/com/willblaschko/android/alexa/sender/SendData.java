package com.willblaschko.android.alexa.sender;

import android.util.Log;

import com.willblaschko.android.alexa.avs.AvsException;
import com.willblaschko.android.alexa.avs.AvsResponse;
import com.willblaschko.android.alexa.callbacks.AsyncCallback;

import org.apache.commons.fileupload.MultipartStream;
import org.apache.commons.io.IOUtils;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.List;
import java.util.Map;

import javax.json.Json;

/**
 * Created by will on 12/7/2015.
 */
public abstract class SendData {

    private final static String TAG = "SendData";

    // multipart request boundary token
    private static final String MPBOUND = "THISISTHEBOUNDARY1234";
    private static final String LINEFEED = "\r\n";
    private static final String BOUND = LINEFEED + "--" + MPBOUND + LINEFEED; // part boundary
    // end of multipart stream
    private static final String END = LINEFEED + LINEFEED + "--" + MPBOUND + "--" + LINEFEED;
    // audio format AlexaService expects
    private static final int CHUNK_LENGTH = 0; // use the default chunk length

    private HttpURLConnection mConnection;
    protected DataOutputStream mOutputStream;

    protected AsyncCallback<Void, Exception> mCallback;


    protected void preparePost(String url, String accessToken) throws IOException {
        mConnection = null;
        mOutputStream = null;

        URL obj = new URL(url);
        mConnection = (HttpURLConnection) obj.openConnection();

        mConnection.setRequestMethod("POST");

        mConnection.setRequestProperty("Authorization", "Bearer " + accessToken);

        mConnection.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + MPBOUND);
        mConnection.setRequestProperty("Transfer-Encoding", "chunked"); // chunked transfer
        mConnection.setChunkedStreamingMode(CHUNK_LENGTH);
        mConnection.setDoOutput(true);
        mOutputStream = new DataOutputStream(mConnection.getOutputStream());

        String metadata = generateSpeechMetadata();

        System.out.println("\n\nSending 'POST' request to URL : " + obj.toString());
        System.out.println(metadata);
        // metadata header
        mOutputStream.writeBytes(BOUND + "Content-Disposition: form-data; name=\"metadata\""
                + LINEFEED + "Content-Type: application/json; charset=UTF-8" + LINEFEED + LINEFEED);
        mOutputStream.write(metadata.getBytes(Charset.forName("UTF-8")));
        // audio header
        mOutputStream.writeBytes(BOUND + "Content-Disposition: form-data; name=\"audio\""
                + LINEFEED + "Content-Type: audio/L16; rate=16000; channels=1" + LINEFEED
                + LINEFEED);

    }

    protected AvsResponse completePost() throws IOException, AvsException {
        long start = System.currentTimeMillis();
        int responseCode = -1;
        InputStream response = null;
        InputStream error = null;

        try {
            mOutputStream.writeBytes(END);
            mOutputStream.flush();
            mOutputStream.close();

            responseCode = mConnection.getResponseCode();
            System.out.println("Response Code : " + responseCode);
            logHeaders(mConnection.getHeaderFields());
            response = mConnection.getInputStream();

            String charSet =
                    getHeaderParameter(mConnection.getHeaderField("Content-Type"), "charset",
                            Charset.forName("UTF-8").displayName());
            String responseBoundary =
                    getHeaderParameter(mConnection.getHeaderField("Content-Type"), "boundary", null);
            if(responseBoundary == null){
                if(mCallback != null){
                    mCallback.failure(new IllegalArgumentException("No data found in response."));
                }
                return null;
            }
            Log.i(TAG, "Complete post took: " + (System.currentTimeMillis() - start));
            return parseResponse(response, responseBoundary.getBytes(charSet));
        } catch (IOException e) {
            if (mConnection != null) {
                error = mConnection.getErrorStream(); // get the error stream
                if (error != null) {
                    // throw the error stream as a string if present
                    AvsException avs = new AvsException(responseCode + ": " + IOUtils.toString(error));
                    if(mCallback != null){
                        mCallback.failure(e);
                    }
                    throw avs;
                }
            }
            if(mCallback != null){
                mCallback.failure(e);
            }
            throw e; // otherwise just throw the original exception
        } finally {
            if(mOutputStream != null) {
                mOutputStream.close();
            }
            if(response != null) {
                response.close();
            }
            if(error != null) {
                error.close();
            }

        }
    }

    // assumes a single part in the multipart blob with the audio data
    public AvsResponse parseResponse(InputStream inpStr, byte[] boundary) throws IOException {
        long start = System.currentTimeMillis();
        MultipartStream mpStream = new MultipartStream(inpStr, boundary, 100000, null);
        // skip past the headers and multipart declaration
        Log.i(TAG, "Found initial boundary: " + mpStream.skipPreamble());

        Log.i(TAG, "\n" + mpStream.readHeaders()); // print the headers of the json part
        ByteArrayOutputStream json = new ByteArrayOutputStream();
        mpStream.readBodyData(json); // get the json data
        System.out.println(json.toString(Charset.defaultCharset().displayName()));
        AvsResponse response =
                new AvsResponse(Json.createReader(new ByteArrayInputStream(json.toByteArray())));

        // add the [cid->audiodata] to the map in the response
        while (mpStream.readBoundary()) {
            String headers = mpStream.readHeaders();
            System.out.println("\n" + headers); // print the headers of audio part
            ByteArrayOutputStream data = new ByteArrayOutputStream();
            mpStream.readBodyData(data); // get the audio data
            if (!isJson(headers)) {
                response.addAudio(getCID(headers), new ByteArrayInputStream(data.toByteArray()));
            } else {
                System.out.println(data.toString(Charset.defaultCharset().displayName()));
            }
        }

        Log.i(TAG, "Parsing response took: " + (System.currentTimeMillis() - start));
        return response;
    }

    private String getCID(String headers) {
        final String contentString = "Content-ID:";
        BufferedReader reader = new BufferedReader(new StringReader(headers));
        try {
            for (String line = reader.readLine(); line != null; line = reader.readLine()) {
                if (line.startsWith(contentString)) {
                    return line.substring(contentString.length()).trim();
                }
            }
        } catch (Exception e) {
            if(mCallback != null){
                mCallback.failure(e);
            }
        }
        return null;
    }

    private boolean isJson(String headers) {
        if (headers.contains("application/json")) {
            return true;
        }
        return false;
    }

    public static String getHeaderParameter(final String headerValue, final String key,
                                            final String defaultValue) {
        if (headerValue == null || key == null) {
            return null;
        }

        String[] parts = headerValue.split(";");

        String value = defaultValue;
        for (String part : parts) {
            part = part.trim();

            if (part.startsWith(key)) {
                value = part.substring(key.length() + 1).replaceAll("(^\")|(\"$)", "").trim();
            }
        }
        return value;
    }

    private void logHeaders(Map<String, List<String>> map) {
        try {
            System.out.println("Response Headers:");
            for (Map.Entry<String, List<String>> entry : map.entrySet()) {
                System.out.println(entry.getKey() + ": " + entry.getValue());
            }
        } catch (IllegalStateException e) {
            e.printStackTrace();
            if(mCallback != null){
                mCallback.failure(e);
            }
        }
    }

    private String generateSpeechMetadata() {
        return "{\n" +
                "\"messageHeader\": {\n" +
                "\"deviceContext\": [\n" +
                "{\n" +
                "\"name\": \"playbackState\",\n" +
                "\"namespace\": \"AudioPlayer\",\n" +
                "\"payload\": {\n" +
                "\"streamId\": \"\",\n" +
                "\"offsetInMilliseconds\": \"\",\n" +
                "\"playerActivity\": \"IDLE\"\n" +
                "}\n" +
                "}\n" +
                "]\n" +
                "},\n" +
                "\"messageBody\": {\n" +
                "\"profile\": \"doppler-scone\",\n" +
                "\"locale\": \"en-us\",\n" +
                "\"format\": \"audio/L16; rate=16000; channels=1\"\n" +
                "}\n" +
                "}";
    }
}
