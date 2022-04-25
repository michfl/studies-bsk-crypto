package pl.edu.pg.eti.ksr.project.network;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

public class ConnectionManager {

    @Getter
    @Setter
    private NetworkManager networkManager;

    @Getter
    private List<byte[]> buffer;

    //public boolean send(ConfigurationDTO conf, )
}
