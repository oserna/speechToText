package hello.async.bus;

/**
 * Generic event bus interface.
 * I assume that there can be several possible implementations with different approach to
 * event/threads handling.
 */
public interface EventBus<E extends Event> {

    /**
     * Subscribe consumer to the event bus using weak link.
     *
     * Subscribing same object twice should not affect how many times subscriber will
     * be called per one event.
     *
     * @param subscriber The object to subscribe to the event bus.
     */
    void subscribe(EventHandler<E> subscriber);

    /**
     * Removes the specified consumer from the event bus subscription list.
     * Once removed, the specified object will no longer receive events posted to the
     * event bus.
     *
     * @param subscriber The object previous subscribed to the event bus.
     */
    void unsubscribe(EventHandler<E> subscriber);

    /**
     * Sends a event (message) to the bus which will be propagated to the appropriate subscribers (handlers).
     *
     * There is no specification given as to how the messages will be delivered,
     * and should be determine in each implementation.
     *
     * @param event Event to publish
     */
    void publish(E event);

    /**
     * Indicates whether the bus has pending events to publish. Since message/event
     * delivery can be asynchronous (on other threads), the method can be used to
     * start or stop certain actions based on all the events having been published.
     * I.e. perhaps before an application closes, etc.
     *
     * @return True if events are still being delivered.
     */
    boolean hasPendingEvents();

}
