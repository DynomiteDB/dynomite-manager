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
import com.amazonaws.services.dynamodbv2.document.spec.DeleteItemSpec;
import com.amazonaws.services.dynamodbv2.document.spec.ScanSpec;
import com.amazonaws.services.dynamodbv2.document.spec.UpdateItemSpec;
import com.amazonaws.services.dynamodbv2.document.utils.NameMap;
import com.amazonaws.services.dynamodbv2.document.utils.ValueMap;
import com.amazonaws.services.dynamodbv2.model.*;
import com.google.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

@Singleton public class InstanceDataDAODynamo {
	private static final Logger logger = LoggerFactory.getLogger(InstanceDataDAODynamo.class);

	// BALIG: Dynamo Config Variables will appear below.
	// TODO: We still need to find a way on how to connect to the dynamodb online
	// Below mentioned is just a sample code
	static DynamoDB dynamoDB = new DynamoDB(new AmazonDynamoDBClient(new ProfileCredentialsProvider()));

	private String CN_KEY = "key";
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
	private String CF_NAME_LOCKS = "locks";

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

	// Create tokens table
	private void createTableTokens(long readCapacityUnits, long writeCapacityUnits) {

		try {

			// Define all the attributes of the table
			ArrayList<AttributeDefinition> attributeDefinitions = new ArrayList<AttributeDefinition>();
			attributeDefinitions.add(new AttributeDefinition().withAttributeName(CN_KEY)
					.withAttributeType("S"));

			// Define the key schema for the table
			ArrayList<KeySchemaElement> keySchema = new ArrayList<KeySchemaElement>();
			keySchema.add(new KeySchemaElement().withAttributeName(CN_KEY) // Partition key
					.withKeyType(KeyType.HASH));

			// Define a request to create a table
			CreateTableRequest request = new CreateTableRequest().withTableName(CF_NAME_TOKENS)
					.withKeySchema(keySchema).withAttributeDefinitions(attributeDefinitions)
					.withProvisionedThroughput(new ProvisionedThroughput()
							.withReadCapacityUnits(readCapacityUnits)
							.withWriteCapacityUnits(writeCapacityUnits));

			// Debugging statement
			System.out.println("Issuing CreateTable request for " + CF_NAME_TOKENS);

			// Create the table
			Table table = dynamoDB.createTable(request);

			// Debugging statement
			System.out.println(
					"Waiting for " + CF_NAME_TOKENS + " to be created...this may take a while...");

			// The pointer will return only if the table is created successfully and ready for CRUD operations
			// i.e. active
			table.waitForActive();
		} catch (Exception e) {
			System.err.println("CreateTable request failed for " + CF_NAME_TOKENS);
			System.err.println(e.getMessage());
		}
	}

