/**
 * Copyright 2016 Netflix, Inc.
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
package com.netflix.dynomitemanager.defaultimpl;

import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.amazonaws.services.ec2.AmazonEC2;
import com.amazonaws.services.ec2.AmazonEC2Client;
import com.amazonaws.services.ec2.model.AvailabilityZone;
import com.amazonaws.services.ec2.model.DescribeAvailabilityZonesResult;
import com.amazonaws.services.ec2.model.DescribeInstancesRequest;
import com.amazonaws.services.ec2.model.DescribeInstancesResult;
import com.amazonaws.services.ec2.model.Instance;
import com.amazonaws.services.ec2.model.Reservation;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.netflix.dynomitemanager.sidecore.IConfigSource;
import com.netflix.dynomitemanager.sidecore.IConfiguration;
import com.netflix.dynomitemanager.sidecore.ICredential;
import com.netflix.dynomitemanager.sidecore.config.InstanceDataRetriever;
import com.netflix.dynomitemanager.sidecore.utils.RetryableCallable;
import com.netflix.dynomitemanager.identity.InstanceEnvIdentity;

/**
 * Define the list of available Dynomite Manager configuration options, then set options based on the environment and
 * an external configuration. Dynomite Manager properties may be provided via the following mechanisms:
 * <ul>
 * <li>Archaius: Excellent option for enterprise deployments as it provides dynamic properties
 * <li>Environment variables: Localized configuration passed in via environment variables
 * <li>Java properties: Localized configuration passed in via the command line in an init scrip
 * </ul>
 */
@Singleton
public class DynomitemanagerConfiguration implements IConfiguration {
	public static final String DM_PREFIX = "dm";

	public static final int MEMCACHED = 0;
	public static final int REDIS = 1;
	public static final int DYNO_PORT = 8102;
	public final static String LOCAL_ADDRESS = "127.0.0.1";

	// Redis
	// =====

	// Full path to redis.conf
	private static final String CONFIG_REDIS_CONF = DM_PREFIX + ".redis.conf";

	// Redis's port. Do NOT expose this port externally. Block via iptables, SGs, or similar.
	public static final int REDIS_PORT = 22122;

	// Full path to Redis init scripts to start/stop redis-server
	private static final String CONFIG_REDIS_START_SCRIPT = DM_PREFIX + ".redis.startscript";
	private static final String CONFIG_REDIS_STOP_SCRIPT = DM_PREFIX + ".redis.stopscript";

	// Enable/disable persistence
	private static final String CONFIG_REDIS_PERSISTENCE_ENABLED = DM_PREFIX + ".redis.persistence.enabled";

	// Persistence type: aof, rdb
	private static final String CONFIG_REDIS_PERSISTENCE_TYPE = DM_PREFIX + ".redis.persistence.type";

	// Directory where the .aof or .rdb file is written
	private static final String CONFIG_REDIS_PERSISTENCE_DIR = DM_PREFIX + ".redis.persistence.directory";

	// AOF and RDB filenames
	private static final String CONFIG_REDIS_AOF_FILENAME = DM_PREFIX + ".redis.aof.filename";
	private static final String CONFIG_REDIS_RDB_FILENAME = DM_PREFIX + ".redis.rdb.filename";

	// Dynomite
	// ========

	// Init scripts to start/stop the dynomite process.
	private static final String CONFIG_DYNOMITE_START_SCRIPT = DM_PREFIX + ".dynomite.init.start";
	private static final String CONFIG_DYNOMITE_STOP_SCRIPT = DM_PREFIX + ".dynomite.init.stop";

	private static final String CONFIG_DYNOMITE_CLUSTER_NAME = DM_PREFIX + ".dynomite.clustername";

	// Seed provider determines how Dynomite learns about the cluster topology
	private static final String CONFIG_DYNOMITE_SEED_PROVIDER = DM_PREFIX + ".dynomite.seed.provider";

	// Port that Dynomite listens on for Redis client connections (ex. redis-cli, Jedis)
	private static final String CONFIG_DYNOMITE_LISTEN_PORT = DM_PREFIX + ".dynomite.port";

	// Ports used for peer communication (Dynomite-to-Dynomite) for internal cluster communication
	private static final String CONFIG_DYNOMITE_PEER_PORT = DM_PREFIX + ".dynomite.peer.port";
	private static final String CONFIG_DYNOMITE_PEER_PORT_SSL = DM_PREFIX + ".dynomite.peer.port.ssl";

	// Rack
	// In AWS, if each ASG maps to exactly one AZ, then set CONFIG_DYNOMITE_USE_ASG_AS_RACK = true and do not use
	// CONFIG_DYNOMITE_RACK. This will set the rack name = asg name. To use CONFIG_DYNOMITE_RACK as the rack name,
	// set a value for CONFIG_DYNOMITE_RACK and set CONFIG_DYNOMITE_USE_ASG_AS_RACK = false
	private static final String CONFIG_DYNOMITE_RACK = DM_PREFIX + ".dynomite.rack";
	private static final String CONFIG_DYNOMITE_USE_ASG_AS_RACK = DM_PREFIX + ".dynomite.asg.rack";

