package hello.async.speak;

import com.sun.speech.freetts.Voice;
import com.sun.speech.freetts.VoiceManager;
import hello.async.EventType;
import hello.async.bus.Event;
import hello.async.bus.EventBuilder;
import hello.async.bus.EventBus;
import hello.async.bus.EventHandler;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import javax.speech.AudioException;
import javax.speech.Central;
import javax.speech.EngineException;
import javax.speech.synthesis.Synthesizer;
import javax.speech.synthesis.SynthesizerModeDesc;

public class BasicSpeaker {


    private List<EventHandler> eventHandlers = new ArrayList<>();

    private EventBus<Event> eventBus = null;


    Synthesizer  synthesizer = null;

    public BasicSpeaker(EventBus<Event> eventBus) {
        this.eventHandlers.add(new TextToSpeechdHandler());
        this.eventBus =  eventBus;
    }

    private void init(){
        System.setProperty("freetts.voices",
                "com.sun.speech.freetts.en.us.cmu_us_kal.KevinVoiceDirectory");


    }

    public void speak(String text){
        try
        {
            VoiceManager voiceManager = VoiceManager.getInstance();
            Voice helloVoice = voiceManager.getVoice("kevin16");

            helloVoice.allocate();
            helloVoice.speak(text);
            helloVoice.deallocate();
        }
        catch(Exception e)
        {
            e.printStackTrace();
        }
    }


    private void close(){

    }

    public class TextToSpeechdHandler implements EventHandler<Event> {

        public EventType getType() {
            return null;
        }

        public boolean canHandle(EventType eventType) {
            if (eventType == EventType.VOICE_CHUNK_ANALYZED
                    || eventType == EventType.CONVERSATION_ENDED) {

                return true;
            }

            return false;
        }

        public void handle(Event event) {

            System.out.println("BasicSpeaker got an event " + event.getType());

            String text = event.get("TEXT", String.class);

            speak(text);

            if (event.getType() == EventType.VOICE_CHUNK_ANALYZED) {
                new Thread(() -> {
                    eventBus.publish(
                            EventBuilder.create(EventType.SPEECH_ENDED)
                                    .set("TEXT", text)
                                    .build());
                }).start();
            }
        }
    }

    public List<EventHandler> getEventHandlers() {
        return eventHandlers;
    }
}