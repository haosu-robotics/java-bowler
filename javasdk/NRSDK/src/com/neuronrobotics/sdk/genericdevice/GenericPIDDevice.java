package com.neuronrobotics.sdk.genericdevice;

import java.util.ArrayList;

import com.neuronrobotics.sdk.commands.bcs.io.GetChannelModeCommand;
import com.neuronrobotics.sdk.commands.bcs.pid.ConfigurePIDCommand;
import com.neuronrobotics.sdk.commands.bcs.pid.ControlAllPIDCommand;
import com.neuronrobotics.sdk.commands.bcs.pid.ControlPIDCommand;
import com.neuronrobotics.sdk.commands.bcs.pid.KillAllPIDCommand;
import com.neuronrobotics.sdk.commands.bcs.pid.PDVelocityCommand;
import com.neuronrobotics.sdk.commands.bcs.pid.ResetPIDCommand;
import com.neuronrobotics.sdk.common.BowlerAbstractConnection;
import com.neuronrobotics.sdk.common.BowlerAbstractDevice;
import com.neuronrobotics.sdk.common.BowlerDatagram;
import com.neuronrobotics.sdk.common.ByteList;
import com.neuronrobotics.sdk.common.IConnectionEventListener;
import com.neuronrobotics.sdk.common.Log;
import com.neuronrobotics.sdk.common.MACAddress;
import com.neuronrobotics.sdk.pid.IPIDControl;
import com.neuronrobotics.sdk.pid.IPIDEventListener;
import com.neuronrobotics.sdk.pid.PIDChannel;
import com.neuronrobotics.sdk.pid.PIDCommandException;
import com.neuronrobotics.sdk.pid.PIDConfiguration;
import com.neuronrobotics.sdk.pid.PIDEvent;
import com.neuronrobotics.sdk.pid.PIDLimitEvent;

/**
 * This class is a generic implementation of the PID system. This can be used as a template, superclass or internal object class for 
 * use with and device that implements the IPIDControl interface. 
 * @author hephaestus
 *
 */
public class GenericPIDDevice extends BowlerAbstractDevice implements IPIDControl {
	protected ArrayList<PIDChannel> channels = new ArrayList<PIDChannel>();
	protected long [] lastPacketTime = null;
	
	
	public GenericPIDDevice(BowlerAbstractConnection connection) {
		setAddress(new MACAddress(MACAddress.BROADCAST));
		setConnection(connection);
	}

	public GenericPIDDevice() {
		addPIDEventListener(new IPIDEventListener() {
			public void onPIDReset(int group, int currentValue) {}
			public void onPIDLimitEvent(PIDLimitEvent e) {}
			public void onPIDEvent(PIDEvent e) {
				getPIDChannel(e.getGroup()).setCurrentCachedPosition(e.getValue());
			}
		});
	}
	
	@Override
	public void setConnection(BowlerAbstractConnection connection) {
		super.setConnection(connection);
		if(connection.isConnected())
			GetAllPIDPosition();
	}
	
	@Override
	public boolean connect(){
		if(super.connect()){
			GetAllPIDPosition();
			return true;
		}
		return false;
	}
	public void onAllResponse(BowlerDatagram data) {

	}

