package pl.edu.pg.eti.ksr.project.observer;

/**
 * Observer pattern implementation.
 * News agency. Informs subscribers about an event.
 */
public interface Subject {

    /**
     * Subscribing to a news agency.
     * @param observer new subscriber
     */
    void attach(Observer observer);

    /**
     * Unsubscribing from a news agency.
     * @param observer current subscriber
     */
    void detach(Observer observer);

    /**
     * Notify all subscribers about an event.
     * @param o information correlated to the event
     */
    void notifyObs(Object o);
}
