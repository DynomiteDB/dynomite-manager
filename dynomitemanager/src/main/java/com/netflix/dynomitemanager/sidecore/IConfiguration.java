/**
 * Copyright 2014 Netflix, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package com.netflix.dynomitemanager.sidecore;

import java.util.List;

/**
 * Dynomite Manager configuration.
 */
public interface IConfiguration {

	public void initialize();

	/**
	 * Get the full path to the Dynomite init start script. The default start script locations are:
	 * <ul>
	 * <li>Netflix: /apps/dynomite/bin/launch_dynomite.sh
	 * <li>DynomiteDB: /etc/init.d/dynomitedb-dynomite start
	 * </ul>
	 * @return full path to the Dynomite init start script
	 */
	public String getDynomiteStartupScript();

	/**
	 * Get the full path to the Dynomite init stop script. The default stop script locations are:
	 * <ul>
	 * <li>Netflix: /apps/dynomite/bin/kill_dynomite.sh
	 * <li>DynomiteDB: /etc/init.d/dynomitedb-dynomite stop
	 * </ul>
	 * @return full path to the Dynomite init stop script
	 */
	public String getDynomiteStopScript();

	/**
	 * @return Script that starts the storage layer
	 */
	public String getStorageStartupScript();

	/**
	 * Get the full path to the storage engine's stop script.
	 * @return full path to the stop script for the Redis or Memcached storage engine
	 */
	public String getStorageStopScript();

	/**
	 * Get the name of the Dynomite cluster.
	 * @return {@link String} Dynomite cluster name
	 */
	public String getClusterName();

	/**
	 * @return Zone (or zone for AWS)
	 */
	public String getZone();

	/**
	 * @return Local hostname
	 */
	public String getHostname();

	/**
	 * @return Get instance name (for AWS)
	 */
	public String getInstanceName();

	/**
	 * @return Get the Region name
	 */
	public String getRegion();

	//public void setRegion(String region);

	/**
	 * @return Get the Data Center name (or region for AWS)
	 */
	public String getRack();

	/**
	 * Get a list of racks (AWS AZs) in this server's data center (AWS Region).
	 * @return {@link List} of racks (or AZs) in this server's data center
	 */
	public List<String> getRacks();

	/**
	 * Amazon specific setting to query ASG Membership
	 */
	public String getASGName();

	/**
	 * Get the security group associated with nodes in this cluster
	 */
	public String getACLGroupName();

	/**
	 * @return Get host IP
	 */
	public String getHostIP();

	/**
	 * @return Bootstrap cluster name (depends on another Cassandra cluster)
	 */
	public String getBootClusterName();

	/**
	 * Get the seed provider. Seed provider must be one of:
	 * <ul>
	 * <li>dynomitemanager_provider: Dynomite Manager provides the complete topology, which service discovery provides list of health nodes.
	 * <li>simple_provider: Static list of seeds provided via the seeds.list file.
	 * <li>florida_provider: Florida provides the complete topology.
	 * <li>dns_provider: DNS provides the complete topology. This option requires the ability to update DNS as nodes are added/removed from the cluster.
	 * </ul>
	 * @return {@link String} the name of seed provider
	 */
	public String getSeedProvider();

	/**
	 * @return Process Name
	 */
	public String getProcessName();

	public String getReadConsistency();

	public String getWriteConsistency();

	/**
	 * Get the port that Dynomite listens on for client connections.
	 * @return {@link int} the port that Dynomite listens on for client connections
	 */
	public int getClientListenPort();

	/**
	 * Get the IP address and port that Dynomite listens on for client connections. This endpoint accepts clients
	 * that send/receive the Redis protocol. Requests send to this port are forwarded to the backend data store. Any
	 * Redis client, such as Jedis or redis-cli, may communicate with Dynomite via the listen address.
	 * @return {@link String} the IP address and port that Dynomite listens on for client connections
	 */
	public String getClientListenAddress();

	/**
	 * Get the port that Dynomite listens on for peer-to-peer communication (i.e. internal cluster communication,
	 * such as gossip).
	 * @return {@link int} the port that Dynomite listens on for peer connections from other Dynomite nodes
	 */
	public int getPeerListenPort();

	/**
	 * Get the IP address and port that Dynomite listens on for peer connections. This endpoint accepts connections
	 * from other Dynomite nodes for internal cluster communication, such as gossip.
	 * @return {@link String} the IP address and port that Dynomite listens on for peer connections
	 */
	public String getPeerListenAddress();

