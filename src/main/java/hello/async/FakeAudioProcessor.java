package hello.async;

import hello.async.bus.Event;
import hello.async.bus.EventBuilder;
import hello.async.bus.EventBus;
import hello.async.bus.EventHandler;

import java.util.ArrayList;
import java.util.List;

public class FakeAudioProcessor {

    private List<EventHandler> eventHandlers = new ArrayList<>();

    private int counter = 0;
    private EventBus<Event> eventBus;

    public FakeAudioProcessor(EventBus<Event> eventBus) {
        this.eventBus = eventBus;
        this.eventHandlers.add(new VoiceCapturedHandler());
    }

    public void intentRecognition(){

        counter ++;

        try {
            Thread.sleep(3000);
            System.out.println("FakeResponse after three seconds");
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        if (counter == 5) {

            eventBus.publish(
                    EventBuilder.create(EventType.CONVERSATION_ENDED)
                            .set("TEXT", "Bye, bye monkey")
                            .build());

            return;
        }


        eventBus.publish(
                EventBuilder.create(EventType.VOICE_CHUNK_ANALYZED)
                        .set("TEXT", "bla, bla, just keep going the conversation")
                        .build());

    }

    public class VoiceCapturedHandler implements EventHandler<Event> {

        public EventType getType() {
            return EventType.VOICE_CHUNK_RECORDED;
        }
        public boolean canHandle(EventType eventType) {
            return true; // As long as getType() return not null this method is not called at all
        }
        public void handle(Event event) {

            System.out.println("FakeAudioProcessor got an event: " + event.getType());

            byte[] recorded = event.get("AUDIO", byte[].class);

            intentRecognition();

        }
    }

    public List<EventHandler> getEventHandlers() {
        return eventHandlers;
    }
}
