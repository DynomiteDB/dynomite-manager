/**
 * Copyright 2016 DynomiteDB
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.netflix.dynomitemanager.identity;

import com.amazonaws.auth.profile.ProfileCredentialsProvider;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClient;
import com.amazonaws.services.dynamodbv2.document.*;
import com.amazonaws.services.dynamodbv2.document.spec.ScanSpec;
import com.amazonaws.services.dynamodbv2.document.utils.NameMap;
import com.amazonaws.services.dynamodbv2.document.utils.ValueMap;
import com.google.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

//import com.netflix.astyanax.AstyanaxContext;
//import com.netflix.astyanax.ColumnListMutation;
//import com.netflix.astyanax.Keyspace;
//import com.netflix.astyanax.MutationBatch;
//import com.netflix.astyanax.connectionpool.Host;
//import com.netflix.astyanax.connectionpool.NodeDiscoveryType;
//import com.netflix.astyanax.connectionpool.OperationResult;
//import com.netflix.astyanax.connectionpool.exceptions.ConnectionException;
//import com.netflix.astyanax.connectionpool.impl.ConnectionPoolConfigurationImpl;
//import com.netflix.astyanax.connectionpool.impl.ConnectionPoolType;
//import com.netflix.astyanax.connectionpool.impl.CountingConnectionPoolMonitor;
//import com.netflix.astyanax.impl.AstyanaxConfigurationImpl;
//import com.netflix.astyanax.model.ColumnFamily;
///import com.netflix.astyanax.model.ColumnList;
//import com.netflix.astyanax.serializers.StringSerializer;
//import com.netflix.astyanax.thrift.ThriftFamilyFactory;
///import com.netflix.astyanax.util.TimeUUIDUtils;
// BALIG: Required imports for Amazon Dynamo

@Singleton
public class InstanceDataDAODynamo {
	private static final Logger logger = LoggerFactory.getLogger(InstanceDataDAODynamo.class);

	// BALIG: Dynamo Config Variables
	static DynamoDB dynamoDB = new DynamoDB(new AmazonDynamoDBClient(new ProfileCredentialsProvider()));

	//private final Keyspace bootKeyspace;
	//private final IConfiguration config;
	//private final HostSupplier hostSupplier;
	//private final String BOOT_CLUSTER;
	//private final String KS_NAME;
	//private final int thriftPortForAstyanax;
	//private final AstyanaxContext<Keyspace> ctx;
	private String CN_ID = "Id";
	private String CN_APPID = "appId";
	private String CN_AZ = "availabilityZone";
	private String CN_DC = "datacenter";
	private String CN_INSTANCEID = "instanceId";
	private String CN_HOSTNAME = "hostname";
	private String CN_EIP = "elasticIP";
	private String CN_TOKEN = "token";
	private String CN_LOCATION = "location";
	private String CN_VOLUME_PREFIX = "ssVolumes";
	private String CN_UPDATETIME = "updatetime";
	private String CF_NAME_TOKENS = "tokens";
	/*
	 * Schema: create column family tokens with comparator=UTF8Type and
	 * column_metadata=[ {column_name: appId, validation_class:
	 * UTF8Type,index_type: KEYS}, {column_name: instanceId, validation_class:
	 * UTF8Type}, {column_name: token, validation_class: UTF8Type},
	 * {column_name: availabilityZone, validation_class: UTF8Type},
	 * {column_name: hostname, validation_class: UTF8Type},{column_name: Id,
	 * validation_class: UTF8Type}, {column_name: elasticIP, validation_class:
	 * UTF8Type}, {column_name: updatetime, validation_class: TimeUUIDType},
	 * {column_name: location, validation_class: UTF8Type}];
	 */
	//public ColumnFamily<String, String> CF_TOKENS = new ColumnFamily<String, String>(CF_NAME_TOKENS,
	//	StringSerializer.get(), StringSerializer.get());
	//private String CF_NAME_LOCKS = "locks";
	// Schema: create column family locks with comparator=UTF8Type;
	//public ColumnFamily<String, String> CF_LOCKS = new ColumnFamily<String, String>(CF_NAME_LOCKS,
	//		StringSerializer.get(), StringSerializer.get());

	/*@Inject public InstanceDataDAODynamo(IConfiguration config, HostSupplier hostSupplier)
			throws ConnectionException {
		this.config = config;

		BOOT_CLUSTER = config.getBootClusterName();

		if (BOOT_CLUSTER == null || BOOT_CLUSTER.isEmpty())
			throw new RuntimeException(
					"BootCluster can not be blank. Please use getBootClusterName() property.");

		KS_NAME = config.getDynamoKeyspaceName();

		if (KS_NAME == null || KS_NAME.isEmpty())
			throw new RuntimeException(
					"Dynamo Keyspace can not be blank. Please use getDynamoKeyspaceName() property.");

		thriftPortForAstyanax = config.getDynamoThriftPortForAstyanax();
		if (thriftPortForAstyanax <= 0)
			throw new RuntimeException(
					"Thrift Port for Astyanax can not be blank. Please use getDynamoThriftPortForAstyanax() property.");

		this.hostSupplier = hostSupplier;

		if (config.isEurekaHostSupplierEnabled())
			ctx = initWithThriftDriverWithEurekaHostsSupplier();
		else
			ctx = initWithThriftDriverWithExternalHostsSupplier();

		ctx.start();
		bootKeyspace = ctx.getClient();
	}*/

	public void createInstanceEntry(AppsInstance instance) throws Exception {
		logger.info("*** Creating New Instance Entry ***");
		String key = getRowKey(instance);
		// If the key exists throw exception
		if (getInstance(instance.getApp(), instance.getRack(), instance.getId()) != null) {
			logger.info(String.format("Key already exists: %s", key));
			return;
		}

		getLock(instance);

		try {
			/*MutationBatch m = bootKeyspace.prepareMutationBatch();
			ColumnListMutation<String> clm = m.withRow(CF_TOKENS, key);
			clm.putColumn(CN_ID, Integer.toString(instance.getId()), null);
			clm.putColumn(CN_APPID, instance.getApp(), null);
			clm.putColumn(CN_AZ, instance.getZone(), null);
			clm.putColumn(CN_DC, config.getRack(), null);
			clm.putColumn(CN_INSTANCEID, instance.getInstanceId(), null);
			clm.putColumn(CN_HOSTNAME, instance.getHostName(), null);
			clm.putColumn(CN_EIP, instance.getHostIP(), null);
			clm.putColumn(CN_TOKEN, instance.getToken(), null);
			clm.putColumn(CN_LOCATION, instance.getDatacenter(), null);
			clm.putColumn(CN_UPDATETIME, TimeUUIDUtils.getUniqueTimeUUIDinMicros(), null);
			Map<String, Object> volumes = instance.getVolumes();
			if (volumes != null) {
				for (String path : volumes.keySet()) {
					clm.putColumn(CN_VOLUME_PREFIX + "_" + path, volumes.get(path).toString(),
							null);
				}
			}
			m.execute();*/
		} catch (Exception e) {
			logger.info(e.getMessage());
		} finally {
			releaseLock(instance);
		}
	}

	/*
	 * To get a lock on the row - Create a choosing row and make sure there are
	 * no contenders. If there are bail out. Also delete the column when bailing
	 * out. - Once there are no contenders, grab the lock if it is not already
	 * taken.
	 */
	private void getLock(AppsInstance instance) throws Exception {

		String choosingkey = getChoosingKey(instance);
		/*MutationBatch m = bootKeyspace.prepareMutationBatch();
		ColumnListMutation<String> clm = m.withRow(CF_LOCKS, choosingkey);

		// Expire in 6 sec
		clm.putColumn(instance.getInstanceId(), instance.getInstanceId(), new Integer(6));
		m.execute();
		int count = bootKeyspace.prepareQuery(CF_LOCKS).getKey(choosingkey).getCount().execute().getResult();
		if (count > 1) {
			// Need to delete my entry
			m.withRow(CF_LOCKS, choosingkey).deleteColumn(instance.getInstanceId());
			m.execute();
			throw new Exception(String.format("More than 1 contender for lock %s %d", choosingkey, count));
		}

		String lockKey = getLockingKey(instance);
		OperationResult<ColumnList<String>> result = bootKeyspace.prepareQuery(CF_LOCKS).getKey(lockKey)
				.execute();
		if (result.getResult().size() > 0 && !result.getResult().getColumnByIndex(0).getName()
				.equals(instance.getInstanceId()))
			throw new Exception(String.format("Lock already taken %s", lockKey));

		clm = m.withRow(CF_LOCKS, lockKey);
		clm.putColumn(instance.getInstanceId(), instance.getInstanceId(), new Integer(600));
		m.execute();
		Thread.sleep(100);
		result = bootKeyspace.prepareQuery(CF_LOCKS).getKey(lockKey).execute();
		if (result.getResult().size() == 1 && result.getResult().getColumnByIndex(0).getName()
				.equals(instance.getInstanceId())) {
			logger.info("Got lock " + lockKey);
			return;
		} else
			throw new Exception(String.format("Cannot insert lock %s", lockKey));*/

	}

	private void releaseLock(AppsInstance instance) throws Exception {
		String choosingkey = getChoosingKey(instance);
		/*MutationBatch m = bootKeyspace.prepareMutationBatch();
		ColumnListMutation<String> clm = m.withRow(CF_LOCKS, choosingkey);

		m.withRow(CF_LOCKS, choosingkey).deleteColumn(instance.getInstanceId());
		m.execute();*/
	}

	public void deleteInstanceEntry(AppsInstance instance) throws Exception {
		// Acquire the lock first
		getLock(instance);

		// Delete the row
		String key = findKey(instance.getApp(), String.valueOf(instance.getId()), instance.getDatacenter(),
				instance.getRack());
		if (key == null)
			return;  //don't fail it

		/*MutationBatch m = bootKeyspace.prepareMutationBatch();
		m.withRow(CF_TOKENS, key).delete();
		m.execute();

		key = getLockingKey(instance);
		// Delete key
		m = bootKeyspace.prepareMutationBatch();
		m.withRow(CF_LOCKS, key).delete();
		m.execute();

		// Have to delete choosing key as well to avoid issues with delete
		// followed by immediate writes
		key = getChoosingKey(instance);
		m = bootKeyspace.prepareMutationBatch();
		m.withRow(CF_LOCKS, key).delete();
		m.execute();*/

	}

	// BALIG: The below function will remain intact
	public AppsInstance getInstance(String app, String rack, int id) {
		Set<AppsInstance> set = getAllInstances(app);
		for (AppsInstance ins : set) {
			if (ins.getId() == id && ins.getRack().equals(rack))
				return ins;
		}
		return null;
	}

	// BALIG: The below function will remain intact
	public Set<AppsInstance> getLocalDCInstances(String app, String region) {
		Set<AppsInstance> set = getAllInstances(app);
		Set<AppsInstance> returnSet = new HashSet<AppsInstance>();

		for (AppsInstance ins : set) {
			if (ins.getDatacenter().equals(region))
				returnSet.add(ins);
		}
		return returnSet;
	}

	// BALIG: Rewritten the code for getAllInstances to get items from dynamo
	public Set<AppsInstance> getAllInstances(String app) {
		Set<AppsInstance> set = new HashSet<AppsInstance>();
		try {

			// Getting the table from which we want to get the items
			Table table = dynamoDB.getTable(CF_NAME_TOKENS);
			ScanSpec scanSpec = new ScanSpec().withFilterExpression(CN_APPID + " = :" + CN_APPID)
					.withValueMap(new ValueMap().withString(":" + CN_APPID, app));

			ItemCollection<ScanOutcome> items = table.scan(scanSpec);

			Iterator<Item> iter = items.iterator();
			while (iter.hasNext()) {
				Item item = iter.next();
				set.add(transform(item));

				// TODO: DELETE THE BELOW DEBUG STATEMENT
				System.out.println(item.toString());
			}
		} catch (Exception e) {
			logger.warn("Caught an Unknown Exception during reading msgs ... -> " + e.getMessage());
			throw new RuntimeException(e);
		}
		return set;
	}

	// BALIG: Rewritten the code for findKey to get a key from dynamo
	public String findKey(String app, String id, String location, String datacenter) {
		try {
			// Getting the table from which we want to get the items
			Table table = dynamoDB.getTable(CF_NAME_TOKENS);

			ScanSpec scanSpec = new ScanSpec().withFilterExpression(
					CN_APPID + " = :" + CN_APPID + " And " + CN_ID + " = :" + CN_ID + " And #"
							+ CN_LOCATION + " = :" + CN_LOCATION + " And " + CN_DC + " = :"
							+ CN_DC)
					.withNameMap(new NameMap().with("#" + CN_LOCATION, CN_LOCATION))
					.withValueMap(new ValueMap().withString(":" + CN_APPID, app)
							.withString(":" + CN_ID, id)
							.withString(":" + CN_LOCATION, location)
							.withString(":" + CN_DC, datacenter));

			ItemCollection<ScanOutcome> items = table.scan(scanSpec);

			Iterator<Item> iter = items.iterator();
			while (iter.hasNext()) {
				Item item = iter.next();

				// TODO: DELETE ALL THE BELOW DEBUG STATEMENTS
				// Print the different attributes of the item fetched
				System.out.println("Key: " + item.get("key"));
				System.out.println("Id: " + item.get("Id"));
				System.out.println("AppId: " + item.get("appId"));
				System.out.println("Availability Zone: " + item.get("availabilityZone"));
				System.out.println("Data center: " + item.get("datacenter"));
				System.out.println("Elastic IP: " + item.get("elasticIP"));
				System.out.println("Hostname: " + item.get("hostname"));
				System.out.println("Instance Id: " + item.get("instanceId"));
				System.out.println("Location: " + item.get("location"));
				System.out.println("Token: " + item.get("token"));
				System.out.println("Update Time: " + item.get("updatetime"));

				// returning the key found
				return item.get("key").toString();
			}
			// return null if no key is found
			return null;

		} catch (Exception e) {
			logger.warn("Caught an Unknown Exception during find a row matching cluster[" + app + "], id["
					+ id + "], and region[" + datacenter + "]  ... -> " + e.getMessage());
			throw new RuntimeException(e);
		}

	}

	// BALIG: Rewritten the code for transform to get items from dynamo
	private AppsInstance transform(Item item) {
		AppsInstance ins = new AppsInstance();

		ins.setApp(item.get(CN_APPID).toString());
		ins.setZone(item.get(CN_AZ).toString());
		ins.setHost(item.get(CN_HOSTNAME).toString());
		ins.setHostIP(item.get(CN_EIP).toString());
		ins.setId(Integer.parseInt(item.get(CN_ID).toString()));
		ins.setInstanceId(item.get(CN_INSTANCEID).toString());
		ins.setDatacenter(item.get(CN_LOCATION).toString());
		ins.setRack(item.get(CN_DC).toString());
		ins.setToken(item.get(CN_TOKEN).toString());

		// TODO: We need to come up with the code to set the update time as below
		//ins.setUpdatetime(item.getTimestamp());
		return ins;
	}

	private String getChoosingKey(AppsInstance instance) {
		return instance.getApp() + "_" + instance.getRack() + "_" + instance.getId() + "-choosing";
	}

	private String getLockingKey(AppsInstance instance) {
		return instance.getApp() + "_" + instance.getRack() + "_" + instance.getId() + "-lock";
	}

	private String getRowKey(AppsInstance instance) {
		return instance.getApp() + "_" + instance.getRack() + "_" + instance.getId();
	}

	/*private AstyanaxContext<Keyspace> initWithThriftDriverWithEurekaHostsSupplier() {

		logger.info("BOOT_CLUSTER = {}, KS_NAME = {}", BOOT_CLUSTER, KS_NAME);
		return new AstyanaxContext.Builder().forCluster(BOOT_CLUSTER).forKeyspace(KS_NAME)
				.withAstyanaxConfiguration(new AstyanaxConfigurationImpl()
						.setDiscoveryType(NodeDiscoveryType.DISCOVERY_SERVICE))
				.withConnectionPoolConfiguration(new ConnectionPoolConfigurationImpl("MyConnectionPool")
						.setMaxConnsPerHost(3).setPort(thriftPortForAstyanax))
				.withHostSupplier(hostSupplier.getSupplier(BOOT_CLUSTER))
				.withConnectionPoolMonitor(new CountingConnectionPoolMonitor())
				.buildKeyspace(ThriftFamilyFactory.getInstance());

	}

	private AstyanaxContext<Keyspace> initWithThriftDriverWithExternalHostsSupplier() {

		logger.info("BOOT_CLUSTER = {}, KS_NAME = {}", BOOT_CLUSTER, KS_NAME);
		return new AstyanaxContext.Builder().forCluster(BOOT_CLUSTER).forKeyspace(KS_NAME)
				.withAstyanaxConfiguration(new AstyanaxConfigurationImpl()
						.setDiscoveryType(NodeDiscoveryType.DISCOVERY_SERVICE)
						.setConnectionPoolType(ConnectionPoolType.ROUND_ROBIN))
				.withConnectionPoolConfiguration(new ConnectionPoolConfigurationImpl("MyConnectionPool")
						.setMaxConnsPerHost(3).setPort(thriftPortForAstyanax))
				.withHostSupplier(getSupplier())
				.withConnectionPoolMonitor(new CountingConnectionPoolMonitor())
				.buildKeyspace(ThriftFamilyFactory.getInstance());

	}

	private Supplier<List<Host>> getSupplier() {

		return new Supplier<List<Host>>() {

			@Override public List<Host> get() {

				List<Host> hosts = new ArrayList<Host>();

				List<String> dynHostnames = new ArrayList<String>(Arrays.asList(StringUtils
						.split(config.getCommaSeparatedDynamoHostNames(), ",")));

				if (dynHostnames.size() == 0)
					throw new RuntimeException(
							"Dynamo Host Names can not be blank. At least one host is needed. Please use getCommaSeparatedDynamoHostNames() property.");

				for (String dynHost : dynHostnames) {
					logger.info("Adding Dynamo Host = {}", dynHost);
					hosts.add(new Host(dynHost, thriftPortForAstyanax));
				}

				return hosts;
			}
		};
	}*/
}