	// Function to insert an item in tokens table
	private void createTokensItems(AppsInstance instance, String key) {
		try {
			// Getting the table in which we want to insert items
			Table table = dynamoDB.getTable(CF_NAME_TOKENS);
			Map<String, Object> volumes = instance.getVolumes();

			// Create a new item which we want to put into the table
			Item item = new Item().withPrimaryKey(CN_KEY, key)
					.withString(CN_ID, Integer.toString(instance.getId()))
					.withString(CN_APPID, instance.getApp()).withString(CN_AZ, instance.getZone())

					// BALIG:
					// TODO: How to get the rack below?
					//.withString(CN_DC, config.getRack())
					.withString(CN_INSTANCEID, instance.getInstanceId())
					.withString(CN_HOSTNAME, instance.getHostName())
					.withString(CN_EIP, instance.getHostIP())
					.withString(CN_TOKEN, instance.getToken())
					.withString(CN_LOCATION, instance.getDatacenter());

			// BALIG:
			// TODO: How to get the update time below?
			//.withString(CN_UPDATETIME, TimeUUIDUtils.getUniqueTimeUUIDinMicros());

			if (volumes != null) {
				for (String path : volumes.keySet()) {
					item.withString(CN_VOLUME_PREFIX + "_" + path, volumes.get(path).toString());
				}
			}

			// Put the item into the tokens table
			table.putItem(item);
		} catch (Exception e) {
			System.err.println("Create items failed.");
			System.err.println(e.getMessage());
		}
	}

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
			//  Call the function to create the tokens table
			createTableTokens(10L, 5L);
			// Call a function to insert items in tokens table
			createTokensItems(instance, key);
		} catch (Exception e) {
			logger.info(e.getMessage());
		} finally {
			releaseLock(instance);
		}
	}

	// Function to delete an item from a table based on primary key
	private void deleteItem(String tableName, String choosingKey) {

		Table table = dynamoDB.getTable(tableName);

		try {

			DeleteItemSpec deleteItemSpec = new DeleteItemSpec().withPrimaryKey("key", choosingKey)
					.withReturnValues(ReturnValue.ALL_OLD);

			DeleteItemOutcome outcome = table.deleteItem(deleteItemSpec);

			// Check the response.
			// BALIG:
			// TODO: Remove the below debugging statements
			System.out.println("Printing item that was deleted...");
			System.out.println(outcome.getItem().toJSONPretty());

		} catch (Exception e) {
			System.err.println("Error deleting item in " + tableName);
			System.err.println(e.getMessage());
		}
	}

	/*
	 * To get a lock on the row - Create a choosing row and make sure there are
	 * no contenders. If there are bail out. Also delete the column when bailing
	 * out. - Once there are no contenders, grab the lock if it is not already
	 * taken.
	 */
	private void getLock(AppsInstance instance) throws Exception {
		String choosingKey = getChoosingKey(instance);
		Table table = dynamoDB.getTable(CF_NAME_LOCKS);

		// BALIG: I have tried to insert the column in dynamodb as per
		//	  below code but I am not sure if we have the functionality
		//	  in dynamoDb where a column expires after mentioned duration
		//	  automatically as happening in the below Netflix code.
		//MutationBatch m = bootKeyspace.prepareMutationBatch();
		//ColumnListMutation<String> clm = m.withRow(CF_LOCKS, choosingkey);

		// Expire in 6 sec
		//clm.putColumn(instance.getInstanceId(), instance.getInstanceId(), new Integer(6));
		//m.execute();

		try {
			UpdateItemSpec updateItemSpec = new UpdateItemSpec().withPrimaryKey(CN_KEY, choosingKey)
					.withUpdateExpression("set #iid = :val")
					.withNameMap(new NameMap().with("#iid", instance.getInstanceId()))
					.withValueMap(new ValueMap().withString(":val", instance.getInstanceId()))
					.withReturnValues(ReturnValue.ALL_NEW);

			UpdateItemOutcome outcome = table.updateItem(updateItemSpec);

			// BALIG:
			// TODO: Remove the below debug statements after testing
			// Check the response.
			System.out.println("Printing item after adding new attribute...");
			System.out.println(outcome.getItem().toJSONPretty());

			TableKeysAndAttributes locksTableKeysAndAttributes = new TableKeysAndAttributes(CF_NAME_LOCKS);
			// Add a partition key
			locksTableKeysAndAttributes.addHashOnlyPrimaryKeys(CN_KEY, choosingKey);

			BatchGetItemOutcome getItemsOutcome = dynamoDB.batchGetItem(locksTableKeysAndAttributes);

			int count = getItemsOutcome.getTableItems().size();
			if (count > 1) {
				// Need to delete my entry
				DeleteItemSpec deleteItemSpec = new DeleteItemSpec().withPrimaryKey(CN_KEY, choosingKey)
						.withConditionExpression("#iid = :val")
						.withNameMap(new NameMap().with("#iid", CN_INSTANCEID))
						.withValueMap(new ValueMap()
								.withString(":val", instance.getInstanceId()))
						.withReturnValues(ReturnValue.ALL_OLD);

				table.deleteItem(deleteItemSpec);
				throw new Exception(String.format("More than 1 contender for lock %s %d", choosingKey,
						count));
			}

			String lockKey = getLockingKey(instance);

			locksTableKeysAndAttributes = new TableKeysAndAttributes(CF_NAME_LOCKS);
			// Add a partition key
			locksTableKeysAndAttributes.addHashOnlyPrimaryKeys(CN_KEY, lockKey);
			getItemsOutcome = dynamoDB.batchGetItem(locksTableKeysAndAttributes);

			if (getItemsOutcome.getTableItems().size() > 0 && !getItemsOutcome.getTableItems()
					.containsKey(instance.getInstanceId()))
				throw new Exception(String.format("Lock already taken %s", lockKey));

			updateItemSpec = new UpdateItemSpec().withPrimaryKey(CN_KEY, choosingKey)
					.withUpdateExpression("set #iid = :val")
					.withNameMap(new NameMap().with("#iid", instance.getInstanceId()))
					.withValueMap(new ValueMap().withString(":val", instance.getInstanceId()))
					.withReturnValues(ReturnValue.ALL_NEW);

			table.updateItem(updateItemSpec);
			Thread.sleep(100);

			locksTableKeysAndAttributes = new TableKeysAndAttributes(CF_NAME_LOCKS);
			// Add a partition key
			locksTableKeysAndAttributes.addHashOnlyPrimaryKeys(CN_KEY, lockKey);
			getItemsOutcome = dynamoDB.batchGetItem(locksTableKeysAndAttributes);

			if (getItemsOutcome.getTableItems().size() == 1 && !getItemsOutcome.getTableItems()
					.containsKey(instance.getInstanceId())) {
				logger.info("Got lock " + lockKey);
				return;
			} else
				throw new Exception(String.format("Cannot insert lock %s", lockKey));

		} catch (Exception e) {
			System.err.println("Failed to get the lock in " + CF_NAME_LOCKS);
			System.err.println(e.getMessage());
		}
	}

	private void releaseLock(AppsInstance instance) throws Exception {
		Table table = dynamoDB.getTable(CF_NAME_LOCKS);
		String choosingKey = getChoosingKey(instance);
		try {
			DeleteItemSpec deleteItemSpec = new DeleteItemSpec().withPrimaryKey(CN_KEY, choosingKey)
					.withConditionExpression("#iid = :val")
					.withNameMap(new NameMap().with("#iid", CN_INSTANCEID))
					.withValueMap(new ValueMap().withString(":val", instance.getInstanceId()))
					.withReturnValues(ReturnValue.ALL_OLD);

			DeleteItemOutcome outcome = table.deleteItem(deleteItemSpec);

			// BALIG:
			// TODO: We need to remove the below debug statements after testing
			// Check the response.
			System.out.println("Printing item that was deleted...");
			System.out.println(outcome.getItem().toJSONPretty());
		} catch (Exception e) {
			System.err.println("Error releasing lock in " + CF_NAME_LOCKS);
		}
	}

	public void deleteInstanceEntry(AppsInstance instance) throws Exception {
		// Acquire the lock first
		getLock(instance);

		// Delete the row
		String key = findKey(instance.getApp(), String.valueOf(instance.getId()), instance.getDatacenter(),
				instance.getRack());
		if (key == null)
			return;  //don't fail it

		deleteItem(CF_NAME_TOKENS, key);

		key = getLockingKey(instance);
		// Delete key
		deleteItem(CF_NAME_LOCKS, key);

		// Have to delete choosing key as well to avoid issues with delete
		// followed by immediate writes
		key = getChoosingKey(instance);
		deleteItem(CF_NAME_LOCKS, key);

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

				// BALIG:
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

				// BALIG:
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

		// BALIG:
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
}
