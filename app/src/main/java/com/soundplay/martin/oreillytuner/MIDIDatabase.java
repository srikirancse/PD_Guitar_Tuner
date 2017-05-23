package com.soundplay.martin.oreillytuner;

/**
 * Created by psbor on 03/16/17.
 */

public class MIDIDatabase {
    private static final MIDIDatabase ourInstance = new MIDIDatabase();

    public static MIDIDatabase getInstance() {
        return ourInstance;
    }

    private MIDIDatabase() {
    }
}
