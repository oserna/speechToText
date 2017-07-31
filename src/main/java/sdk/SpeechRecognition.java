package sdk;

import com.aldebaran.qi.Application;
import com.aldebaran.qi.CallError;
import com.aldebaran.qi.Session;
import com.aldebaran.qi.helper.EventCallback;
import com.aldebaran.qi.helper.proxies.ALMemory;
import com.aldebaran.qi.helper.proxies.ALSpeechRecognition;
import com.aldebaran.qi.helper.proxies.ALTextToSpeech;
import hello.IntentDetection;

import java.util.ArrayList;
import java.util.List;

public class SpeechRecognition {


    private final IntentDetection intentDetection;

    public SpeechRecognition(IntentDetection intentDetection) {
        this.intentDetection = intentDetection;
    }

    public void start() throws Exception {

        String robotUrl = "tcp://192.168.8.101:9559";

        // Create a new application
        Application application = new Application(new String[]{}, robotUrl);

        // Start your application
        application.start();

        System.out.println("Successfully connected to the robot");
        // Subscribe to selected ALMemory events
        //SpeechRecognition reactor = new SpeechRecognition();
        this.run(application.session());
        // Run your application
        application.run();

    }

    ALMemory memory;
    ALTextToSpeech tts;
    ALSpeechRecognition sr;
    long speechRecognitionId = 0;

    public void run(Session session) throws Exception {
        tts = new ALTextToSpeech(session);
        memory = new ALMemory(session);
        sr = new ALSpeechRecognition(session);
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
                tts.say("Hello Mr. Freddie");
            }
        });



    }
}