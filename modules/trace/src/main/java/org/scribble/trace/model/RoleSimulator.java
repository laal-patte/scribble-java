/*
 * Copyright 2009-14 www.scribble.org
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */
package org.scribble.trace.model;

import org.codehaus.jackson.annotate.JsonSubTypes;
import org.codehaus.jackson.annotate.JsonSubTypes.Type;
import org.codehaus.jackson.annotate.JsonTypeInfo;
import org.scribble.monitor.Message;
import org.scribble.trace.SimulatorContext;

// TODO: Need to change subtype mechanism to allow new role simulators to be dynamically
// used - registered using the RoleSimulatorManager.

/**
 * This abstract class represents a simulator associated with a
 * role in a message trace.
 *
 */
@JsonTypeInfo(use=JsonTypeInfo.Id.NAME, include=JsonTypeInfo.As.PROPERTY, property="type")
@JsonSubTypes({@Type(value=MonitorRoleSimulator.class) })
public abstract class RoleSimulator {

	/**
	 * This method initializes the role simulator.
	 * 
	 * @param context The context
	 */
	public abstract void init(SimulatorContext context);
	
	/**
	 * This method simulates the sending of a message.
	 * 
	 * @param context The context
	 * @param mesg The message
	 * @param toRole The target role
	 * @return Whether the send simulated as expected
	 */
	public abstract boolean send(SimulatorContext context, Message mesg, String toRole);
	
	/**
	 * This method simulates the receiving of a message.
	 * 
	 * @param context The context
	 * @param mesg The message
	 * @param fromRole The source role
	 * @return Whether the receive simulated as expected
	 */
	public abstract boolean receive(SimulatorContext context, Message mesg, String fromRole);
	
	/**
	 * This method closes the role simulator.
	 * 
	 * @param context The context
	 */
	public abstract void close(SimulatorContext context);
	
}
