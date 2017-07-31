package hello.async.recognition;

import com.aldebaran.qi.CallError;
import com.aldebaran.qi.helper.proxies.ALSpeechRecognition;
import hello.async.EventType;
import hello.async.bus.Event;
import hello.async.bus.EventBus;
import hello.async.bus.EventHandler;

import java.util.ArrayList;
import java.util.List;

public class GingerSpeechRecognition implements SpeechRecognition{

    private ALSpeechRecognition speechRecognition;

    private EventBus<Event> eventBus;

    private List<EventHandler> eventHandlers = new ArrayList<>();

    public GingerSpeechRecognition(ALSpeechRecognition speechRecognition, EventBus<Event> eventBus) {
        this.eventBus = eventBus;
        this.speechRecognition = speechRecognition;

        this.eventHandlers.add(new EndConversationdHandler());
    }

    @Override
    public void start() {}

    @Override
    public void pause() {
        try {
            speechRecognition.pause(Boolean.FALSE);
        } catch (CallError callError) {
            callError.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void resume() {
        try {
            speechRecognition.pause(Boolean.TRUE);
        } catch (CallError callError) {
            callError.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void stop() {}

    public class EndConversationdHandler implements EventHandler<Event> {

        public EventType getType() {return EventType.CONVERSATION_ENDED;}

        public boolean canHandle(EventType eventType) {return true;}

        public void handle(Event event) {

            System.out.println("GingerSpeechRecognition got an event " + event.getType());

            String text = event.get("TEXT", String.class);

            pause();

        }

    }

    public List<EventHandler> getEventHandlers() {
        return eventHandlers;
    }

}
