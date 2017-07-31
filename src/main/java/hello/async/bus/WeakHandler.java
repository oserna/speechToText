package hello.async.bus;

import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;

/**
 * Provide weak link wrapper for handler class and expose some generic handlers methods.
 */
class WeakHandler extends WeakReference<EventHandler> {

    private final int hash;

    WeakHandler(EventHandler handler, ReferenceQueue q) {
        super(handler, q);
        hash = handler.hashCode();
    }

    @Override
    public int hashCode() {
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof WeakHandler)) {
            return false;
        }

        Object t = this.get();
        Object u = ((WeakHandler)obj).get();
        if (t == u) {
            return true;
        }
        if (t == null || u == null) {
            return false;
        }
        return t.equals(u);
    }
}
