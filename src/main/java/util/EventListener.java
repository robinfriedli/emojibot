package util;


import core.ElementChangingEvent;
import core.ElementCreatedEvent;
import core.ElementDeletingEvent;

public abstract class EventListener {

    public void elementCreating(ElementCreatedEvent event) {
    }

    public void elementDeleting(ElementDeletingEvent event) {
    }

    public void elementChanging(ElementChangingEvent event) {
    }

}
