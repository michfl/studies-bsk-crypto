package pl.edu.pg.eti.ksr.project.network.observer;

public interface Channel {

    enum NewsType {

        // State of the TCP manager has been changed
        STATE_CHANGE,

        // New message has been received and added to the communication buffer
        NEW_MESSAGE
    }

    /**
     * Method called on all observers when event of a given type appears.
     * @param type type of the event
     * @param o data related to event
     */
    void update(NewsType type, Object o);
}
