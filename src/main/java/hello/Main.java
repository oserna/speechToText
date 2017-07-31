package hello;

import com.aldebaran.qi.Application;
import com.aldebaran.qi.CallError;
import com.aldebaran.qi.Session;
import com.aldebaran.qi.helper.EventCallback;
import com.aldebaran.qi.helper.proxies.ALMemory;
import com.aldebaran.qi.helper.proxies.ALSpeechRecognition;
import com.aldebaran.qi.helper.proxies.ALTextToSpeech;
import sdk.EventCallbackProcessor;
import sdk.SpeechRecognition;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class Main {

    ALMemory memory;
    ALTextToSpeech tts;
    ALSpeechRecognition sr;
    long speechRecognitionId = 0;
    private IntentDetection intentDetection;


    private void init() throws Exception {
        String robotUrl = "tcp://127.0.0.1:9559";

        // Create a new application
        Application application = new Application(new String[]{}, robotUrl);

        // Start your application
        application.start();

        System.out.println("Successfully connected to the robot");
        memory = new ALMemory(application.session());
        sr = new ALSpeechRecognition(application.session());
        tts = new ALTextToSpeech(application.session());
        intentDetection = new IntentDetection(tts, sr, UUID.randomUUID().toString());



        EventCallbackProcessor processor = new EventCallbackProcessor(memory, sr);

        sr.setLanguage("English");
        List<String> vocabulary = new ArrayList();
        vocabulary.add("Ginger");

        sr.pause(true);
        sr.setVocabulary(vocabulary, false);
        sr.pause(false);

        speechRecognitionId = 0;
        speechRecognitionId = memory.subscribeToEvent("WordRecognized", processor);

        memory.subscribeToEvent("ALTextToSpeech/TextDone", new EventCallback<Integer>() {
            @Override
            public void onEvent(Integer ready) throws InterruptedException, CallError {
                System.out.println("Text Done: " + ready.toString());
                if (ready.intValue() == 1) {
                    //sr.pause(false);
                    intentDetection.setIsTriggered(true);
                }
            }
        });
        memory.subscribeToEvent("CodeWord", new EventCallback<String>() {
            @Override
            public void onEvent(String data) throws InterruptedException, CallError {
                System.out.println("CodeWord: " + data);
                tts.say("Welcome to I N G . What would you like to do? You can check your account balances or maybe open a new account...");
            }
        });

        new Thread(intentDetection).start();

        // Run your application
        application.run();

        ////////////////////
    }

    public static void main(String[] args) throws Exception {
        Main instance = new Main();
        instance.init();
    }

}