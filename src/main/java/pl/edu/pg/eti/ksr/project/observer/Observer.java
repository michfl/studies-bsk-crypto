package pl.edu.pg.eti.ksr.project.observer;

/**
 * Observer pattern implementation.
 * Subject subscriber.
 */
public interface Observer {

    /**
     * Method called on all observers when event appears.
     * @param o data related to event
     */
    void update(Object o);
}
