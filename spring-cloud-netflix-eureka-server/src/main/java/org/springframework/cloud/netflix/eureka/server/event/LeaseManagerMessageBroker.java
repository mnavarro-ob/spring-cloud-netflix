/*
 * Copyright 2013-2015 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.cloud.netflix.eureka.server.event;

import java.util.List;

import javax.annotation.Nullable;

import lombok.extern.apachecommons.CommonsLog;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.netflix.eureka.server.advice.LeaseManagerLite;
import org.springframework.context.ApplicationContext;

import com.google.common.base.Optional;
import com.google.common.base.Predicate;
import com.netflix.appinfo.InstanceInfo;
import com.netflix.discovery.shared.Application;
import com.netflix.eureka.PeerAwareInstanceRegistry;
import com.netflix.eureka.lease.Lease;

import static com.google.common.collect.Iterables.tryFind;

/**
 * @author Spencer Gibb
 */
@CommonsLog
public class LeaseManagerMessageBroker implements LeaseManagerLite<InstanceInfo> {

	@Autowired
	private ApplicationContext ctxt;

	@Override
	public void register(InstanceInfo info, boolean isReplication) {
		register(info, Lease.DEFAULT_DURATION_IN_SECS, isReplication);
	}

	@Override
	public void register(InstanceInfo info, int leaseDuration, boolean isReplication) {
		log.debug("register " + info.getAppName() + ", vip " + info.getVIPAddress()
				+ ", leaseDuration " + leaseDuration + ", isReplication " + isReplication);
		// TODO: what to publish from info (whole object?)
		this.ctxt.publishEvent(new EurekaInstanceRegisteredEvent(this, info,
				leaseDuration, isReplication));
	}

	@Override
	public boolean cancel(String appName, String serverId, boolean isReplication) {
		log.debug("cancel " + appName + " serverId " + serverId + ", isReplication {}"
				+ isReplication);
		this.ctxt.publishEvent(new EurekaInstanceCanceledEvent(this, appName, serverId,
				isReplication));
		return false;
	}

	@Override
	public boolean renew(final String appName, final String serverId,
			boolean isReplication) {
		log.debug("renew " + appName + " serverId " + serverId + ", isReplication {}"
				+ isReplication);
		List<Application> applications = PeerAwareInstanceRegistry.getInstance()
				.getSortedApplications();
		Optional<Application> app = tryFind(applications, new Predicate<Application>() {
			@Override
			public boolean apply(@Nullable Application input) {
				return input.getName().equals(appName);
			}
		});
		if (app.isPresent()) {
			Optional<InstanceInfo> info = tryFind(app.get().getInstances(),
					new Predicate<InstanceInfo>() {
						@Override
						public boolean apply(@Nullable InstanceInfo input) {
							return input.getHostName().equals(serverId);
						}
					});
			this.ctxt.publishEvent(new EurekaInstanceRenewedEvent(this, appName,
					serverId, info.orNull(), isReplication));
		}
		return false;
	}

	@Override
	public void evict() {
	}

}
