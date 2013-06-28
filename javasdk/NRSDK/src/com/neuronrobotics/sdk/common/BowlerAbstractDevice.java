/*******************************************************************************
 * Copyright 2010 Neuron Robotics, LLC
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/
/**
 *
 * Copyright 2009 Neuron Robotics, LLC
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
package com.neuronrobotics.sdk.common;

import java.util.ArrayList;

import com.neuronrobotics.sdk.commands.bcs.core.NamespaceCommand;
import com.neuronrobotics.sdk.commands.bcs.core.PingCommand;
import com.neuronrobotics.sdk.commands.bcs.core.RpcArgumentsCommand;
import com.neuronrobotics.sdk.commands.bcs.core.RpcCommand;
import com.neuronrobotics.sdk.commands.neuronrobotics.dyio.InfoFirmwareRevisionCommand;
import com.neuronrobotics.sdk.util.ThreadUtil;

// TODO: Auto-generated Javadoc
/**
 * AbstractDevices are used to model devices that are connected to the Bowler network. AbstractDevice
 * implementations should encapsulate command generation and provide higher-level actions to users.  
 * 
 * @author rbreznak
 *
 */
public abstract class BowlerAbstractDevice implements IBowlerDatagramListener {
	
	private long heartBeatTime=1000;
	private long lastPacketTime=0;
	private HeartBeat beater;
	/** The connection. */
	private BowlerAbstractConnection connection=null;
	
	/** The address. */
	private MACAddress address = new MACAddress(MACAddress.BROADCAST);
	
	/**
	 * Determines if the device is available.
	 *
	 * @return true if the device is avaiable, false if it is not
	 * @throws InvalidConnectionException the invalid connection exception
	 */
	public boolean isAvailable() throws InvalidConnectionException{
		return getConnection().isConnected();
	}

	
	/**
	 * Set the connection to use when communicating commands with a device.
	 *
	 * @param connection the new connection
	 */
	private static ArrayList<IConnectionEventListener> disconnectListeners = new ArrayList<IConnectionEventListener> ();
	private ArrayList<NamespaceEncapsulation> namespaceList;
	
	public void addConnectionEventListener(IConnectionEventListener l ) {
		if(!disconnectListeners.contains(l)) {
			disconnectListeners.add(l);
		}
	}
	public void removeConnectionEventListener(IConnectionEventListener l ) {
		if(disconnectListeners.contains(l)) {
			disconnectListeners.remove(l);
		}
	}
	public void setConnection(BowlerAbstractConnection connection) {
		setThreadedUpstreamPackets(true);
		if(connection == null) {
			throw new NullPointerException("Can not use a NULL connection.");
		}
		for(IConnectionEventListener i:disconnectListeners) {
			connection.addConnectionEventListener(i);
		}
		this.connection = connection;
		connection.addDatagramListener(this);
	}
	
	/**
	 * This method tells the connection object to start and connects the up and down streams pipes. 
	 * Once this method is called and returns without exception, the device is ready to communicate with
	 *
	 * @return true, if successful
	 * @throws InvalidConnectionException the invalid connection exception
	 */
	public boolean connect() throws InvalidConnectionException {
		
		if (connection == null) {
			throw new InvalidConnectionException("Null Connection");
		}
		if(!connection.isConnected()) {
			if(!connection.connect()) {
				return false;
			}else {
				startHeartBeat();
			}
		}
		return this.isAvailable();
	}
	
	/**
	 * This method tells the connection object to disconnect its pipes and close out the connection. Once this is called, it is safe to remove your device.
	 */
	public void disconnect() {
		
		if (connection != null) {
			if(connection.isConnected()){
				Log.info("Disconnecting Bowler Device");
				connection.disconnect();
			}
		}
	}
	
	/**
	 * Get the connection that is used for communicating with the device.
	 *
	 * @return the device's connection
	 */
	public BowlerAbstractConnection getConnection() {
		return connection;
	}
	
