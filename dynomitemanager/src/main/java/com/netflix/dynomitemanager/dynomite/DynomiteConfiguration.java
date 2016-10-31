package com.netflix.dynomitemanager.dynomite;

import com.google.inject.ImplementedBy;

/**
 * Get the Dynomite configuration properties provided by Archaius.
 */
@ImplementedBy(DynomiteConfigurationRetriever.class)
public interface DynomiteConfiguration {

    // Ports
    // =====

    /**
     * Get the client port used by Redis (i.e. RESP) clients to query Dynomite.
     * @return the client port
     */
    int getClientPort();

    /**
     * Get the peer-to-peer port used by Dynomite to communicate with other Dynomite nodes.
     * @return the peer-to-peer port used for intra-cluster communication
     */
    int getPeerPort();

    // Memory usage
    // ============

    /**
     * Get the mbuf (memory buffer) size.
     * @return the memory buffer (mbuf) size
     */
    int getMbufSize();

    /**
     * Get the maximum number of allocated messages.
     * @return the maximum number of allocated messages
     */
    int getAllocatedMessages();

    // REST API
    // ========

    /**
     * Get the base URL for the Dynomite REST API.
     * @return the base URL for the Dynomite REST API
     */
    String getApiUrl();

    /**
     * Get the URL to set Dynomite's state to normal mode.
     * @return the URL to the set state to normal API endpoint
     */
    String getApiSetStateNormal();

    /**
     * Get the URL to set Dynomite's state to resuming mode.
     * @return the URL to the set state to resuming API endpoint
     */
    String getApiSetStateResuming();

    /**
     * Get the URL to set Dynomite's state to write-only mode.
     * @return the URL to the set state to write-only API endpoint
     */
    String getApiSetStateWritesOnly();

    /**
     * Get the URL to the API to set the read CL.
     * @return full url to the set read CL API
     */
    String getApiSetReadConsistency();

    /**
     * Get the URL to the API to set the write CL.
     * @return full url to the set write CL API
     */
    String getApiSetWriteConsistency();

    // Consistency level
    // =================

    /**
     * Get the read consistency level (CL). Read CL is a cluster-wide setting.
     * @return the read consistency level
     */
    String getReadConsistency();

    /**
     * Get the write consistency level (CL). Write CL is a cluster-wide setting.
     * @return the write consistency level
     */
    String getWriteConsistency();

    // Scripts
    // =======

    /**
     * Get the full path to the Dynomite start script.
     * @return the full path to the Dynomite init start script
     */
    String getStartScript();

    /**
     * Get the full path to the Dynomite stop script.
     * @return the full path to the Dynomite init stop script
     */
    String getStopScript();

    // Misc settings
    // =============

    /**
     * Get the seed provider that supplies Dynomite with the cluster topology.
     * @return the name of the seed provider
     */
    String getSeedProvider();

    /**
     * Get the dynomite process name.
     * @return the dynomite process name
     */
    String getProcessName();

}