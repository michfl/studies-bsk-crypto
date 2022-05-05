package pl.edu.pg.eti.ksr.project.observer;

/**
 * Observer pattern implementation.
 * Subject subscriber.
 */
public interface Observer {

    /**
     * Method called on all observers when event of a given type appears.
     * @param type type of the event
     * @param o data related to event
     */
    void update(Subject.NewsType type, Object o);
}
