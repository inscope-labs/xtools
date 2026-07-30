package com.inscopelabs.abx.xtools.kernel.mode

/**
 * Represents the two mutually exclusive operating modes of xtools.
 * See §1.2 of the development plan.
 */
enum class OperatingMode {
    /** Default mode: xtools governs all data access via its own enable‑switch framework. */
    STANDALONE,

    /**
     * Mode triggered by a valid abx‑server session.
     * In this mode, xtools' standalone data layers are disabled, and all data access
     * is routed exclusively through the abx‑sfm AIDL contract.
     */
    GOVERNED
}
