package hello.async.recognition;

import hello.async.EventType;
import hello.async.bus.Event;
import hello.async.bus.EventBus;
import hello.async.bus.EventHandler;

import java.util.ArrayList;
import java.util.List;

public class ConsoleSpeechRecognition implements SpeechRecognition {

    private EventBus<Event> eventBus;

    private List<EventHandler> eventHandlers = new ArrayList<>();

    public ConsoleSpeechRecognition(EventBus<Event> eventBus) {
        this.eventBus = eventBus;

        this.eventHandlers.add(new EndConversationdHandler());
    }

    @Override
    public void start() {}

    @Override
    public void pause() {
        System.out.println("ConsoleSpeechRecognition paused");
    }

    @Override
    public void resume() {
        System.out.println("ConsoleSpeechRecognition resumed");
    }

    @Override
    public void stop() {}

    public class EndConversationdHandler implements EventHandler<Event> {

        public EventType getType() {return EventType.CONVERSATION_ENDED;}

        public boolean canHandle(EventType eventType) {return true;}

        public void handle(Event event) {

            System.out.println("ConsoleSpeechRecognition got an event " + event.getType());

            String text = event.get("TEXT", String.class);

            pause();

        }

    }

    public List<EventHandler> getEventHandlers() {
        return eventHandlers;
    }
}
