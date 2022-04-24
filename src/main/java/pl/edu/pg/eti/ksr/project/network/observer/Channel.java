package pl.edu.pg.eti.ksr.project.network.observer;

public interface Channel {

    enum NewsType {
        STATE_CHANGE,
        NEW_MESSAGE
    }

    void update(NewsType type, Object o);
}