	public void onAsyncResponse(BowlerDatagram data) {
		if(data.getRPC().contains("_pid")){
			PIDEvent e =new PIDEvent(data);
	
			firePIDEvent(e);
		}
		if(data.getRPC().contains("apid")){
			int [] pos = new int[getNumberOfChannels()];
			for(int i=0;i<getNumberOfChannels();i++) {
				pos[i] = ByteList.convertToInt( data.getData().getBytes(i*4, (i*4)+4),true);
				PIDEvent e =new PIDEvent(i,pos[i],0,0);
				firePIDEvent(e);
			}	
		}
		if(data.getRPC().contains("pidl")){
			firePIDLimitEvent(new PIDLimitEvent(data));
		}

	}
	/* (non-Javadoc)
	 * @see com.neuronrobotics.sdk.pid.IPIDControl#SetPIDSetPoint
	 */
	public boolean SetPIDSetPoint(int group,int setpoint,double seconds){
		getPIDChannel(group).setCachedTargetValue(setpoint);
		Log.info("Setting PID position group="+group+", setpoint="+setpoint+" ticks, time="+seconds+" sec.");
		return send(new  ControlPIDCommand((char) group,setpoint, seconds))!=null;
	}
	/* (non-Javadoc)
	 * @see com.neuronrobotics.sdk.pid.IPIDControl#SetAllPIDSetPoint
	 */
	public boolean SetAllPIDSetPoint(int []setpoints,double seconds){
		int[] sp;
		if(setpoints.length<getNumberOfChannels()) {
			sp = new int[getNumberOfChannels()];
			for(int i=0;i<sp.length;i++) {
				sp[i]=getPIDChannel(i).getCachedTargetValue();
			}
			for(int i=0;i<setpoints.length;i++) {
				sp[i]=setpoints[i];
			}
		}else {
			sp=setpoints;
		}
		for(int i=0;i<getNumberOfChannels();i++){
			getPIDChannel(i).setCachedTargetValue(sp[i]);
		}
		return send(new  ControlAllPIDCommand(sp, seconds))!=null;
	}
	/* (non-Javadoc)
	 * @see com.neuronrobotics.sdk.pid.IPIDControl#GetPIDPosition
	 */
	public int GetPIDPosition(int group) {
		BowlerDatagram b = send(new  ControlPIDCommand((char) group));
		return ByteList.convertToInt(b.getData().getBytes(1, 4),true);
	}

	public int GetCachedPosition(int group) {
		return getPIDChannel(group).getCurrentCachedPosition();
	}

	public void SetCachedPosition(int group, int value) {

		getPIDChannel(group).setCurrentCachedPosition(value);
	}
	/* (non-Javadoc)
	 * @see com.neuronrobotics.sdk.pid.IPIDControl#GetAllPIDPosition
	 */
	public int [] GetAllPIDPosition() {
		BowlerDatagram b = send(new ControlAllPIDCommand());
		ByteList data = b.getData();
		int [] back = new int[data.size()/4];
		for(int i=0;i<back.length;i++) {
			back[i] = ByteList.convertToInt( data.getBytes(i*4, (i*4)+4),true);
		}
		if(back.length != getNumberOfChannels()){
			channels =  new ArrayList<PIDChannel>();
			lastPacketTime =  new long[back.length];
			for(int i=0;i<back.length;i++){
				PIDChannel c =new PIDChannel(this,i);
				c.setCachedTargetValue(back[i]);
				channels.add(c);
			}
		}
		return back;
	}
	
	/* (non-Javadoc)
	 * @see com.neuronrobotics.sdk.pid.IPIDControl#ConfigurePIDController
	 */
	public boolean ConfigurePIDController(PIDConfiguration config) {
		return send(new  ConfigurePIDCommand(config))!=null;
	}

	/* (non-Javadoc)
	 * @see com.neuronrobotics.sdk.pid.IPIDControl#getPIDConfiguration
	 */
	public PIDConfiguration getPIDConfiguration(int group) {
		BowlerDatagram conf = send(new ConfigurePIDCommand( (char) group) );
		PIDConfiguration back=new PIDConfiguration (conf);
		return back;
	}

	public boolean ResetPIDChannel(int group) {
		BowlerDatagram rst = send(new  ResetPIDCommand((char) group));
		if(rst==null)
			return false;
		int val = GetPIDPosition(group);
		firePIDResetEvent(group,val);
		return true;
	}

	/* (non-Javadoc)
	 * @see com.neuronrobotics.sdk.pid.IPIDControl#ResetPIDChannel
	 */
	public boolean ResetPIDChannel(int group, int valueToSetCurrentTo) {
		BowlerDatagram rst = send(new  ResetPIDCommand((char) group,valueToSetCurrentTo));
		if(rst==null)
			return false;
		int val = GetPIDPosition(group);
		firePIDResetEvent(group,val);
		return true;
	}
	
	

