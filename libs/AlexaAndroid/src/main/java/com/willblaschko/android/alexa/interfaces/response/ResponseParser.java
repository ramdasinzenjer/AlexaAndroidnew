package com.willblaschko.android.alexa.interfaces.response;

import android.util.Log;

import com.google.gson.Gson;
import com.willblaschko.android.alexa.data.Directive;
import com.willblaschko.android.alexa.interfaces.AvsException;
import com.willblaschko.android.alexa.interfaces.AvsItem;
import com.willblaschko.android.alexa.interfaces.AvsResponse;
import com.willblaschko.android.alexa.interfaces.alerts.AvsSetAlertItem;
import com.willblaschko.android.alexa.interfaces.audioplayer.AvsPlayAudioItem;
import com.willblaschko.android.alexa.interfaces.audioplayer.AvsPlayRemoteItem;
import com.willblaschko.android.alexa.interfaces.playbackcontrol.AvsMediaNextCommandItem;
import com.willblaschko.android.alexa.interfaces.playbackcontrol.AvsMediaPauseCommandItem;
import com.willblaschko.android.alexa.interfaces.playbackcontrol.AvsMediaPlayCommandItem;
import com.willblaschko.android.alexa.interfaces.playbackcontrol.AvsMediaPreviousCommandItem;
import com.willblaschko.android.alexa.interfaces.playbackcontrol.AvsReplaceAllItem;
import com.willblaschko.android.alexa.interfaces.playbackcontrol.AvsReplaceEnqueuedItem;
import com.willblaschko.android.alexa.interfaces.speaker.AvsAdjustVolumeItem;
import com.willblaschko.android.alexa.interfaces.speaker.AvsSetMuteItem;
import com.willblaschko.android.alexa.interfaces.speaker.AvsSetVolumeItem;
import com.willblaschko.android.alexa.interfaces.speechrecognizer.AvsExpectSpeechItem;
import com.willblaschko.android.alexa.interfaces.speechsynthesizer.AvsSpeakItem;

import org.apache.commons.fileupload.MultipartStream;
import org.apache.commons.io.IOUtils;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.io.StringWriter;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Static helper class to parse incoming responses from the Alexa server and generate a corresponding
 * {@link AvsResponse} item with all the directives matched to their audio streams.
 *
 * @author will on 5/21/2016.
 */
public class ResponseParser {

    public static final String TAG = "ResponseParser";

    /**
     * Get the AvsItem associated with a Alexa API post/get, this will contain a list of {@link AvsItem} directives,
     * if applicable.
     * @param stream the input stream as a result of our  OkHttp post/get calls
     * @param boundary the boundary we're using to separate the multiparts
     * @return the parsed AvsResponse
     * @throws IOException
     */

