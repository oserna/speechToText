
package hello.async.bus;

import hello.async.EventType;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * MUTABLE Event (or message) object that passed from publisher to consumer.
 * Be aware of possible message values mutability, do not try to edit this values in consumer.
 * Each event may contain key-value(object) map.
 *
 * Keep in mind that Event is not serializable because it should be used only in one active instance.
 */
public class Event {

    private final EventType type;

    private final Map<String, Object> properties = new HashMap<>(8);

    private boolean locked;

    /**
     * Create message with appropriate type.
     *
     * @param type Message type, event bus will use type while choosing appropriate consumers.
     */
    public Event(EventType type) {
        this.type = Objects.requireNonNull(type);
    }

    /**
     * Create event with provided properties.
     *
     * @param type Message type, event bus will use type while choosing appropriate consumers.'
     * @param properties Event properties
     */
    public Event(EventType type, Map<String, Object> properties) {
        this.type = Objects.requireNonNull(type);;
        this.properties.putAll(Objects.requireNonNull(properties));
    }

    /**
     * Return message type.
     * Event bus will use type while choosing appropriate consumers.
     */
    public EventType getType() {
        return type;
    }

    protected void lock() {
        this.locked = true;
    }

    private void isLocked() {
        if (locked) {
            throw new UnsupportedOperationException("You can not call any setters on "
                + getClass().getSimpleName() + " instance after event was published.");
        }
    }

    /**
     * Return true if event has any properties (key-value maps is not empty).
     */
    public boolean hasProperties() {
        return properties.isEmpty();
    }

    /**
     * Return all keys associated with key-value properties of current event.
     */
    public Collection<String> getKeys() {
        return properties.keySet();
    }

    /**
     * Associate property value with event.
     *
     * @param key Property key
     * @param value Property value
     * @throws UnsupportedOperationException If this method will be called during event processing
     */
    public void set(String key, Object value) throws UnsupportedOperationException {
        isLocked();
        properties.put(key, value);
    }

    /**
     * Return message property value casted to class if any.
     *
     * @param key Property key in map
     * @param cls Expected value class
     * @return Result will be empty if value does not exist or wrong class to cast
     */
    public <T> Optional<T> getValue(String key, Class<T> cls) {
        return Optional.ofNullable(get(key, cls));
    }

    /**
     * Return message property value casted to class if any.
     *
     * @param key Property key in map
     * @param cls Expected value class
     * @return Will return matched value or null (if value does not exist or wrong class)
     */
    public <T> T get(String key, Class<T> cls) {
        Object val = properties.get(key);
        if (val == null) {
            return null;
        }

        if (cls.isInstance(val)) {
            return cls.cast(val);
        }
        return null;
    }

    /**
     * Return message property value casted to class or default value.
     *
     * @param key Property key in map
     * @param cls Expected value class
     * @return Will return matched value or null (if value does not exist or wrong class)
     *
     */
    public <T> T getOr(String key, Class<T> cls, T defaultValue) {
        T val = get(key, cls);
        return val == null ? defaultValue : val;
    }

    /**
     * Return string representation of value using toSting method.
     *
     * @param key Property key
     * @return String or null if not value
     */
    public String getString(String key) {
        Object val = properties.get(key);

        return val == null ? null : val.toString();
    }

    /**
     * Return string representation of value using toSting method or default String.
     *
     * @param key Property key
     * @return String or default value
     */
    public String getStringOr(String key, String defaultValue) {
        String str = getString(key);
        return str == null ? defaultValue : str;
    }

    public static void main(String[] args) {

        byte [] array = new byte[1024];

        System.out.println(array.getClass().getSimpleName());

        if (byte[].class.isInstance(array)) {
            System.out.println("true");
        }
    }
}
