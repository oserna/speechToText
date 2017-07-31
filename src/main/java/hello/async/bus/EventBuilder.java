package hello.async.bus;

import hello.async.EventType;

import java.lang.reflect.Constructor;

/**
 * Builder for Event type.
 * You can create builder for your own event type only by extending it
 * and implementing your own create method.
 * Or you can implement create method in any part of your code.
 */
public class EventBuilder<E extends Event> {

    private final E event;

    public EventBuilder(E event) {
        this.event = event;
    }

    /**
     * Create new builder for event with specific type.
     */
    public static EventBuilder create(EventType eventType) {
        return new EventBuilder(new Event(eventType));
    }

    /**
     * Create new builder for any event subclass with specific type.
     */
    public static <T extends Event> EventBuilder<T> create(Class<T> cls, EventType eventType) {
        try {
            Constructor<T> constructor = cls.getConstructor(EventType.class);
            return new EventBuilder<>(constructor.newInstance(eventType));
        } catch (Exception ex) {
            throw new IllegalArgumentException("Can not create event builder for " + cls + " and type " + eventType, ex);
        }
    }

    /**
     * Put property into event properties.
     */
    public EventBuilder<E> set(String key, Object value) {
        event.set(key, value);
        return this;
    }

    public E build() {
        return this.event;
    }
}