	// Static list of racks within this server's DC. In AWS, leave racks blank and the list of racks will be defined
	// as the AZs within this server's Region. DEFAULT_DYNOMITE_RACKS is set via the call to setDefaultRacksToAZs() in the
	// initialize() method.
	private static final String CONFIG_DYNOMITE_RACKS = DM_PREFIX + ".dynomite.racks";
	private List<String> DEFAULT_DYNOMITE_RACKS = ImmutableList.of();

	// Tokens distribution type (i.e. how are tokens distributed around the cluster) and hash (i.e. which hashing
	// algorithm to use).
	private static final String CONFIG_DYNOMITE_TOKENS_DISTRIBUTION = DM_PREFIX + ".dynomite.tokens.distribution";
	private static final String CONFIG_DYNOMITE_TOKENS_HASH = DM_PREFIX + ".dynomite.tokens.hash";

	// Length of time in ms before the timeout
	private static final String CONFIG_DYNOMITE_REQUEST_TIMEOUT = DM_PREFIX + ".dynomite.request.timeout";

	// Delay between gossip rounds in ms
	private static final String CONFIG_DYNOMITE_GOSSIP_INTERVAL = DM_PREFIX + ".dynomite.gossip.interval";

	// Determines if Dynomite preconnects to the backend data store (i.e. Redis(
	private static final String CONFIG_DYNOMITE_PRECONNECT = DM_PREFIX + ".dynomite.connections.preconnect";

	// Backend storage type: Redis = 1, Memcached = 0
	private static final String CONFIG_DYNOMITE_DATA_STORE = DM_PREFIX + ".dynomite.datastore";

	// Is the Dynomite cluster running in multiple data centers (DCs) / AWS Regions. true == multi DC
	private static final String CONFIG_IS_MULTI_DC = DM_PREFIX + ".dyno.multiple.datacenters";
	private static final String CONFIG_DYNO_HEALTHCHECK_ENABLE = DM_PREFIX + ".dyno.healthcheck.enable";
	// The max percentage of system memory to be allocated to the Dynomite fronted data store.
	private static final String CONFIG_DYNO_STORAGE_MEM_PCT_INT = DM_PREFIX + ".dyno.storage.mem.pct.int";

	private static final String CONFIG_DYNO_MBUF_SIZE = DM_PREFIX + ".dyno.mbuf.size";
	private static final String CONFIG_DYNO_MAX_ALLOC_MSGS = DM_PREFIX + ".dyno.allocated.messages";

	private static final String CONFIG_DYN_PROCESS_NAME = DM_PREFIX + ".dyno.processname";

	// Full path to the dynomite.yaml file
	private static final String CONFIG_DYNOMITE_YAML = DM_PREFIX + ".dynomite.yaml";

	private static final String CONFIG_METADATA_KEYSPACE = DM_PREFIX + ".metadata.keyspace";
	private static final String CONFIG_SECURED_OPTION = DM_PREFIX + ".secured.option";
	private static final String CONFIG_DYNO_AUTO_EJECT_HOSTS = DM_PREFIX + ".auto.eject.hosts";

	// Cassandra Cluster for token management
	private static final String CONFIG_BOOTCLUSTER_NAME = DM_PREFIX + ".bootcluster";
	private static final String CONFIG_CASSANDRA_KEYSPACE_NAME = DM_PREFIX + ".cassandra.keyspace.name";
	private static final String CONFIG_CASSANDRA_THRIFT_PORT = DM_PREFIX + ".cassandra.thrift.port";
	private static final String CONFIG_COMMA_SEPARATED_CASSANDRA_HOSTNAMES =
			DM_PREFIX + ".cassandra.comma.separated.hostnames";

	// Eureka
	// ======
	private static final String CONFIG_IS_EUREKA_HOST_SUPPLIER_ENABLED =
			DM_PREFIX + ".eureka.host.supplier.enabled";

	// Amazon specific
	private static final String CONFIG_ASG_NAME = DM_PREFIX + ".az.asgname";
	private static final String CONFIG_REGION_NAME = DM_PREFIX + ".az.region";
	private static final String CONFIG_ACL_GROUP_NAME = DM_PREFIX + ".acl.groupname";
	private static final String CONFIG_VPC = DM_PREFIX + ".vpc";
	private static final String CONFIG_EC2_ROLE_ASSUMPTION_ARN = DM_PREFIX + ".ec2.roleassumption.arn";
	private static final String CONFIG_VPC_ROLE_ASSUMPTION_ARN = DM_PREFIX + ".vpc.roleassumption.arn";
	private static final String CONFIG_DUAL_ACCOUNT = DM_PREFIX + ".roleassumption.dualaccount";