	/**
	 * Set the MAC Address of the device. Every device should have a unique MAC address.
	 *
	 * @param address the new address
	 */
	public void setAddress(MACAddress address) {
		if(!address.isValid()) {
			throw new InvalidMACAddressException();
		}
		this.address = address;
	}
	
	/**
	 * Get the device's mac address.
	 *
	 * @return  the device's address
	 */
	public MACAddress getAddress() {
		//System.out.println();
		return address;
	}
	
	/**
	 * Send a sendable to the connection.
	 *
	 * @param sendable the sendable
	 * @return the syncronous response
	 */
	public BowlerDatagram send(ISendable sendable) {
		if((new BowlerDatagram(new ByteList(sendable.getBytes())).getRPC().toLowerCase().contains("_png"))){
			//Log.debug("sending ping");
		}else
			Log.debug("TX>>\n"+sendable.toString());
		
		BowlerDatagram b =connection.send(sendable);
		if(b != null) {
			if(b.getRPC().toLowerCase().contains("_png")){
				//Log.debug("ping ok!");
			}else
				Log.debug("RX<<\n"+
						(b.toString())
						);
		}else {
			//switch protocol version, try again
			Log.debug("RX<<: No response");
		}
		lastPacketTime = System.currentTimeMillis();
		return b;
	}
	

	/**
	 * Send a command to the connection.
	 *
	 * @param command the command
	 * @return the syncronous response
	 * @throws NoConnectionAvailableException the no connection available exception
	 * @throws InvalidResponseException the invalid response exception
	 */
	public BowlerDatagram send(BowlerAbstractCommand command) throws NoConnectionAvailableException, InvalidResponseException {	
		if(!connection.isConnected()) {
			throw new NoConnectionAvailableException();
		}
		
		return command.validate(send(BowlerDatagramFactory.build(getAddress(), command)));
	}
	/**
	 * THis is the scripting interface to Bowler devices. THis allows a user to describe a namespace, rpc, and array or 
	 * arguments to be paced into the packet based on the data types of the argument. The response in likewise unpacked 
	 * into an array of objects.
	 * @param namespace The string of the desired namespace
	 * @param rpcString The string of the desired RPC
	 * @param arguments An array of objects corresponding to the data to be stuffed into the packet.
	 * @return The return arguments parsed and packet into an array of arguments
	 * @throws DeviceConnectionException If the desired RPC's are not available then this will be thrown
	 */
	public Object [] send(String namespace,BowlerMethod method, String rpcString, Object[] arguments) throws DeviceConnectionException{
		for (NamespaceEncapsulation ns:namespaceList){
			if(ns.getNamespace().toLowerCase().contains(namespace.toLowerCase())){
				//found the namespace
				for(RpcEncapsulation rpc:ns.getRpcList()){
					if(		rpc.getRpc().toLowerCase().contains(rpcString.toLowerCase()) &&
							rpc.getDownstreamMethod() == method){
						//Found the command in the namespace
						BowlerDatagram dg =  send(rpc.getCommand(arguments));
						return rpc.parseResponse(dg);//parse and return
					}
				}
			}
		}
		System.err.println("No method found");
		for (NamespaceEncapsulation ns:namespaceList){
			System.err.println("Namespace "+ns);
		}
		throw new DeviceConnectionException("Device does not contain command '"+namespace+" "+method+" "+rpcString+"'");
	}
		
	/**
	 * Implementation of the Bowler ping ("_png") command
	 * Sends a ping to the device returns the device's MAC address.
	 *
	 * @return the device's address
	 */
	public BowlerDatagram ping() {
		try {
			BowlerDatagram bd = send(new PingCommand());
			//System.out.println("Ping success " + bd.getAddress());
			setAddress(bd.getAddress());
			return bd;
		} catch (InvalidResponseException e) {
			Log.error("Invalid response from Ping");
			return null;
		} catch (NoConnectionAvailableException e) {
			Log.error("No connection is available.");
			return null;
		}
	}
	
