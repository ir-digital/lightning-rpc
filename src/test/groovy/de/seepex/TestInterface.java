package de.seepex;

import java.util.Map;

public interface TestInterface {

    Map getMap();


    default int getInt() {
        return 0;
    }
}