	// Dynomite Consistency
	private static final String CONFIG_DYNO_READ_CONS = DM_PREFIX + ".dyno.read.consistency";
	private static final String CONFIG_DYNO_WRITE_CONS = DM_PREFIX + ".dyno.write.consistency";

	// warm up
	private static final String CONFIG_DYNO_WARM_FORCE = DM_PREFIX + ".dyno.warm.force";
	private static final String CONFIG_DYNO_WARM_BOOTSTRAP = DM_PREFIX + ".dyno.warm.bootstrap";
	private static final String CONFIG_DYNO_ALLOWABLE_BYTES_SYNC_DIFF = DM_PREFIX + ".dyno.warm.bytes.sync.diff";
	private static final String CONFIG_DYNO_MAX_TIME_BOOTSTRAP = DM_PREFIX + ".dyno.warm.msec.bootstraptime";

	// Backup and Restore
	private static final String CONFIG_BACKUP_ENABLED = DM_PREFIX + ".dyno.backup.snapshot.enabled";
	private static final String CONFIG_BUCKET_NAME = DM_PREFIX + ".dyno.backup.bucket.name";
	private static final String CONFIG_S3_BASE_DIR = DM_PREFIX + ".dyno.backup.s3.base_dir";
	private static final String CONFIG_BACKUP_HOUR = DM_PREFIX + ".dyno.backup.hour";
	private static final String CONFIG_BACKUP_SCHEDULE = DM_PREFIX + ".dyno.backup.schedule";
	private static final String CONFIG_RESTORE_ENABLED = DM_PREFIX + ".dyno.backup.restore.enabled";
	private static final String CONFIG_RESTORE_TIME = DM_PREFIX + ".dyno.backup.restore.date";

	// VPC
	private static final String CONFIG_INSTANCE_DATA_RETRIEVER = DM_PREFIX + ".instanceDataRetriever";

	// Defaults
	private final String DEFAULT_MEMCACHED_START_SCRIPT = "/apps/memcached/bin/memcached";
	private final String DEFAULT_MEMCACHED_STOP_SCRIPT = "/usr/bin/pkill memcached";

	private final String DEFAULT_DYN_PROCESS_NAME = "dynomite";
	private final int DEFAULT_DYN_MEMCACHED_PORT = 11211;

	private final String DEFAULT_METADATA_KEYSPACE = "dyno_bootstrap";
	private final String DEFAULT_SECURED_OPTION = "datacenter";

	// Backup & Restore
	// ----------------

	private static final boolean DEFAULT_BACKUP_ENABLED = false;
	private static final boolean DEFAULT_RESTORE_ENABLED = false;
	//private static final String DEFAULT_BUCKET_NAME = "us-east-1.dynomite-backup-test";
	private static final String DEFAULT_BUCKET_NAME = "dynomite-backup";

	private static final String DEFAULT_BUCKET_FOLDER = "backup";
	private static final String DEFAULT_RESTORE_REPOSITORY_TYPE = "s3";

	private static final String DEFAULT_RESTORE_SNAPSHOT_NAME = "";
	private static final String DEFAULT_RESTORE_SOURCE_REPO_REGION = "us-east-1";
	private static final String DEFAULT_RESTORE_SOURCE_CLUSTER_NAME = "";
	private static final String DEFAULT_RESTORE_REPOSITORY_NAME = "testrepo";
	private static final String DEFAULT_RESTORE_TIME = "20101010";
	private static final String DEFAULT_BACKUP_SCHEDULE = "day";
	private static final int DEFAULT_BACKUP_HOUR = 12;

	// AWS Dual Account
	private static final boolean DEFAULT_DUAL_ACCOUNT = false;

	private static final Logger logger = LoggerFactory.getLogger(DynomitemanagerConfiguration.class);

	private final String CLUSTER_NAME = System.getenv("NETFLIX_APP");
	// TODO: ASG SHOULD NOT BE HARDCODED AS WE CAN QUERY IT FROM THE METADATA API
	private final String AUTO_SCALE_GROUP_NAME = System.getenv("AUTO_SCALE_GROUP");
	private static final String DEFAULT_INSTANCE_DATA_RETRIEVER = "com.netflix.dynomitemanager.sidecore.config.AwsInstanceDataRetriever";
	private static final String VPC_INSTANCE_DATA_RETRIEVER = "com.netflix.dynomitemanager.sidecore.config.VpcInstanceDataRetriever";

	// TODO: Get the ASG the AWS API
	private static String ASG_NAME = System.getenv("ASG_NAME");
	// TODO: Get the Region from the AWS API
	private static String REGION = System.getenv("EC2_REGION");

	private final InstanceDataRetriever retriever;
	private final ICredential provider;
	private final IConfigSource configSource;
	private final InstanceEnvIdentity insEnvIdentity;

