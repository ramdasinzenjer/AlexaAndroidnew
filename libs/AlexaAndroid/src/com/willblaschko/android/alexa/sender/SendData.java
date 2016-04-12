package com.willblaschko.android.alexa.sender;

import android.util.Log;

import com.willblaschko.android.alexa.BuildConfig;
import com.willblaschko.android.alexa.Util;
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
 * An abstract class that supplies a DataOutputStream which is used to send a POST request to the AVS server
 * with a voice data intent, it handles the response with completePost() (called by extending classes)
 */
public abstract class SendData {

    private final static String TAG = "SendData";

    // multipart request boundary token
    private static final String MPBOUND = Util.generateMultipartBoundary();
    private static final String LINEFEED = "\r\n";
    private static final String BOUND = LINEFEED + "--" + MPBOUND + LINEFEED; // part boundary
    // end of multipart stream
    private static final String END = LINEFEED + LINEFEED + "--" + MPBOUND + "--" + LINEFEED;
    // audio format AlexaService expects
    private static final int CHUNK_LENGTH = 0; // use the default chunk length

    private HttpURLConnection mConnection;

    //the output stream that extending classes will use to pass data to the AVS server
    protected DataOutputStream mOutputStream;

    protected AsyncCallback<Void, Exception> mCallback;

    /**
     * Set up all the headers that we need in our HttpURLConnection POST, this prepares the connection for
     * the raw audio that we'll need to pass to the AVS server
     * @param url the URL we're posting to (this only has one option currently, but passing that in in case it changes)
     * @param accessToken the access token of the user who has given consent to the app
     * @throws IOException if we can't perform our operations on the mConnection (not initiated, etc)
     */
    protected void preparePost(String url, String accessToken) throws IOException {
        mConnection = null;
        mOutputStream = null;

        //open a connection to the URL
        URL obj = new URL(url);
        mConnection = (HttpURLConnection) obj.openConnection();

        //set the request to POST
        mConnection.setRequestMethod("POST");

        //set our authentication access token header
        mConnection.setRequestProperty("Authorization", "Bearer " + accessToken);

        //set the content type to multipart and define our boundary
        mConnection.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + MPBOUND);
        //let the server know we're sending a chunked transfer
        mConnection.setRequestProperty("Transfer-Encoding", "chunked");
        mConnection.setChunkedStreamingMode(CHUNK_LENGTH);
        mConnection.setDoOutput(true);

        //get the output stream from the connection
        mOutputStream = new DataOutputStream(mConnection.getOutputStream());

        //generate our metadata, this seems to be static for the moment, but might need to change
        String metadata = generateSpeechMetadata();

        //logging for sanity's stake
        if(BuildConfig.DEBUG) {
            Log.i(TAG, "\n\nSending 'POST' request to URL : " + obj.toString());
            Log.i(TAG, metadata);
        }

