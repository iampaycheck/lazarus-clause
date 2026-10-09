package com.iampaycheck.ghostcore.client;

import com.iampaycheck.ghostcore.network.GhostSyncPayload;
import com.iampaycheck.ghostcore.network.ScanResultPayload;

/** Client-side landing point for server payloads, plus the latest Ghost snapshot for the HUD. */
public final class ClientPayloads {
    private static GhostSyncPayload state = GhostSyncPayload.EMPTY;

    public static GhostSyncPayload state() {
        return state;
    }

    public static void onSync(GhostSyncPayload payload) {
        state = payload;
    }

    public static void onScan(ScanResultPayload payload) {
        ScanHighlights.start(payload);
    }

    static void reset() {
        state = GhostSyncPayload.EMPTY;
        ScanHighlights.clear();
    }

    private ClientPayloads() {}
}