	// Cassandra default configuration
	private static final String DEFAULT_BOOTCLUSTER_NAME = "cass_dyno";
	private static final int DEFAULT_CASSANDRA_THRIFT_PORT = 9160; //7102;
	private static final String DEFAULT_CASSANDRA_KEYSPACE_NAME = "dyno_bootstrap";
	private static final String DEFAULT_COMMA_SEPARATED_CASSANDRA_HOSTNAMES = "127.0.0.1";
	private static final boolean DEFAULT_IS_EUREKA_HOST_SUPPLIER_ENABLED = true;

	//= instance identity meta data
	private String RAC, ZONE, PUBLIC_HOSTNAME, PUBLIC_IP, INSTANCE_ID, INSTANCE_TYPE;
	private String NETWORK_MAC;  //Fetch metadata of the running instance's network interface

	//== vpc specific
	private String NETWORK_VPC;  //Fetch the vpc id of running instance

	@Inject
	public DynomitemanagerConfiguration(ICredential provider, IConfigSource configSource,
			InstanceDataRetriever retriever, InstanceEnvIdentity insEnvIdentity) {
		this.retriever = retriever;
		this.provider = provider;
		this.configSource = configSource;
		this.insEnvIdentity = insEnvIdentity;

		RAC = retriever.getRac();
		ZONE = RAC;
		PUBLIC_HOSTNAME = retriever.getPublicHostname();
		PUBLIC_IP = retriever.getPublicIP();

		INSTANCE_ID = retriever.getInstanceId();
		INSTANCE_TYPE = retriever.getInstanceType();

		NETWORK_MAC = retriever.getMac();
		if (insEnvIdentity.isNonDefaultVpc() || insEnvIdentity.isDefaultVpc()) {
			NETWORK_VPC = retriever.getVpcId();
			logger.info("vpc id for running instance: " + NETWORK_VPC);
		}
	}

	private InstanceDataRetriever getInstanceDataRetriever()
			throws InstantiationException, IllegalAccessException, ClassNotFoundException {
		String s = null;

		if (this.insEnvIdentity.isClassic()) {
			s = this.configSource.get(CONFIG_INSTANCE_DATA_RETRIEVER, DEFAULT_INSTANCE_DATA_RETRIEVER);

		} else if (this.insEnvIdentity.isNonDefaultVpc()) {
			s = this.configSource.get(CONFIG_INSTANCE_DATA_RETRIEVER, VPC_INSTANCE_DATA_RETRIEVER);
		} else {
			logger.error("environment cannot be found");
			throw new IllegalStateException(
					"Unable to determine environemt (vpc, classic) for running instance.");
		}
		return (InstanceDataRetriever) Class.forName(s).newInstance();

	}

	/**
	 * Set Dynomite Manager's configuration options.
	 */
	public void initialize() {
		setupEnvVars();
		this.configSource.initialize(ASG_NAME, REGION);
		// Set the default racks to the list of AZs queried via the AWS SDK
		setDefaultRacksToAZs(REGION);
	}

	/**
	 * Set configuration options provided by environment variables or Java properties. Java properties are only used
	 * if the equivalent environment variable is not set.
	 *
	 * Environment variables and Java properties are applied in the following order:
	 * <ol>
	 * <li>Environment variable: Preferred value
	 * <li>Java property: If environment variable is not set, then Java property is used.
	 * </ol>
	 */
	private void setupEnvVars() {
		// Search in java opt properties
		try {
			logger.info("Setting up environmental variables and Java properties.");
			REGION = StringUtils.isBlank(REGION) ? System.getProperty("EC2_REGION") : REGION;
			// Infer from zone
			if (StringUtils.isBlank(REGION))
				REGION = this.retriever.getRac().substring(0, this.retriever.getRac().length() - 1);
			ASG_NAME = StringUtils.isBlank(ASG_NAME) ? System.getProperty("ASG_NAME") : ASG_NAME;
			if (StringUtils.isBlank(ASG_NAME))
				ASG_NAME = populateASGName(REGION, this.retriever.getInstanceId());
			logger.info(String.format("REGION set to %s, ASG Name set to %s", REGION, ASG_NAME));
		} catch (Exception e) {
			throw new RuntimeException(e.getMessage(), e);
		}
	}

	/**
	 * Query Amazon to get ASG name. Currently not available as part of instance info api.
	 */
	private String populateASGName(String region, String instanceId) {
		GetASGName getASGName = new GetASGName(region, instanceId);

		try {
			return getASGName.call();
		} catch (Exception e) {
			logger.error("Failed to determine ASG name.", e);
			return null;
		}
	}

	private class GetASGName extends RetryableCallable<String> {
		private static final int NUMBER_OF_RETRIES = 15;
		private static final long WAIT_TIME = 30000;
		private final String instanceId;
		private final AmazonEC2 client;

