/*
 *
 * Copyright 2016 DynomiteDB
 * All rights reserved.
 *
 */

package com.netflix.dynomitemanager.identity;

import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.grpc.stub.StreamObserver;

import java.io.IOException;
import java.util.logging.Logger;

/**
 * Server that manages startup/shutdown of a {@code DAOServer} server.
 */
public class DAOServer {
  private static final Logger logger = Logger.getLogger(DAOServer.class.getName());

  /* The port on which the server should run */
  private int port = 50051;
  private Server server;

  private void start() throws IOException {
    server = ServerBuilder.forPort(port)
        .addService(new DAOImpl())
        .build()
        .start();
    logger.info("Server started, listening on " + port);
    Runtime.getRuntime().addShutdownHook(new Thread() {
      @Override
      public void run() {
        // Use stderr here since the logger may have been reset by its JVM shutdown hook.
        System.err.println("*** shutting down gRPC server since JVM is shutting down");
        DAOServer.this.stop();
        System.err.println("*** server shut down");
      }
    });
  }

  private void stop() {
    if (server != null) {
      server.shutdown();
    }
  }

  /**
   * Await termination on the main thread since the grpc library uses daemon threads.
   */
  private void blockUntilShutdown() throws InterruptedException {
    if (server != null) {
      server.awaitTermination();
    }
  }

  /**
   * Main launches the server from the command line.
   */
  public static void main(String[] args) throws IOException, InterruptedException {
    final DAOServer server = new DAOServer();
    server.start();
    server.blockUntilShutdown();
  }

  private class DAOImpl extends DAOGrpc.DAOImplBase {
    @Override
    public void createInstanceEntry(AppsInstanceFields req, StreamObserver<CRUDResponse> responseObserver) {
      //try {

        CRUDResponse reply = CRUDResponse.newBuilder().setResponse("Here we got the values of " +                            
                              "App Instance to insert in DB. The values are " + 
                              req.getCNID() +
                              req.getCNAPPID() +
                              req.getCNAZ() +
                              req.getCNDC() +
                              req.getCNINSTANCEID() +
                              req.getCNHOSTNAME() +
                              req.getCNEIP() +
                              req.getCNTOKEN() +
                              req.getCNLOCATION() +
                              req.getCNUPDATETIME() +
                              req.getVolumes()).build();
        responseObserver.onNext(reply);
        responseObserver.onCompleted();
     // } catch(IOException ex){
        //CRUDResponse reply = CRUDResponse.newBuilder().setResponse("The entry for App Instance " + 
                  //            "with values as " + req.getCNID() + ", " + req.getCNAPPID() + ", etc " +
                    //          "cannot be created. Error: " + ex.toString()).build();
        responseObserver.onNext(reply);
        responseObserver.onCompleted();                      
     // }
    }
  }
}