	/**
	 * Get the SSL port that Dynomite listens on for secure peer-to-peer communication (i.e. internal cluster
	 * communication, such as gossip).
	 * @return {@link int} the SSL port that Dynomite listens on for peer connections from other Dynomite nodes
	 */
	public int getPeerListenPortSSL();

	/**
	 * Get the full path to the dynomite.yaml file.
	 * @return {@link String} full path to the dynomite.yaml file
	 */
	public String getDynomiteYaml();

	public boolean getAutoEjectHosts();

	/**
	 * Get the token distribution type, which must be one of:
	 * <ul>
	 * <li>vnode (default)
	 * <li>ketama
	 * <li>modula
	 * <li>random
	 * <li>single
	 * </ul>
	 *
	 * Token distribution type defines how hashed keys (aka tokens) are distributed across Dynomite nodes.
	 * @return
	 */
	public String getTokensDistribution();

	/**
	 * Get the hashing algorithm that is used to generate a token.
	 * @return {@link String} the name of the hashing algorithm used to generate a token
	 */
	public String getTokensHash();

	/**
	 * Get the length of time in milliseconds between gossip rounds.
	 * @return {@link int} delay between gossip rounds in milliseconds
	 */
	public int getGossipInterval();

	/**
	 * Get the connections preconnection setting. If true, then Dynomite will preconnect to the backend data store.
	 * @return {@link boolean} true if Dynomite should preconnect to the backend data store, false if not
	 */
	public boolean getPreconnect();

	public int getServerRetryTimeout();

	/**
	 * Get the request timeout in milliseconds.
	 * @return {@link int} request timeout in milliseconds
	 */
	public int getRequestTimeout();

	public String getTokens();

	public String getMetadataKeyspace();

	/**
	 * Get the backend data store type.
	 * @return {@link int} the backend data store type
	 */
	public int getDataStoreType();

	/**
	 * Returns whether or not the Dynomite cluster is configured for multiple data centers (DCs). Returns true when
	 * configured for multi-DC.
	 * @return {@link boolean} true if the Dynomite cluster is multi-DC (AWS Regions)
	 */
	public boolean isMultiDC();

	public String getSecuredOption();

	public boolean isWarmBootstrap();

	public boolean isForceWarm();

	public boolean isHealthCheckEnable();

	public int getAllowableBytesSyncDiff();

	public int getMaxTimeToBootstrap();

	/** The max percentage of system memory to be allocated to the Dynomite fronted data store. */
	public int getStorageMemPercent();

	public int getMbufSize();

	public int getAllocatedMessages();

	// VPC
	public boolean isVpc();

	/**
	 * @return the VPC id of the running instance.
	 */
	public String getVpcId();

	/*
	 * @return the Amazon Resource Name (ARN) for EC2 classic.
	 */
	public String getClassicAWSRoleAssumptionArn();

	/*
	 * @return the Amazon Resource Name (ARN) for VPC.
	 */
	public String getVpcAWSRoleAssumptionArn();

	/*
	 * @return cross-account deployments
	 */
	public boolean isDualAccount();

	// Backup and Restore

	public String getBucketName();

	public String getBackupLocation();

	public boolean isBackupEnabled();

	public boolean isRestoreEnabled();

	public String getBackupSchedule();

	public int getBackupHour();

	public String getRestoreDate();

	// Persistence

	public String getPersistenceLocation();

	public boolean isPersistenceEnabled();

	public boolean isAof();

	// Cassandra
	public String getCassandraKeyspaceName();

	public int getCassandraThriftPortForAstyanax();

	public String getCommaSeparatedCassandraHostNames();

	public boolean isEurekaHostSupplierEnabled();

	// Redis
	// =====

	/**
	 * Get the full path to the redis.conf configuration file. The default path to redis.conf is:
	 * <ul>
	 * <li>Netflix: /apps/nfredis/conf/redis.conf
	 * <li>DynomiteDB: /etc/dynomitedb/redis.conf
	 * </ul>
	 * @return the {@link String} full path to the redis.conf configuration file
	 */
	public String getRedisConf();

	/**
	 * Get the name of the AOF file including extension.
	 * @return the {@link String} AOF filename
	 */
	public String getRedisAofFilename();

	/**
	 * Get the name of the RDB file including extension.
	 * @return the {@link String} RDB filename
	 */
	public String getRedisRdbFilename();
}
