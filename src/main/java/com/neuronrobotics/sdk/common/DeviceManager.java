package com.neuronrobotics.sdk.common;

import java.util.ArrayList;
import java.util.List;

import com.neuronrobotics.replicator.driver.BowlerBoardDevice;
import com.neuronrobotics.replicator.driver.NRPrinter;
import com.neuronrobotics.sdk.bootloader.NRBootLoader;
import com.neuronrobotics.sdk.bowlercam.device.BowlerCamDevice;
import com.neuronrobotics.sdk.dyio.DyIO;
import com.neuronrobotics.sdk.genericdevice.GenericDevice;
import com.neuronrobotics.sdk.pid.GenericPIDDevice;
import com.neuronrobotics.sdk.ui.ConnectionDialog;
import com.neuronrobotics.sdk.util.ThreadUtil;

public class DeviceManager {
	private static final ArrayList<BowlerAbstractDevice> devices = new ArrayList<BowlerAbstractDevice>();
	private static final ArrayList<IDeviceAddedListener> deviceAddedListener = new ArrayList<IDeviceAddedListener>();
	
	public static void addConnection(final BowlerAbstractDevice newDevice, String name){
		if(devices.contains(newDevice)){
			Log.warning("Device is already added " +newDevice.getScriptingName());
		}
		int numOfThisDeviceType=0;
		
		for (int i = 0; i < devices.size(); i++) {
			if(newDevice.getClass().isInstance(devices.get(i)))
				numOfThisDeviceType++;
		}
		if(numOfThisDeviceType>0)
			name = name+numOfThisDeviceType;
		newDevice.setScriptingName(name);
		devices.add(newDevice);
		newDevice.addConnectionEventListener(new IConnectionEventListener() {
			@Override public void onDisconnect(BowlerAbstractConnection source) {
				DeviceManager.remove(newDevice);
			}
			@Override public void onConnect(BowlerAbstractConnection source) {}
		});
		for(IDeviceAddedListener l :deviceAddedListener){
			l.onNewDeviceAdded(newDevice);
		}
	}
	public static void addConnection(BowlerAbstractConnection connection) {
		if (connection == null) {
			return;
		}

		GenericDevice gen = new GenericDevice(connection);
		try {
			if (!gen.connect()) {
				throw new InvalidConnectionException("Connection is invalid");
			}
			if (!gen.ping(true)) {
				throw new InvalidConnectionException("Communication failed");
			}
		} catch (Exception e) {
			// connection.disconnect();
			ThreadUtil.wait(1000);
			BowlerDatagram.setUseBowlerV4(false);
			if (!gen.connect()) {
				throw new InvalidConnectionException("Connection is invalid");
			}
			if (!gen.ping()) {
				connection = null;
				throw new InvalidConnectionException("Communication failed");
			}
			throw new RuntimeException(e);
		}
		if (gen.hasNamespace("neuronrobotics.dyio.*")) {
			DyIO dyio = new DyIO(gen.getConnection());
			dyio.connect();
			String name = "dyio";

			addConnection(dyio, name);

		} else if (gen.hasNamespace("bcs.cartesian.*")) {
			BowlerBoardDevice delt = new BowlerBoardDevice();
			delt.setConnection(gen.getConnection());
			delt.connect();
			String name = "bowlerBoard";
			addConnection(delt, name);
			addConnection(new NRPrinter(delt), "cnc");
			
		} else if (gen.hasNamespace("bcs.pid.*")) {
			GenericPIDDevice delt = new GenericPIDDevice();
			delt.setConnection(gen.getConnection());
			delt.connect();
			String name = "pid";

			addConnection(delt, name);
		} else if (gen.hasNamespace("bcs.bootloader.*")
				|| gen.hasNamespace("neuronrobotics.bootloader.*")) {
			NRBootLoader delt = new NRBootLoader(gen.getConnection());
			String name = "bootloader";

			addConnection(delt, name);
		} else if (gen.hasNamespace("neuronrobotics.bowlercam.*")) {
			BowlerCamDevice delt = new BowlerCamDevice();
			delt.setConnection(gen.getConnection());
			delt.connect();
			String name = "bowlercam";
			addConnection(delt, name);
		} else {
			addConnection(gen, "device");
		}
		
	}

	public static void addConnection() {
		new Thread() {
			public void run() {
				BowlerDatagram.setUseBowlerV4(true);
				addConnection(ConnectionDialog.promptConnection());
			}
		}.start();
	}

	public static void remove(BowlerAbstractDevice newDevice) {
		if(devices.contains(newDevice)){
			devices.remove(newDevice);
			for(IDeviceAddedListener l :deviceAddedListener){
				l.onDeviceRemoved(newDevice);
			}
		}
	}
	
	public static void addDeviceAddedListener(IDeviceAddedListener l){
		if(!deviceAddedListener.contains(l))
			deviceAddedListener.add(l);
	}
	public static void removeDeviceAddedListener(IDeviceAddedListener l){
		if(deviceAddedListener.contains(l))
			deviceAddedListener.remove(l);
	}
	
	public static BowlerAbstractDevice getSpecificDevice(Class<?> class1, String name){
		List<String> devs =listConnectedDevice( class1);
		if(devs.size()==0)
			return null;
		else
			for (String d:devs) {
				if(d.contentEquals(name)){
					for (int i = 0; i < devices.size(); i++) {
						if(devices.get(i).getScriptingName().contains(d))
							return devices.get(i);
					}
				}
				
			}
		return null;
	}
	
	public static List<String> listConnectedDevice(Class<?> class1){
		List<String> choices = new ArrayList<String>();
		for (int i = 0; i < devices.size(); i++) {
			if(class1==null)
				choices.add(devices.get(i).getScriptingName());
			else if(class1.isInstance(devices.get(i))){
				choices.add(devices.get(i).getScriptingName());
			}
		}
		return choices;
		
	}
}