	/* (non-Javadoc)
	 * @see com.neuronrobotics.sdk.pid.IPIDControl#flushPIDChannels
	 */
	@Override
	public void flushPIDChannels(double time) {
		int [] data = new int[getNumberOfChannels()];
		for(int i=0;i<getNumberOfChannels();i++){
			data[i]=getPIDChannel(i).getCachedTargetValue();
		}
		Log.info("Flushing in "+time+"ms");
		SetAllPIDSetPoint(data, time);
	}
	/* (non-Javadoc)
	 * @see com.neuronrobotics.sdk.pid.IPIDControl#SetPIDInterpolatedVelocity
	 */
	@Override
	public boolean SetPIDInterpolatedVelocity(int group, int unitsPerSecond, double seconds) throws PIDCommandException {
		long dist = (long)unitsPerSecond*(long)seconds;
		long delt = ((long) (GetCachedPosition(group))-dist);
		if(delt>2147483646 || delt<-2147483646){
			throw new PIDCommandException("(Current Position) - (Velocity * Time) too large: "+delt+"\nTry resetting the encoders");
		}
		return SetPIDSetPoint(group, (int) delt, seconds);
	}
	/* (non-Javadoc)
	 * @see com.neuronrobotics.sdk.pid.IPIDControl#SetPDVelocity
	 */
	@Override
	public boolean SetPDVelocity(int group, int unitsPerSecond, double seconds)throws PIDCommandException {
		try{
			Log.debug("Setting hardware velocity control");
			return send(new PDVelocityCommand(group, unitsPerSecond, seconds))!=null;
		}catch (Exception ex){
			Log.error("Failed! Setting interpolated velocity control..");
			return SetPIDInterpolatedVelocity( group, unitsPerSecond,  seconds);
		}
	}
	/**
	 * Gets the number of PID channels availible to the system. It is determined by how many PID channels the device reports
	 * back after a calling GetAllPIDPosition();
	 * @return
	 */
	public int getNumberOfChannels(){
		return channels.size();
	}
	/* (non-Javadoc)
	 * @see com.neuronrobotics.sdk.pid.IPIDControl#getPIDChannel
	 */
	@Override
	public PIDChannel getPIDChannel(int group) {
		if(getNumberOfChannels()==0) {
			GetAllPIDPosition();
		}
		while(!(group < getNumberOfChannels() )){
			PIDChannel c =new PIDChannel(this,group);
			channels.add(c);
		}
		return channels.get(group);
	}
	/* (non-Javadoc)
	 * @see com.neuronrobotics.sdk.pid.IPIDControl#illAllPidGroups
	 */
	@Override
	public boolean killAllPidGroups() {
		getConnection().setSleepTime(10000);
		return send(new KillAllPIDCommand())==null;
	}
	
	private ArrayList<IPIDEventListener> PIDEventListeners = new ArrayList<IPIDEventListener>();
	/* (non-Javadoc)
	 * @see com.neuronrobotics.sdk.pid.IPIDControl#addPIDEventListener
	 */
	public void addPIDEventListener(IPIDEventListener l) {
		synchronized(PIDEventListeners){
			if(!PIDEventListeners.contains(l))
				PIDEventListeners.add(l);
		}
	}
	/* (non-Javadoc)
	 * @see com.neuronrobotics.sdk.pid.IPIDControl#removePIDEventListener
	 */
	public void removePIDEventListener(IPIDEventListener l) {
		synchronized(PIDEventListeners){
			if(PIDEventListeners.contains(l))
				PIDEventListeners.remove(l);
		}
	}
	public void firePIDLimitEvent(PIDLimitEvent e){
		synchronized(PIDEventListeners){
			for(IPIDEventListener l: PIDEventListeners)
				l.onPIDLimitEvent(e);
		}
		//channels.get(e.getGroup()).firePIDLimitEvent(e);
	}
	public void firePIDEvent(PIDEvent e){
		if(lastPacketTime != null){
			if(lastPacketTime[e.getGroup()]>e.getTimeStamp()){
				return;
			}else{
				lastPacketTime[e.getGroup()]=e.getTimeStamp();
			}
		}
		SetCachedPosition(e.getGroup(), e.getValue());
		synchronized(PIDEventListeners){
			for(IPIDEventListener l: PIDEventListeners)
				l.onPIDEvent(e);
		}
		//channels.get(e.getGroup()).firePIDEvent(e);
	}
	public void firePIDResetEvent(int group,int value){
		SetCachedPosition(group, value);
		for(IPIDEventListener l: PIDEventListeners)
			l.onPIDReset(group,value);
		//channels.get(group).firePIDResetEvent(group, value);
	}

}