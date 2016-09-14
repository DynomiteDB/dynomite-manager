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
package com.netflix.dynomitemanager.sidecore.storage;

import static com.netflix.dynomitemanager.defaultimpl.DynomitemanagerConfiguration.REDIS;
import static com.netflix.dynomitemanager.defaultimpl.DynomitemanagerConfiguration.MEMCACHED;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.netflix.dynomitemanager.sidecore.IConfiguration;

@Singleton
public class StorageProxyProvider {

	@Inject private IConfiguration config;
	private IStorageProxy storageProxy;

	public IStorageProxy getStorageProxy() {
		if (config.getDataStoreType() == MEMCACHED) {
			if (storageProxy == null) {
				storageProxy = new MemcachedStorageProxy();
			}
		} else if (config.getDataStoreType() == REDIS) {
			if (storageProxy == null) {
				storageProxy = new RedisStorageProxy();
			}
		}

		return storageProxy;
	}

}