		public GetASGName(String region, String instanceId) {
			super(NUMBER_OF_RETRIES, WAIT_TIME);
			this.instanceId = instanceId;
			client = new AmazonEC2Client(provider.getAwsCredentialProvider());
			client.setEndpoint("ec2." + region + ".amazonaws.com");
		}

		@Override
		public String retriableCall() throws IllegalStateException {
			DescribeInstancesRequest desc = new DescribeInstancesRequest().withInstanceIds(instanceId);
			DescribeInstancesResult res = client.describeInstances(desc);

			for (Reservation resr : res.getReservations()) {
				for (Instance ins : resr.getInstances()) {
					for (com.amazonaws.services.ec2.model.Tag tag : ins.getTags()) {
						if (tag.getKey().equals("aws:autoscaling:groupName"))
							return tag.getValue();
					}
				}
			}

			logger.warn("Couldn't determine ASG name");
			throw new IllegalStateException("Couldn't determine ASG name");
		}
	}

	/**
	 * {@inheritDoc}
	 * @return {@inheritDoc}
	 */
	@Override
	public String getDynomiteStartupScript() {
		final String DEFAULT_DYNOMITE_START_SCRIPT = "/apps/dynomite/bin/launch_dynomite.sh";
		return configSource.get(CONFIG_DYNOMITE_START_SCRIPT, DEFAULT_DYNOMITE_START_SCRIPT);
	}

	/**
	 * {@inheritDoc}
	 * @return {@inheritDoc}
	 */
	@Override
	public String getDynomiteStopScript() {
		final String DEFAULT_DYNOMITE_STOP_SCRIPT = "/apps/dynomite/bin/kill_dynomite.sh";
		return configSource.get(CONFIG_DYNOMITE_STOP_SCRIPT, DEFAULT_DYNOMITE_STOP_SCRIPT);
	}

	/**
	 * {@inheritDoc}
	 * @return {@inheritDoc}
	 */
	@Override
	public String getClusterName() {
		final String DEFAULT_CLUSTER_NAME = "dynomite_demo1";
		String clusterName = System.getenv("NETFLIX_APP");

		if (StringUtils.isBlank(clusterName))
			return configSource.get(CONFIG_DYNOMITE_CLUSTER_NAME, DEFAULT_CLUSTER_NAME);

		return clusterName;
	}

	@Override
	public String getZone() {
		return ZONE;
	}

	@Override
	public String getHostname() {
		return PUBLIC_HOSTNAME;
	}

	@Override
	public String getInstanceName() {
		return INSTANCE_ID;
	}

	@Override
	public String getRack() {
		final String DEFAULT_DYNOMITE_RACK = "RAC1";

		if (useASGAsRack()) {
			return getASGName();
		}

		return configSource.get(CONFIG_DYNOMITE_RACK, DEFAULT_DYNOMITE_RACK);
	}

	// Determines if the rack name should be set to the ASG name. Set to true when each ASG maps to a single AZ.
	// This is an AWS specific method.
	private boolean useASGAsRack() {
		return configSource.get(CONFIG_DYNOMITE_USE_ASG_AS_RACK, true);
	}

	/**
	 * Get the list of statically defined racks (AWS AZs) within this server's data center (AWS Region). The default
	 * racks start as an empty list and are then populated by {@link #initialize()}.
	 * @return
	 */
	@Override
	public List<String> getRacks() {
		return configSource.getList(CONFIG_DYNOMITE_RACKS, DEFAULT_DYNOMITE_RACKS);
	}

	public String getRegion() {
		return System.getenv("EC2_REGION") == null ?
				configSource.get(CONFIG_REGION_NAME, "") :
				System.getenv("EC2_REGION");
	}

	@Override
	public String getASGName() {
		return AUTO_SCALE_GROUP_NAME;
	}

	/**
	 * AWS specific method to set the list of racks to the available AZs within this server's Region.
	 * @param region the AWS Region
	 */
	private void setDefaultRacksToAZs(String region) {
		AmazonEC2 client = new AmazonEC2Client(provider.getAwsCredentialProvider());
		client.setEndpoint("ec2." + region + ".amazonaws.com");
		DescribeAvailabilityZonesResult res = client.describeAvailabilityZones();
		List<String> zone = Lists.newArrayList();
		for (AvailabilityZone reg : res.getAvailabilityZones()) {
			if (reg.getState().equals("available"))
				zone.add(reg.getZoneName());
			if (zone.size() == 3)
				break;
		}
		//        DEFAULT_AVAILABILITY_ZONES =  StringUtils.join(zone, ",");
		DEFAULT_DYNOMITE_RACKS = ImmutableList.copyOf(zone);
	}

	@Override
	public String getACLGroupName() {
		return configSource.get(CONFIG_ACL_GROUP_NAME, this.getClusterName());
	}

	@Override
	public String getHostIP() {
		return PUBLIC_IP;
	}

