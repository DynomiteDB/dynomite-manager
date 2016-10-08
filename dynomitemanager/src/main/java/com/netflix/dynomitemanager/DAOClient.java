/*
 *
 * Copyright 2016 DynomiteDB
 * All rights reserved.
 *
 */

package com.netflix.dynomitemanager.identity;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.StatusRuntimeException;

import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * A simple client that requests a DAO operations from the {@link DBServer}.
 */
public class DAOClient {
  private static final Logger logger = Logger.getLogger(DAOClient.class.getName());

  private final ManagedChannel channel;
  private final DAOGrpc.DAOBlockingStub blockingStub;

  /** Construct client connecting to DB server at {@code host:port}. */
  public DAOClient(String host, int port) {
    channel = ManagedChannelBuilder.forAddress(host, port)
        // Channels are secure by default (via SSL/TLS). For the example we disable TLS to avoid
        // needing certificates.
        .usePlaintext(true)
        .build();
    blockingStub = DAOGrpc.newBlockingStub(channel);
  }

  public void shutdown() throws InterruptedException {
    channel.shutdown().awaitTermination(5, TimeUnit.SECONDS);
  }

  /** createInstanceEntry. */
  public void createInstanceEntry() {
    logger.info("Will try to create an instance entry ...");
    // Here we need to set all the app instance values.
    // For now I am hard-coding the values for app instance
    AppsInstanceFields request = AppsInstanceFields.newBuilder().setCNID("sampleId")
                                                                .setCNAPPID("sampleAppId")
                                                                .setCNAZ("sampleAz")
                                                                .setCNDC("sampleDc")
                                                                .setCNINSTANCEID("sampleInsId")
                                                                .setCNHOSTNAME("sampleHostName")
                                                                .setCNEIP("sampleEip")
                                                                .setCNTOKEN("sampleToken")
                                                                .setCNLOCATION("sampleLoc")
                                                                .setCNUPDATETIME("sampleUpdtTime")
                                                                .setVolumes("sampleVolumes").build();
    CRUDResponse response;
    try {
      response = blockingStub.createInstanceEntry(request);
    } catch (StatusRuntimeException e) {
      logger.log(Level.WARNING, "RPC failed: {0}", e.getStatus());
      return;
    }
    logger.info("Info: " + response.getResponse());
  }

  /**
   * DAO server. If provided, the first element of {@code args}
   */
  public static void main(String[] args) throws Exception {
    DAOClient client = new DAOClient("localhost", 50051);
    try {
      /* Access a service running on the local machine on port 50051 */
      client.createInstanceEntry();
    } finally {
      client.shutdown();
    }
  }
}