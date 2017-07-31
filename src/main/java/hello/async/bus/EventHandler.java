package hello.async.bus;

import hello.async.EventType;

/**
 * Any subtype of this can handle messages in EventBus.
 * There is no restriction on how many handlers will be subscribed to one or another event type.
 * Keep in mind that handler will be subscribed to EventBush using weak link.
 */
public interface EventHandler<E extends Event> {

    /**
     * Return exact event type that must be handled by this handler.
     * Can return null in this case {@link EventHandler#canHandle(java.lang.String)}
     * will be called each time to decide if particular message should be handled by current consumer.
     *
     * @return Type name or null
     */
    EventType getType();

    /**
     * In a case if {@link EventHandler#getType()} return null this method will be called to
     * check if current event type can be handled here.
     * If getType() result is not null this method will not be called.
     *
     * @param eventType Event type
     * @return True if event type can be handled or False.
     */
    boolean canHandle(EventType eventType);

    /**
     * This method should handle event of appropriate type.
     */
    void handle(E event);
}