	@Override
	public String getBootClusterName() {
		return configSource.get(CONFIG_BOOTCLUSTER_NAME, DEFAULT_BOOTCLUSTER_NAME);
	}

	/**
	 * {@inheritDoc}
	 * @return {@inheritDoc}
	 */
	@Override
	public String getSeedProvider() {
		final String DEFAULT_DYNOMITE_SEED_PROVIDER = "dynomitemanager_provider";
		return configSource.get(CONFIG_DYNOMITE_SEED_PROVIDER, DEFAULT_DYNOMITE_SEED_PROVIDER);
	}

	@Override
	public String getProcessName() {
		return configSource.get(CONFIG_DYN_PROCESS_NAME, DEFAULT_DYN_PROCESS_NAME);
	}

	/**
	 * {@inheritDoc}
	 * @return {@inheritDoc}
	 */
	@Override
	public String getDynomiteYaml() {
		final String DEFAULT_DYNOMITE_YAML = "/apps/dynomite/conf/dynomite.yml";
		return configSource.get(CONFIG_DYNOMITE_YAML, DEFAULT_DYNOMITE_YAML);
	}

	@Override
	public boolean getAutoEjectHosts() {
		return configSource.get(CONFIG_DYNO_AUTO_EJECT_HOSTS, true);
	}

	/**
	 * {@inheritDoc}
	 * @return {@inheritDoc}
	 */
	@Override
	public String getTokensDistribution() {
		final String DEFAULT_DYNOMITE_TOKENS_DISTRIBUTION = "vnode";
		return configSource.get(CONFIG_DYNOMITE_TOKENS_DISTRIBUTION, DEFAULT_DYNOMITE_TOKENS_DISTRIBUTION);
	}

	/**
	 * {@inheritDoc}
	 * @return {@inheritDoc}
	 */
	@Override
	public String getTokensHash() {
		final String DEFAULT_DYNOMITE_TOKENS_HASH = "murmur";
		return configSource.get(CONFIG_DYNOMITE_TOKENS_HASH, DEFAULT_DYNOMITE_TOKENS_HASH);
	}

	/**
	 * {@inheritDoc}
	 * @return {@inheritDoc}
	 */
	@Override
	public int getClientListenPort() {
		final int DEFAULT_DYNOMITE_LISTEN_PORT = 8102;
		return configSource.get(CONFIG_DYNOMITE_LISTEN_PORT, DEFAULT_DYNOMITE_LISTEN_PORT);
	}

	/**
	 * {@inheritDoc}
	 * @return {@inheritDoc}
	 */
	@Override
	public String getClientListenAddress() {
		return "0.0.0.0:" + getClientListenPort();
	}

	/**
	 * {@inheritDoc}
	 * @return {@inheritDoc}
	 */
	@Override
	public int getPeerListenPort() {
		final int DEFAULT_DYNOMITE_PEER_PORT = 8101;
		return configSource.get(CONFIG_DYNOMITE_PEER_PORT, DEFAULT_DYNOMITE_PEER_PORT);
	}

	/**
	 * {@inheritDoc}
	 * @return {@inheritDoc}
	 */
	@Override
	public String getPeerListenAddress() {
		return "0.0.0.0:" + getPeerListenPort();
	}

	/**
	 * {@inheritDoc}
	 * @return {@inheritDoc}
	 */
	@Override
	public int getPeerListenPortSSL() {
		final int DEFAULT_DYNOMITE_PEER_PORT_SSL = 8101;
		return configSource.get(CONFIG_DYNOMITE_PEER_PORT_SSL, DEFAULT_DYNOMITE_PEER_PORT_SSL);
	}

	/**
	 * {@inheritDoc}
	 * @return {@inheritDoc}
	 */
	@Override
	public int getGossipInterval() {
		final int DEFAULT_DYNOMITE_GOSSIP_INTERVAL = 10000;
		return configSource.get(CONFIG_DYNOMITE_GOSSIP_INTERVAL, DEFAULT_DYNOMITE_GOSSIP_INTERVAL);
	}

	/**
	 * {@inheritDoc}
	 * @return {@inheritDoc}
	 */
	@Override
	public boolean getPreconnect() {
		return configSource.get(CONFIG_DYNOMITE_PRECONNECT, true);
	}

	@Override
	public int getServerRetryTimeout() {
		return 30000;
	}

	/**
	 * {@inheritDoc}
	 * @return {@inheritDoc}
	 */
	@Override
	public int getRequestTimeout() {
		final int DEFAULT_DYNOMITE_REQUEST_TIMEOUT = 5000;
		return configSource.get(CONFIG_DYNOMITE_REQUEST_TIMEOUT, DEFAULT_DYNOMITE_REQUEST_TIMEOUT);
	}

	public String getMetadataKeyspace() {
		return configSource.get(CONFIG_METADATA_KEYSPACE, DEFAULT_METADATA_KEYSPACE);
	}

