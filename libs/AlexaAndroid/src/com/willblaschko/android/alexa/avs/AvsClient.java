/* Copyright 2015 Amazon.com, Inc. or its affiliates. All Rights Reserved. */

package com.willblaschko.android.alexa.avs;

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
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import javax.json.Json;

public class AvsClient {
    // multipart request boundary token
    private static final String MPBOUND = "THISISTHEBOUNDARY1234";
    private static final String LINEFEED = "\r\n";
    private static final String BOUND = LINEFEED + "--" + MPBOUND + LINEFEED; // part boundary
    // end of multipart stream
    private static final String END = LINEFEED + "--" + MPBOUND + "--" + LINEFEED;
    // audio format AlexaService expects
    private HttpURLConnection mConnection;
    private DataOutputStream mOutputStream;
    private static final int CHUNK_LENGTH = 0; // use the default chunk length

    private static final String SPEECHREQUEST = "/v1/avs/speechrecognizer/recognize";
    public static final String NEXTITEM = "/v1/avs/audioplayer/getNextItem";
    public static final String EVENTS = "/v1/avs/audioplayer/";

    public AvsResponse sendRequest(String url, String accessToken, String metadata)
            throws IOException, AvsException {
        HttpURLConnection connection = null;
        DataOutputStream outputStream = null;
        InputStream response = null;
        InputStream error = null;
        int responseCode = -1;

        try {
            URL obj = new URL(url);
            connection = (HttpURLConnection) obj.openConnection();

            connection.setRequestMethod("POST");
            connection.setRequestProperty("Authorization", "Bearer " + accessToken);
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setDoOutput(true);
            outputStream = new DataOutputStream(connection.getOutputStream());

            System.out.println("\n\nSending 'POST' request to URL : " + obj.toString());
            System.out.println(metadata);

            outputStream.write(metadata.getBytes(StandardCharsets.UTF_8));

            outputStream.flush();
            outputStream.close();

            responseCode = connection.getResponseCode();
            System.out.println("Response Code : " + responseCode);
            logHeaders(connection.getHeaderFields());
            if (responseCode == 204) {
                System.out.println("Found 204 response");
                return null;
            }
            response = connection.getInputStream();

            String charSet =
                    getHeaderParameter(connection.getHeaderField("Content-Type"), "charset",
                            StandardCharsets.UTF_8.displayName());
            String responseBoundary =
                    getHeaderParameter(connection.getHeaderField("Content-Type"), "boundary", null);

            return parseResponse(response, responseBoundary.getBytes(charSet));
        } catch (IOException e) {
            if (connection != null) {
                error = connection.getErrorStream();
                if (error != null) {
                    // throw error stream as string if present
                    throw new AvsException(responseCode + ": " + IOUtils.toString(error));
                }
            }
            throw e; // otherwise just throw the original exception
        } finally {
            IOUtils.closeQuietly(outputStream);
            IOUtils.closeQuietly(response);
            IOUtils.closeQuietly(error);
        }
    }

    // start the alexa http request
    // returns the connection output stream which should be filled with audio data
    public DataOutputStream startRequest(String url, String accessToken, String metadata)
            throws IOException {
        mConnection = null;
        mOutputStream = null;

        URL obj = new URL(url + SPEECHREQUEST);
        mConnection = (HttpURLConnection) obj.openConnection();

        mConnection.setRequestMethod("POST");

        mConnection.setRequestProperty("Authorization", "Bearer " + accessToken);

        mConnection.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + MPBOUND);
        mConnection.setRequestProperty("Transfer-Encoding", "chunked"); // chunked transfer
        mConnection.setChunkedStreamingMode(CHUNK_LENGTH);
        mConnection.setDoOutput(true);
        mOutputStream = new DataOutputStream(mConnection.getOutputStream());

        System.out.println("\n\nSending 'POST' request to URL : " + obj.toString());
        System.out.println(metadata);
        // metadata header
        mOutputStream.writeBytes(BOUND + "Content-Disposition: form-data; name=\"metadata\""
                + LINEFEED + "Content-Type: application/json; charset=UTF-8" + LINEFEED + LINEFEED);
        mOutputStream.write(metadata.getBytes(StandardCharsets.UTF_8));
        // audio header
        mOutputStream.writeBytes(BOUND + "Content-Disposition: form-data; name=\"audio\""
                + LINEFEED + "Content-Type: audio/L16; rate=16000; channels=1" + LINEFEED
                + LINEFEED);

        return mOutputStream;
    }

    // finish the request to the alexa http service after writing the audio data
    // returns the MP3 audio
    public AvsResponse finishRequest() throws IOException, AvsException {
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
                            StandardCharsets.UTF_8.displayName());
            String responseBoundary =
                    getHeaderParameter(mConnection.getHeaderField("Content-Type"), "boundary", null);

            return parseResponse(response, responseBoundary.getBytes(charSet));
        } catch (IOException e) {
            if (mConnection != null) {
                error = mConnection.getErrorStream(); // get the error stream
                if (error != null) {
                    // throw the error stream as a string if present
                    throw new AvsException(responseCode + ": " + IOUtils.toString(error));
                }
            }
            throw e; // otherwise just throw the original exception
        } finally {
            IOUtils.closeQuietly(mOutputStream);
            IOUtils.closeQuietly(response);
            IOUtils.closeQuietly(error);
        }
    }

    // assumes a single part in the multipart blob with the audio data
    public AvsResponse parseResponse(InputStream inpStr, byte[] boundary) throws IOException {
        MultipartStream mpStream = new MultipartStream(inpStr, boundary, 100000, null);
        // skip past the headers and multipart declaration
        System.out.println("Found initial boundary: " + mpStream.skipPreamble());

        System.out.println("\n" + mpStream.readHeaders()); // print the headers of the json part
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

        return response;
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
        }
        return null;
    }

    private boolean isJson(String headers) {
        if (headers.contains("application/json")) {
            return true;
        }
        return false;
    }

    private void logHeaders(Map<String, List<String>> map) {
        try {
            System.out.println("Response Headers:");
            for (Map.Entry<String, List<String>> entry : map.entrySet()) {
                System.out.println(entry.getKey() + ": " + entry.getValue());
            }
        } catch (IllegalStateException e) {
            e.printStackTrace();
        }
    }

}