	/**
	 * Gets the revisions.
	 *
	 * @return the revisions
	 */
	public ArrayList<ByteList> getRevisions(){
		ArrayList<ByteList> list = new ArrayList<ByteList>();
		try {
			BowlerDatagram b = send(new InfoFirmwareRevisionCommand());
			Log.debug("FW info:\n"+b);
			for(int i=0;i<(b.getData().size()/3);i++){
				list.add(new ByteList(b.getData().getBytes((i*3),3)));
			}
		} catch (InvalidResponseException e) {
			Log.error("Invalid response from Firmware rev request");
			
		} catch (NoConnectionAvailableException e) {
			Log.error("No connection is available.");
		}
		return list;
	}
	
	/**
	 * Get all the namespaces.
	 *
	 * @return the namespaces
	 */
	public ArrayList<String>  getNamespaces(){
		try{
			//throw new RuntimeException("Called getNamespaces");
		}catch(Exception ex){
			ex.printStackTrace();
		}
		ArrayList<String> ret = new ArrayList<String>();
		if(namespaceList == null)
			namespaceList = new ArrayList<NamespaceEncapsulation>();
		synchronized (namespaceList){
			int numTry=0;
			boolean done=false;
			while(!done){
				numTry++;
				try {
					BowlerDatagram b = send(new NamespaceCommand(0));
					int num;
					String tmpNs =b.getData().asString();
					if(tmpNs.length() ==  b.getData().size()){
						//System.out.println("Ns = "+tmpNs+" len = "+tmpNs.length()+" data = "+b.getData().size());
						b = send(new NamespaceCommand());
						num= b.getData().getByte(0);		
						Log.warning("This is an older implementation of core, depricated");
					}else{
						num= b.getData().getByte(b.getData().size()-1);
						Log.info("This is the new core");
					}
					
					if(num<1){
						Log.error("Namespace request failed:\n"+b);
					}else{
						Log.info("Number of Namespaces="+num);
					}
					Log.debug("There are "+num+" namespaces on this device");
					for (int i=0;i<num;i++){
						b = send(new NamespaceCommand(i));
						String space = b.getData().asString();
						Log.debug("Adding Namespace: "+space);
						
						namespaceList.add(new NamespaceEncapsulation(space));
					}
					Log.debug("Attempting to populate RPC lists for all "+namespaceList.size());
					for(NamespaceEncapsulation ns:namespaceList){
						getRpcList(ns.getNamespace());
					}
					done = true;
				} catch (InvalidResponseException e) {
					Log.error("Invalid response from Namespace");
					if(numTry>3)
						throw e;
					
				} catch (NoConnectionAvailableException e) {
					Log.error("No connection is available.");
					if(numTry>3)
						throw e;
				}
				if(!done){
					//failed coms, reset list
					namespaceList = new ArrayList<NamespaceEncapsulation>();
				}
			}
		}
		
		for(NamespaceEncapsulation ns:namespaceList){
			ret.add(ns.getNamespace());
		}
		
		return ret;
		
	}
	/**
	 * Check the device to see if it has the requested namespace
	 * @param string
	 * @return
	 */
	public boolean hasNamespace(String string) {
		if(namespaceList == null)
			getNamespaces();
		for(NamespaceEncapsulation ns:namespaceList){
			if(ns.getNamespace().contains(string))
				return true;
		}
		return false;
	}
	
