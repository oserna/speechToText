package hello.async.bus;


import java.lang.ref.ReferenceQueue;
import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Simple event bus with no background threads.
 * All consumers will be called directly during event publishing.
 * You can use it in a cases where event publishing is rare
 * or if there is requirement to use as less threads as possible.
 */
public class EventBusSimple<E extends Event> implements EventBus<E> {

    private final ReferenceQueue gcQueue = new ReferenceQueue();

    private final AtomicInteger processing = new AtomicInteger();

    private final Set<WeakHandler> handlers = Collections.newSetFromMap(new ConcurrentHashMap<WeakHandler, Boolean>());

    @Override
    public void subscribe(EventHandler<E> subscriber) {
        handlers.add(new WeakHandler(subscriber, gcQueue));
    }

    @Override
    public void unsubscribe(EventHandler<E> subscriber) {
        handlers.remove(new WeakHandler(subscriber, gcQueue));
    }

    @Override
    public void publish(E event) {
        if (event == null) {
            return;
        }
        processing.incrementAndGet();
        try {
            event.lock();
            processEvent(event);
        } finally {
            processing.decrementAndGet();
        }
    }

    @Override
    public boolean hasPendingEvents() {
        return processing.get() > 0;
    }

    private void processEvent(E event) {
        WeakHandler wh;
        while ((wh = (WeakHandler)gcQueue.poll()) != null) {
            handlers.remove(wh);
        }
        if (event != null) {
            notifySubscribers(event);
        }
    }

    private void notifySubscribers(E event) {
        for (WeakHandler wh : handlers) {
            EventHandler eh = wh.get();
            if (eh == null) {
                continue;
            }

            try {
                if (eh.getType() == null) {
                    if (eh.canHandle(event.getType())) {
                        eh.handle(event);
                    }
                } else if (eh.getType().equals(event.getType())) {
                    eh.handle(event);
                }
            } catch (Throwable th) {
                System.out.println("Handler fail on event " + event.getType() + ". " + th.getMessage());
                th.printStackTrace();
            }
        }
    }
}
