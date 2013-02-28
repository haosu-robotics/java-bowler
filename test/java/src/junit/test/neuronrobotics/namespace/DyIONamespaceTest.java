package junit.test.neuronrobotics.namespace;

import static org.junit.Assert.*;

import org.junit.Before;
import org.junit.Test;

import com.neuronrobotics.sdk.common.ByteList;
import com.neuronrobotics.sdk.dyio.DyIO;
import com.neuronrobotics.sdk.dyio.DyIORegestry;
import com.neuronrobotics.sdk.ui.ConnectionDialog;

public class DyIONamespaceTest {
	
	@Before
	public void setUp() throws Exception {
		if(!DyIORegestry.get().isAvailable()){
			if(ConnectionDialog.getBowlerDevice(DyIORegestry.get())){
				return;
			}
		}else{
			return;
		}
		fail("No device availible");
	}

	@Test
	public void dyioNamespaceTest() {
		DyIO dyio= DyIORegestry.get();
		
		if(!dyio.isAvailable())
			fail();
		assertTrue(DyIORegestry.get().hasNamespace("neuronrobotics.dyio.*"));
		
		dyio.enableDebug();
		
		dyio.getRevisions();
		
		String name = dyio.getInfo();
		String setName ="My DyIO";
		
		dyio.setInfo(setName);
		
		String newName = dyio.getInfo();
		
		dyio.disableDebug();
		dyio.setInfo(name);
		dyio.enableDebug();
		
		double volts = dyio.getBatteryVoltage(true);
		
		dyio.setServoPowerSafeMode(true);
	
		assertTrue(setName.contains(newName));
		assertTrue(name.contains(dyio.getInfo()));
		
		
	}

}