	@Override
	public String getTokens() {
		// TODO Auto-generated method stub
		return null;
	}

	/**
	 * Get the backend storage type.
	 *
	 * <ul>
	 * <li>0: Memcached
	 * <li>1: Redis
	 * </ul>
	 * @return {@link int} 0 for Memcached, 1 for Redis
	 */
	@Override
	public int getDataStoreType() {
		return configSource.get(CONFIG_DYNOMITE_DATA_STORE, REDIS);
	}

	/**
	 * {@inheritDoc}
	 * @return {@inheritDoc}
	 */
	public boolean isMultiDC() {
		return configSource.get(CONFIG_IS_MULTI_DC, true);
	}

	public String getSecuredOption() {
		return configSource.get(CONFIG_SECURED_OPTION, DEFAULT_SECURED_OPTION);
	}

	public boolean isHealthCheckEnable() {
		return configSource.get(CONFIG_DYNO_HEALTHCHECK_ENABLE, true);
	}

	public boolean isWarmBootstrap() {
		return configSource.get(CONFIG_DYNO_WARM_BOOTSTRAP, false);
	}

	public boolean isForceWarm() {
		return configSource.get(CONFIG_DYNO_WARM_FORCE, false);
	}

	public int getAllowableBytesSyncDiff() {
		return configSource.get(CONFIG_DYNO_ALLOWABLE_BYTES_SYNC_DIFF, 100000);
	}

	public int getMaxTimeToBootstrap() {
		return configSource.get(CONFIG_DYNO_MAX_TIME_BOOTSTRAP, 900000);
	}

	public String getReadConsistency() {
		return configSource.get(CONFIG_DYNO_READ_CONS, "DC_ONE");
	}

	public String getWriteConsistency() {
		return configSource.get(CONFIG_DYNO_WRITE_CONS, "DC_ONE");
	}

	@Override
	public int getStorageMemPercent() {
		return configSource.get(CONFIG_DYNO_STORAGE_MEM_PCT_INT, 85);
	}

	/**
	 * Get the full path to the storage engine's (ex. Redis) start init script. The default start init script is:
	 * <ul>
	 * <li>Netflix: /apps/nfredis/bin/launch_nfredis.sh
	 * <li>DynomiteDB: /etc/init.d/dynomitedb-redis start
	 * </ul>
	 * @return {@link String} full path to the storage engine's (ex. Redis) start init script
	 */
	@Override
	public String getStorageStartupScript() {
		final String DEFAULT_REDIS_START_SCRIPT = "/apps/nfredis/bin/launch_nfredis.sh";

		if (getDataStoreType() == MEMCACHED)
			return DEFAULT_MEMCACHED_START_SCRIPT;

		return configSource.get(CONFIG_REDIS_START_SCRIPT, DEFAULT_REDIS_START_SCRIPT);
	}

	/**
	 * Get the full path to the storage engine's (ex. Redis) stop init script. The default stop init script is:
	 * <ul>
	 * <li>Netflix: /apps/nfredis/bin/kill_redis.sh
	 * <li>DynomiteDB: /etc/init.d/dynomitedb-redis stop
	 * </ul>
	 * @return {@link String} full path to the storage engine's (ex. Redis) stop init script
	 */
	@Override
	public String getStorageStopScript() {
		final String DEFAULT_REDIS_STOP_SCRIPT = "/apps/nfredis/bin/kill_redis.sh";

		if (getDataStoreType() == MEMCACHED)
			return DEFAULT_MEMCACHED_STOP_SCRIPT;

		return configSource.get(CONFIG_REDIS_STOP_SCRIPT, DEFAULT_REDIS_STOP_SCRIPT);
	}

	public int getMbufSize() {
		return configSource.get(CONFIG_DYNO_MBUF_SIZE, 16384);
	}

	public int getAllocatedMessages() {
		return configSource.get(CONFIG_DYNO_MAX_ALLOC_MSGS, 200000);
	}

	public boolean isVpc() {
		return configSource.get(CONFIG_VPC, false);
	}

	// Backup & Restore Implementations

	@Override
	public String getPersistenceLocation() {
		final String DEFAULT_REDIS_PERSISTENCE_DIR = "/mnt/data/nfredis";
		return configSource.get(CONFIG_REDIS_PERSISTENCE_DIR, DEFAULT_REDIS_PERSISTENCE_DIR);
	}

	@Override
	public String getBucketName() {
		return configSource.get(CONFIG_BUCKET_NAME, DEFAULT_BUCKET_NAME);
	}

	@Override
	public String getBackupLocation() {
		return configSource.get(CONFIG_S3_BASE_DIR, DEFAULT_BUCKET_FOLDER);
	}

	@Override
	public boolean isBackupEnabled() {
		return configSource.get(CONFIG_BACKUP_ENABLED, DEFAULT_BACKUP_ENABLED);
	}

