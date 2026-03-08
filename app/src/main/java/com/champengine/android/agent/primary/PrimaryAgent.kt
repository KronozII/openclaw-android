package com.champengine.android.agent.primary

// ── AgentState ────────────────────────────────────────────────────
// Reflects server-side connection state, not on-device model state.
sealed class AgentState {
    object Idle        : AgentState()
    object Connecting  : AgentState()
    data class Ready(val model: String) : AgentState()
    data class Generating(val partial: String) : AgentState()
    data class Frozen(val reason: String) : AgentState()
    data class Error(val message: String) : AgentState()
}
