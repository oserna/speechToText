package hello.async.speak;

import hello.async.bus.Event;
import hello.async.bus.EventBuilder;
import hello.async.bus.EventBus;
import hello.async.bus.EventHandler;
import hello.async.EventType;

import java.util.ArrayList;
import java.util.List;

public class ConsoleSpeaker implements Speaker {

    private EventBus<Event> eventBus;

    private List<EventHandler> eventHandlers = new ArrayList<>();

    public ConsoleSpeaker(EventBus<Event> eventBus) {

        this.eventBus = eventBus;

        this.eventHandlers.add(new TextToSpeechdHandler());
    }

    @Override
    public void speak(String text) {
        System.out.println("Speaker: "+text);

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

            System.out.println("GingerSpeaker got an event " + event.getType());

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
