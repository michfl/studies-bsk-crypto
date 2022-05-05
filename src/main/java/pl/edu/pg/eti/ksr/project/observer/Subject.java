package pl.edu.pg.eti.ksr.project.observer;

/**
 * Observer pattern implementation.
 * News agency. Informs subscribers about an event.
 */
public interface Subject {

    /**
     * All possible news types.
     */
    enum NewsType {

        // State of the TCP manager has been changed
        STATE_CHANGE,

        // New message has been received and added to the communication buffer
        NEW_MESSAGE
    }

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
     * @param type type of the event
     * @param o information correlated to the event
     */
    void notifyObs(NewsType type, Object o);
}
