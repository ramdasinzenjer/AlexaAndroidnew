#Alexa Voice Library

*A library and sample app to abstract access to the Amazon Alexa service for Android applications.*

First and foremost, my goal with this project is to help others who have less of an understanding of Java, Android, or both, to be able to quickly and easily integrate the Amazon Alexa platform into their own applications.

For getting started with the Amazon Alexa platform, take a look over here: https://developer.amazon.com/appsandservices/solutions/alexa/alexa-voice-service/getting-started-with-the-alexa-voice-service

##Sample Application

The application contained within this repo, which is used to showcase the included library, functions as a home assist replacement (drag up from the home button on an Android device), which would typically trigger Google Now, but instead now launches the VoiceLaunchActivity.java Activity.

This application is about a bare-bones as is possible given the Android system and the library, it uses the built in speech-to-text functionality in the Google Play Services to get user input and then transfer it to the Alexa cloud.

The application could be easily extended to be Alexa Skill-specific or be integrated as part of a larger application that needs remote voice control capabilities.

##Using the Library

Most of the library can be accessed through the [AlexaManager](http://willblaschko.github.io/AlexaAndroid/com/willblaschko/android/alexa/AlexaManager.html) and [AlexaAudioPlayer](http://willblaschko.github.io/AlexaAndroid/com/willblaschko/android/alexa/avs/AlexaAudioPlayer.html) classes, both of which are singletons.

###Installation

* Copy the libs/AlexaAndroid folder into your application, and set it up as a library.
* Follow the process for creating a connected device detailed at the Amazon link at the top of the Readme.
* Add login-with-amazon-sdk.jar (part of the Amazon process) to the AlexaAndroid/libs folder.
* Add your api_key.txt file (part of the Amazon process) to the app/src/main/assets folder.
* Start integration and testing!

###Library Instantiation

```java
//get our AlexaManager instance for convenience
AlexaManager mAlexaManager = AlexaManager.getInstance(this, PRODUCT_ID);

//instantiate our audio player
AlexaAudioPlayer mAudioPlayer = AlexaAudioPlayer.getInstance(this);

//Remove the current item and check for more items once
//we've finished playing
mAudioPlayer.addCallback(mAlexaAudioPlayerCallback);
```

###Library Methods

Here's a quick overview of the other methods that will likely be accessed, check the [JavaDoc](http://willblaschko.github.io/AlexaAndroid/) for more details. All methods below are asynchronous:

```java
//Run an async check on whether we're logged in or not
mAlexaManager.checkLoggedIn(mLoggedInCheck);

//Check if the user is already logged in to their Amazon account
mAlexaManager.checkLoggedIn(AsyncCallback...);

//Log the user in
mAlexaManager.logIn(AuthorizationCallback...);

//Send a request to the Alexa server and process results on the AsyncCallback
mAlexaManager.sendTextRequest(REQUEST_TYPE.TYPE_VOICE_RESPONSE, String ..., AsyncCallback ...);

//Start creating a request using audio, for Android-M make
//sure we have permission to access microphone first
mAlexaManager.startRecording(REQUEST_TYPE.TYPE_VOICE_RESPONSE, AsyncCallback...);

//Start creating a request using audio but prepend an asset
//(raw audio recorded at the correct bitrate) to the front of the request: "Open MySkillKit and..."
mAlexaManager.startRecording(REQUEST_TYPE.TYPE_VOICE_RESPONSE, int..., AsyncCallback...);

//Start creating a request using audio but prepend a byte array
//(raw audio recorded at the correct bitrate) to the front of the request: "Open MySkillKit and..."
mAlexaManager.startRecording(REQUEST_TYPE.TYPE_VOICE_RESPONSE, byte[]..., AsyncCallback...);

//Stop recording the request and send it to the Alexa server,
//process results on the AsyncCallback
mAlexaManager.stopRecording(AsynCallback...);

//Play an AvsPlayItem returned by our requests
mAudioPlayer.playItem(AvsPlayItem...);

//Play an AvsSpeakItem returned by our requests
mAudioPlayer.playItem(AvsSpeakItem...);
```

##Everything Else

Let me know if you would like to contribute to this library!