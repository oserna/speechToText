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
import hello.async.FakeAudioProcessor;
import hello.async.bus.Event;
import hello.async.bus.EventBuilder;
import hello.async.bus.EventBus;
import hello.async.bus.EventBusAsync;
import hello.async.bus.EventHandler;
import hello.async.recognition.ConsoleSpeechRecognition;
import hello.async.recognition.GingerSpeechRecognition;
import hello.async.speak.BasicSpeaker;
import hello.async.speak.ConsoleSpeaker;
import hello.async.speak.GingerSpeaker;
import sdk.EventCallbackProcessor;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class FakeMain {


    private AudioEventLoop audioEventLoop = null;
    private AudioProcessor audioProcessor = null;

    private ConsoleSpeaker gingerSpeaker = null;
    private BasicSpeaker basicSpeaker = null;
    private ConsoleSpeechRecognition speechRecognition = null;

    private EventBus<Event> eventBus = null;

    public FakeMain() {
        this.eventBus = new EventBusAsync<>();

        this.audioEventLoop = new AudioEventLoop(UUID.randomUUID().toString(), eventBus);
        this.audioProcessor = new AudioProcessor("ya29.El-ZBHzZ9D244ReFTXHdD3PJAKMw-DXo9sv3Efl_QLqGSkvJ5crwCNqTo6sN5eQ2ms4jZzDM4_2XaODyYVzSW2i8Ki-46BVuG4ApwWCBNoslJ5nqF16_HbfWAG0y1j3TeA",eventBus);
//        this.gingerSpeaker = new ConsoleSpeaker(eventBus);
        this.basicSpeaker = new BasicSpeaker(eventBus);
        this.speechRecognition = new ConsoleSpeechRecognition(eventBus);

    }

    private void init() throws Exception {

        this.subscribe(eventBus, audioEventLoop.getEventHandlers());
        this.subscribe(eventBus, audioProcessor.getEventHandlers());
        this.subscribe(eventBus, basicSpeaker.getEventHandlers());
        this.subscribe(eventBus, speechRecognition.getEventHandlers());

        this.audioEventLoop.start();

        Thread.sleep(1000);

        eventBus.publish(
                EventBuilder.create(EventType.SPEECH_ENDED)
                        .set("TEXT", "Just to fake how to start a conversation")
                        .build());


    }

    private void stop () {
        this.audioEventLoop.setCapturing(false);
    }

    private void subscribe(EventBus<Event> eventBus, List<EventHandler> handlers){
        for (EventHandler handler: handlers){
            eventBus.subscribe(handler);
        }
    }

    public static void main(String[] args) throws Exception {
        FakeMain instance = new FakeMain();
        instance.init();

        Thread.sleep(300000);

        instance.stop();
        System.out.println("FakeMain ended");

    }
}