        //metadata header
        mOutputStream.writeBytes(BOUND + "Content-Disposition: form-data; name=\"metadata\""
                + LINEFEED + "Content-Type: application/json; charset=UTF-8" + LINEFEED + LINEFEED);
        mOutputStream.write(metadata.getBytes(Charset.forName("UTF-8")));
        //audio header
        mOutputStream.writeBytes(BOUND + "Content-Disposition: form-data; name=\"audio\""
                + LINEFEED + "Content-Type: audio/L16; rate=16000; channels=1" + LINEFEED
                + LINEFEED);

    }

    /**
     * When finished adding voice data to the output, we close it using completePost() and it is sent off to the AVS server
     * and the response is parsed and returned
     * @return AvsResponse with all the data returned from the server
     * @throws IOException
     * @throws AvsException
     */
    protected AvsResponse completePost() throws IOException, AvsException {
        long start = System.currentTimeMillis();
        int responseCode = -1;
        InputStream response = null;
        InputStream error = null;

        try {
            //finish our POST and send it
            mOutputStream.writeBytes(END);
            mOutputStream.flush();
            mOutputStream.close();

            //get the response code from the server
            responseCode = mConnection.getResponseCode();

            //logging for sanity's stake
            if(BuildConfig.DEBUG) {
                Log.i(TAG,"Response Code : " + responseCode);
                logHeaders(mConnection.getHeaderFields());
            }

            //our input stream (raw audio)
            response = mConnection.getInputStream();

            String charSet =
                    getHeaderParameter(mConnection.getHeaderField("Content-Type"), "charset",
                            Charset.forName("UTF-8").displayName());
            String responseBoundary =
                    getHeaderParameter(mConnection.getHeaderField("Content-Type"), "boundary", null);

            //if we have no data, pass up a failure and return null
            if(responseBoundary == null){
                if(mCallback != null){
                    mCallback.failure(new IllegalArgumentException("No data found in response."));
                    mCallback.complete();
                }
                return null;
            }
            Log.i(TAG, "Complete post took: " + (System.currentTimeMillis() - start));

            //parse the response
            return parseResponse(response, responseBoundary.getBytes(charSet));
        } catch (IOException e) {
            if (mConnection != null) {
                error = mConnection.getErrorStream(); // get the error stream
                if (error != null) {
                    // throw the error stream as a string if present
                    AvsException avs = new AvsException(responseCode + ": " + IOUtils.toString(error));
                    if(mCallback != null){
                        mCallback.failure(e);
                        mCallback.complete();
                    }
                    throw avs;
                }
            }
            if(mCallback != null){
                mCallback.failure(e);
                mCallback.complete();
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

    /**
     * Assumes a single part in the multipart blob with the audio data
     * @param inpStr the input stream as a result of our HttpURLConnection POST
     * @param boundary the boundary we're using to separate the multiparts
     * @return the parsed AvsResponse
     * @throws IOException
     */
    public AvsResponse parseResponse(InputStream inpStr, byte[] boundary) throws IOException {
        long start = System.currentTimeMillis();
        MultipartStream mpStream = new MultipartStream(inpStr, boundary, 100000, null);
        // skip past the headers and multipart declaration

        Log.i(TAG, "Found initial boundary: " + mpStream.skipPreamble());

        //clean up the headers
        String heads = mpStream.readHeaders();

        //logging for sanity's stake
        if(BuildConfig.DEBUG) {
            Log.i(TAG, "\n" + heads); // print the headers of the json part
        }

        ByteArrayOutputStream json = new ByteArrayOutputStream();
        //get the json data
        mpStream.readBodyData(json);

        //logging for sanity's stake
        if(BuildConfig.DEBUG) {
            Log.i(TAG, json.toString(Charset.defaultCharset().displayName()));
        }

        AvsResponse response = new AvsResponse(Json.createReader(new ByteArrayInputStream(json.toByteArray())));

        // add the [cid->audiodata] to the map in the response
        while (mpStream.readBoundary()) {
            String headers = mpStream.readHeaders();
            System.out.println("\n" + headers); // print the headers of audio part
            ByteArrayOutputStream data = new ByteArrayOutputStream();
            mpStream.readBodyData(data); // get the audio data
            if (!isJson(headers)) {
                //convert our multipart into byte data
                response.addAudio(getCID(headers), new ByteArrayInputStream(data.toByteArray()));
            } else {
                System.out.println(data.toString(Charset.defaultCharset().displayName()));
            }
        }

        Log.i(TAG, "Parsing response took: " + (System.currentTimeMillis() - start));
        return response;
    }

    /**
     * Get the content id from the return headers from the AVS server
     * @param headers the return headers from the AVS server
     * @return a string form of our content id
     */
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
                mCallback.complete();
            }
        }
        return null;
    }

    /**
     * Check if the response is JSON (a validity check)
     * @param headers the return headers from the AVS server
     * @return true if headers state the response is JSON, false otherwise
     */
    private boolean isJson(String headers) {
        if (headers.contains("application/json")) {
            return true;
        }
        return false;
    }

    /**
     * Get a value from the header response from the AVS server, by key and default value
     * @param headerValue
     * @param key
     * @param defaultValue
     * @return
     */
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

    /**
     * Print out all our headers so we can see them in LogCat
     * @param map A Map<String, List<String>> of all the headers returned by the AVS server
     */
    private void logHeaders(Map<String, List<String>> map) {
        try {
            Log.i(TAG,"Response Headers:");
            for (Map.Entry<String, List<String>> entry : map.entrySet()) {
                Log.i(TAG,entry.getKey() + ": " + entry.getValue());
            }
        } catch (IllegalStateException e) {
            e.printStackTrace();
            if(mCallback != null){
                mCallback.failure(e);
                mCallback.complete();
            }
        }
    }

    /**
     * Placeholder which doesn't seem to matter, but this String needs to be present for a valid audio query
     * to be sent to the AVS servers
     * @return placeholder String
     */
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