    public static AvsResponse parseResponse(InputStream stream, String boundary) throws IOException, IllegalStateException, AvsException {
        long start = System.currentTimeMillis();

        List<Directive> directives = new ArrayList<>();
        HashMap<String, ByteArrayInputStream> audio = new HashMap<>();

        MultipartStream mpStream = new MultipartStream(stream, boundary.getBytes(), 100000, null);

        Pattern pattern = Pattern.compile("<(.*?)>");

        //have to do this otherwise mpStream throws and exception
        if(mpStream.skipPreamble()){
            Log.i(TAG, "Found initial boundary: true");
        }else {
            StringWriter writer = new StringWriter();
            IOUtils.copy(stream, writer, Charset.defaultCharset());
            String body = writer.toString();
            Log.e(TAG, body);
            throw new AvsException("Response from Alexa server malformed.");
        }

        //we have to use the count hack here because otherwise readBoundary() throws an exception
        int count = 0;
        while (count < 1 || mpStream.readBoundary()) {
            String headers = mpStream.readHeaders();
            ByteArrayOutputStream data = new ByteArrayOutputStream();
            mpStream.readBodyData(data);
            if (!isJson(headers)) {
                // get the audio data
                //convert our multipart into byte data
                String contentId = getCID(headers);
                if(contentId != null) {
                    Matcher matcher = pattern.matcher(contentId);
                    if (matcher.find()) {
                        String currentId = "cid:" + matcher.group(1);
                        audio.put(currentId, new ByteArrayInputStream(data.toByteArray()));
                    }
                }
            } else {
                // get the json directive
                String directive = data.toString(Charset.defaultCharset().displayName());
                directives.add(getDirective(directive));
            }
            count++;
        }


        AvsResponse response = new AvsResponse();

        for(Directive directive: directives){

            Log.i(TAG, "Parsing directive type: "+directive.getHeader().getNamespace()+":"+directive.getHeader().getName());

            if(directive.isPlayBehaviorReplaceAll()){
                response.add(0, new AvsReplaceAllItem());
            }
            if(directive.isPlayBehaviorReplaceEnqueued()){
                response.add(new AvsReplaceEnqueuedItem());
            }

            AvsItem item = null;

            if(directive.isTypeSpeak()){
                String cid = directive.getPayload().getUrl();
                ByteArrayInputStream sound = audio.get(cid);
                item = new AvsSpeakItem(cid, sound);
            }else if(directive.isTypePlay()){
                String url = directive.getPayload().getAudioItem().getStream().getUrl();
                if(url.contains("cid:")){
                    ByteArrayInputStream sound = audio.get(url);
                    item = new AvsPlayAudioItem(url, sound);
                }else{
                    item = new AvsPlayRemoteItem(url, directive.getPayload().getAudioItem().getStream().getOffsetInMilliseconds());
                }
            }else if(directive.isTypeSetAlert()){
                item = new AvsSetAlertItem(directive.getPayload().getToken(), directive.getPayload().getType(), directive.getPayload().getScheduledTime());
                response.add(item);
            }else if(directive.isTypeSetMute()){
                item = new AvsSetMuteItem(directive.getPayload().isMute());
            }else if(directive.isTypeSetVolume()){
                item = new AvsSetVolumeItem(directive.getPayload().getVolume());
            }else if(directive.isTypeAdjustVolume()){
                item = new AvsAdjustVolumeItem(directive.getPayload().getVolume());
            }else if(directive.isTypeExpectSpeech()){
                item = new AvsExpectSpeechItem(directive.getPayload().getTimeoutInMilliseconds());
            }else if(directive.isTypeMediaPlay()){
                item = new AvsMediaPlayCommandItem();
            }else if(directive.isTypeMediaPause()){
                item = new AvsMediaPauseCommandItem();
            }else if(directive.isTypeMediaNext()){
                item = new AvsMediaNextCommandItem();
            }else if(directive.isTypeMediaPrevious()){
                item = new AvsMediaPreviousCommandItem();
            }

            if(item != null){
                response.add(item);
            }
        }

        Log.i(TAG, "Parsing response took: " + (System.currentTimeMillis() - start));

        return response;
    }

    /**
     * Parse our directive using Gson into an object
     * @param directive the string representation of our JSON object
     * @return the reflected directive
     */
    private static Directive getDirective(String directive){
        Gson gson = new Gson();
        Directive.DirectiveWrapper wrapper = gson.fromJson(directive, Directive.DirectiveWrapper.class);
        return wrapper.getDirective();
    }


    /**
     * Get the content id from the return headers from the AVS server
     * @param headers the return headers from the AVS server
     * @return a string form of our content id
     */
    private static String getCID(String headers) throws IOException {
        final String contentString = "Content-ID:";
        BufferedReader reader = new BufferedReader(new StringReader(headers));
        for (String line = reader.readLine(); line != null; line = reader.readLine()) {
            if (line.startsWith(contentString)) {
                return line.substring(contentString.length()).trim();
            }
        }
        return null;
    }

    /**
     * Check if the response is JSON (a validity check)
     * @param headers the return headers from the AVS server
     * @return true if headers state the response is JSON, false otherwise
     */
    private static boolean isJson(String headers) {
        if (headers.contains("application/json")) {
            return true;
        }
        return false;
    }
}
