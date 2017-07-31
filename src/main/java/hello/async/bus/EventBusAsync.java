package hello.async.bus;


import java.lang.ref.ReferenceQueue;
import java.util.Collections;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Async event bus that will run each event/handler call in separate thread.
 * By default using CachedThreadPool to run handlers.
 */
public class EventBusAsync<E extends Event> implements EventBus<E> {


    private final Thread eventQueueThread;

    private final Queue<E> eventsQueue = new ConcurrentLinkedQueue<>();

    private final ReferenceQueue gcQueue = new ReferenceQueue();

    private final Set<WeakHandler> handlers = Collections.newSetFromMap(new ConcurrentHashMap<WeakHandler, Boolean>());

    private final ExecutorService handlersExecutor;

    /**
     * Create new EventBus instance with default presets.
     */
    public EventBusAsync() {
        this(Executors.newCachedThreadPool());
    }

    /**
     * Create instance with customer ExecutorService for event handlers.
     *
     * @param handlersExecutor Will be used to run event handler processing for each event
     */
    public EventBusAsync(ExecutorService handlersExecutor) {
        this.handlersExecutor = handlersExecutor;
        eventQueueThread = new Thread(this::eventsQueue, "EventQueue handlers thread");
        eventQueueThread.setDaemon(true);
        eventQueueThread.start();
    }

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
        event.lock();
        eventsQueue.add(event);
    }

    @Override
    public boolean hasPendingEvents() {
        return !eventsQueue.isEmpty();
    }

    private void eventsQueue() {
        while (true) {
            WeakHandler wh;
            while ((wh = (WeakHandler)gcQueue.poll()) != null) {
                handlers.remove(wh);
            }

            E event = eventsQueue.poll();
            if (event != null) {
                notifySubscribers(event);
            }
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
                        handlersExecutor.submit(() -> {
                            runHandler(eh, event);
                        });
                    }
                } else if (eh.getType().equals(event.getType())) {
                    handlersExecutor.submit(() -> {
                        runHandler(eh, event);
                    });
                }
            } catch (Throwable th) {
                System.out.println("Handler notify fail on event " + event.getType() + ". " + th.getMessage());
                th.printStackTrace();
            }
        }
    }

    private void runHandler(EventHandler eh, E event) {
        try {
            eh.handle(event);
        } catch (Throwable th) {
            System.out.println("Handler fail on event " + event.getType() + ". " + th.getMessage());
            th.printStackTrace();
        }
    }
}
