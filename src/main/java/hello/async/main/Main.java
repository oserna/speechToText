package hello.async.main;

import com.aldebaran.qi.Application;
import com.aldebaran.qi.CallError;
import com.aldebaran.qi.helper.EventCallback;
import com.aldebaran.qi.helper.proxies.ALMemory;
import com.aldebaran.qi.helper.proxies.ALSpeechRecognition;
import com.aldebaran.qi.helper.proxies.ALTextToSpeech;
import hello.IntentDetection;
import hello.async.AudioEventLoop;
import hello.async.AudioProcessor;
import hello.async.EventType;
import hello.async.bus.Event;
import hello.async.bus.EventBuilder;
import hello.async.bus.EventBus;
import hello.async.bus.EventBusAsync;
import hello.async.bus.EventHandler;
import hello.async.recognition.GingerSpeechRecognition;
import hello.async.speak.GingerSpeaker;
import sdk.EventCallbackProcessor;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class Main {

    ALMemory memory;
    ALTextToSpeech tts;
    ALSpeechRecognition sr;
    long speechRecognitionId = 0;

    private AudioEventLoop audioEventLoop = null;
    private AudioProcessor audioProcessor = null;
    private GingerSpeaker gingerSpeaker = null;
    private GingerSpeechRecognition speechRecognition = null;
    private EventBus<Event> eventBus = null;

    public Main() {
        this.eventBus = new EventBusAsync<>();

        this.audioEventLoop = new AudioEventLoop(UUID.randomUUID().toString(), eventBus);
        this.audioProcessor = new AudioProcessor("TOKEN", eventBus);
        this.gingerSpeaker = new GingerSpeaker(tts, eventBus);
        this.speechRecognition = new GingerSpeechRecognition(sr, eventBus);

    }

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

        System.out.println("Aldebaran stuff initialized");

        this.subscribe(eventBus, audioEventLoop.getEventHandlers());
        this.subscribe(eventBus, audioProcessor.getEventHandlers());
        this.subscribe(eventBus, gingerSpeaker.getEventHandlers());
        this.subscribe(eventBus, speechRecognition.getEventHandlers());


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
                }
            }
        });
        memory.subscribeToEvent("CodeWord", new EventCallback<String>() {
            @Override
            public void onEvent(String data) throws InterruptedException, CallError {
                System.out.println("CodeWord: " + data);

                String text = "Welcome to I N G . What would you like to do? You can check your account balances or maybe open a new account...";

                tts.say(text);

                eventBus.publish(
                        EventBuilder.create(EventType.SPEECH_ENDED)
                                .set("TEXT", text)
                                .build());

            }
        });

        this.audioEventLoop.start();

        // Run your application
        application.run();

        ////////////////////
    }

    private void subscribe(EventBus<Event> eventBus, List<EventHandler> handlers){
        for (EventHandler handler: handlers){
            eventBus.subscribe(handler);
        }
    }

    public static void main(String[] args) throws Exception {
        Main instance = new Main();
        instance.init();
    }
}
