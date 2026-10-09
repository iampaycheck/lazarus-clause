package com.iampaycheck.ghostcore.ghost;

/** What the Ghost is doing; drives where it hovers and how its shell animates. */
public enum GhostState {
    IDLE,
    SCANNING,
    REVIVING,
    TRANSMAT;

    private static final GhostState[] VALUES = values();

    public static GhostState byId(int id) {
        return id >= 0 && id < VALUES.length ? VALUES[id] : IDLE;
    }
}