	public void startHeartBeat(){
		lastPacketTime=System.currentTimeMillis();
		beater = new HeartBeat();
		beater.start();
	}
	public void startHeartBeat(long msHeartBeatTime){
		if (msHeartBeatTime<10)
			msHeartBeatTime = 10;
		heartBeatTime= msHeartBeatTime;
		startHeartBeat();
	}
	public void stopHeartBeat(){
		beater=null;
	}
	private BowlerAbstractDevice getInstance(){
		return this;
	}
	private class HeartBeat extends Thread{
		public void run(){
			while (connection.isConnected()){
				if((connection.msSinceLastSend())>heartBeatTime){
					try{
						if(ping()==null){
							Log.debug("Ping failed, disconnecting");
							connection.disconnect();
						}
					}catch(Exception e){
						Log.debug("Ping failed, disconnecting");
						connection.disconnect();
					}
				}
				ThreadUtil.wait(10);
				if(getInstance() == null){
					Log.debug("Instance null, disconnecting");
					connection.disconnect();
				}
			}
		}
	}
	/**
	 * Tells the connection to use asynchronous packets as threads or not. 
	 * @param up
	 */
	public void setThreadedUpstreamPackets(boolean up){
		if(connection != null){
			connection.setThreadedUpstreamPackets(up);
		}
	}
	/**
	 * Requests all of the RPC's from a namespace
	 * @param s
	 * @return
	 */
	public ArrayList<RpcEncapsulation> getRpcList(String namespace) {
		int namespaceIndex = 0;
		boolean hasCoreRpcNS = false;
		for (int i=0;i<namespaceList.size();i++){
			if(namespaceList.get(i).getNamespace().contains(namespace)){
				namespaceIndex=i;
			}
			if(namespaceList.get(i).getNamespace().contains("bcs.rpc.*")){
				hasCoreRpcNS=true;
			}
		}
		if(!hasCoreRpcNS){
			//this device has no RPC namespace, failing out
			Log.info("Device has no RPC identification namespace");
			return new ArrayList<RpcEncapsulation>();
		}
		if(namespaceList.get(namespaceIndex).getRpcList()!=null){
			//fast return if list is already populated
			return namespaceList.get(namespaceIndex).getRpcList();
		}
		
		try{
			//populate RPC set
			BowlerDatagram b = send(new  RpcCommand(namespaceIndex));
			//int ns = b.getData().getByte(0);// gets the index of the namespace
			//int rpcIndex = b.getData().getByte(1);// gets the index of the selected RPC
			int numRpcs = b.getData().getByte(2);// gets the number of RPC's
			if(numRpcs<1){
				Log.error("RPC request failed:\n"+b);
			}else{
				Log.info("Number of RPC's = "+numRpcs);
			}
			Log.debug("There are "+numRpcs+" RPC's in "+namespace);
			namespaceList.get(namespaceIndex).setRpcList(new ArrayList<RpcEncapsulation>());
			for (int i=0;i<numRpcs;i++){
				b = send(new RpcCommand(namespaceIndex,i));
				String rpcStr = new String(b.getData().getBytes(3, 4));
				
				b = send(new RpcArgumentsCommand(namespaceIndex,i));
				
				byte []data = b.getData().getBytes(2);
				BowlerMethod downstreamMethod = BowlerMethod.get(data[0]);
				int numDownArgs = data[1];
				BowlerMethod upstreamMethod   = BowlerMethod.get(data[numDownArgs+2]);
				int numUpArgs = data[numDownArgs+3];
				
				BowlerDataType [] downArgs = new BowlerDataType[numDownArgs];
				BowlerDataType [] upArgs = new BowlerDataType[numUpArgs];
				
				for(int k=0;k<numDownArgs;k++){
					downArgs[k] = BowlerDataType.get(data[k+2]);
				}
				for(int k=0;k<numUpArgs;k++){
					upArgs[k] = BowlerDataType.get(data[k+numDownArgs+4]);
				}
				RpcEncapsulation tmpRpc = new RpcEncapsulation(namespaceIndex,namespace, rpcStr, downstreamMethod,downArgs,upstreamMethod,upArgs);
				System.out.println(tmpRpc);
				namespaceList.get(namespaceIndex).getRpcList().add(tmpRpc);
			}
			
		}catch(InvalidResponseException ex){
			Log.debug("Older version of core, discovery disabled");
		}
		return namespaceList.get(namespaceIndex).getRpcList();
	}
	
	/**
	 * On all response.
	 *
	 * @param data the data
	 */
	@Deprecated
	public void onAllResponse(BowlerDatagram data){
		// this is here to prevent the breaking of an interface, 
	}
	
}