	@Override
	public boolean isRestoreEnabled() {
		return configSource.get(CONFIG_RESTORE_ENABLED, DEFAULT_RESTORE_ENABLED);
	}

	@Override
	public String getBackupSchedule() {
		if (CONFIG_BACKUP_SCHEDULE != null &&
				!"day".equals(CONFIG_BACKUP_SCHEDULE) &&
				!"week".equals(CONFIG_BACKUP_SCHEDULE)) {

			logger.error("The persistence schedule FP is wrong: day or week");
			logger.error("Defaulting to day");
			return configSource.get("day", DEFAULT_BACKUP_SCHEDULE);
		}
		return configSource.get(CONFIG_BACKUP_SCHEDULE, DEFAULT_BACKUP_SCHEDULE);
	}

	@Override
	public int getBackupHour() {
		return configSource.get(CONFIG_BACKUP_HOUR, DEFAULT_BACKUP_HOUR);
	}

	@Override
	public String getRestoreDate() {
		return configSource.get(CONFIG_RESTORE_TIME, DEFAULT_RESTORE_TIME);
	}

	@Override
	public boolean isPersistenceEnabled() {
		final boolean DEFAULT_REDIS_PERSISTENCE_ENABLED = false;
		return configSource.get(CONFIG_REDIS_PERSISTENCE_ENABLED, DEFAULT_REDIS_PERSISTENCE_ENABLED);
	}

	@Override
	public boolean isAof() {
		final String DEFAULT_REDIS_PERSISTENCE_TYPE = "aof";
		if (configSource.get(CONFIG_REDIS_PERSISTENCE_TYPE, DEFAULT_REDIS_PERSISTENCE_TYPE).equals("rdb")) {
			return false;
		} else if (configSource.get(CONFIG_REDIS_PERSISTENCE_TYPE, DEFAULT_REDIS_PERSISTENCE_TYPE).equals("aof")) {
			return true;
		} else {
			logger.error("The persistence type FP is wrong: aof or rdb");
			logger.error("Defaulting to rdb");
			return false;
		}
	}

	// VPC
	public String getVpcId() {
		return NETWORK_VPC;
	}

	@Override
	public String getClassicAWSRoleAssumptionArn() {
		return configSource.get(CONFIG_EC2_ROLE_ASSUMPTION_ARN);
	}

	@Override
	public String getVpcAWSRoleAssumptionArn() {
		return configSource.get(CONFIG_VPC_ROLE_ASSUMPTION_ARN);
	}

	@Override
	public boolean isDualAccount() {
		return configSource.get(CONFIG_DUAL_ACCOUNT, DEFAULT_DUAL_ACCOUNT);
	}

	// Cassandra configuration for token management
	@Override
	public String getCassandraKeyspaceName() {
		return configSource.get(CONFIG_CASSANDRA_KEYSPACE_NAME, DEFAULT_CASSANDRA_KEYSPACE_NAME);
	}

	@Override
	public int getCassandraThriftPortForAstyanax() {
		return configSource.get(CONFIG_CASSANDRA_THRIFT_PORT, DEFAULT_CASSANDRA_THRIFT_PORT);
	}

	@Override
	public String getCommaSeparatedCassandraHostNames() {
		return configSource.get(CONFIG_COMMA_SEPARATED_CASSANDRA_HOSTNAMES,
				DEFAULT_COMMA_SEPARATED_CASSANDRA_HOSTNAMES);
	}

	@Override
	public boolean isEurekaHostSupplierEnabled() {
		return configSource
				.get(CONFIG_IS_EUREKA_HOST_SUPPLIER_ENABLED, DEFAULT_IS_EUREKA_HOST_SUPPLIER_ENABLED);
	}

	// Redis
	// =====

	/**
	 * {@inheritDoc}
	 * @return {@inheritDoc}
	 */
	@Override
	public String getRedisConf() {
		final String DEFAULT_REDIS_CONF = "/apps/nfredis/conf/redis.conf";
		return configSource.get(CONFIG_REDIS_CONF, DEFAULT_REDIS_CONF);
	}

	/**
	 * {@inheritDoc}
	 * @return {@inheritDoc}
	 */
	@Override
	public String getRedisAofFilename() {
		final String DEFAULT_REDIS_AOF_FILENAME = "appendonly.aof";
		return configSource.get(CONFIG_REDIS_AOF_FILENAME, DEFAULT_REDIS_AOF_FILENAME);
	}

	/**
	 * {@inheritDoc}
	 * @return {@inheritDoc}
	 */
	@Override
	public String getRedisRdbFilename() {
		final String DEFAULT_REDIS_RDB_FILENAME = "nfredis.rdb";
		return configSource.get(CONFIG_REDIS_RDB_FILENAME, DEFAULT_REDIS_RDB_FILENAME);
	}

}
