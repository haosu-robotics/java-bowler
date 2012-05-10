package com.neuronrobotics.sdk.addons.kinematics;

import java.awt.Color;

import javax.swing.JFrame;

import com.neuronrobotics.sdk.addons.kinematics.DHChain;
import com.neuronrobotics.sdk.addons.kinematics.DhInverseSolver;
import com.neuronrobotics.sdk.addons.kinematics.gui.SimpleTransformViewer;
import com.neuronrobotics.sdk.addons.kinematics.math.TransformNR;

public class ComputedGeometricModel  implements DhInverseSolver{
	private DHChain dhChain;
	private boolean debug;
	static SimpleTransformViewer viewer = new  SimpleTransformViewer();
	static JFrame frame = new JFrame();
	public ComputedGeometricModel(DHChain dhChain, boolean debug) {
		this.dhChain = dhChain;
		this.setDebug(debug);
		frame.add(viewer);
		frame.setSize(720, 640);
		frame.setVisible(true);
		
	}
	
	public double[] inverseKinematics(TransformNR target,double[] jointSpaceVector ) {
		viewer.addTransform(target, "Target",Color.pink);
		
		int linkNum = jointSpaceVector.length;
		double [] inv = new double[linkNum];
		if(!checkSphericalWrist() || dhChain.getLinks().size() != 6) {
			throw new RuntimeException("This is not a 6DOF arm with a spherical wrist, this solver will not work");
		}
        double theta[] = new double[6];
        double alpha[] = new double[6];
        double r[] = new double[6];
        double d[] = new double[6];
        for(int i=0;i<6;i++){
        	DHLink l = dhChain.getLinks().get(i);
        	theta[i]=l.getTheta();
        	alpha[i]=l.getAlpha();
        	d[i]=l.getD();
        	r[i]=l.getR();
        	inv[i]=0;
        }
        
        TransformNR sphericalCenter = new TransformNR(dhChain.getLinks().get(5).DhStepInverseRotory(target.getMatrixTransform(), 0));
        
        viewer.addTransform(sphericalCenter, "SC",Color.GREEN);
        

		return inv;
	}

	private boolean checkSphericalWrist() {
		int end = dhChain.getLinks().size()-1;
		return 	dhChain.getLinks().get(end).getR()	==0 && 
				dhChain.getLinks().get(end-1).getR()==0 && 
				dhChain.getLinks().get(end-2).getR()==0  ;
	}

	public void setDebug(boolean debug) {
		this.debug = debug;
	}

	public boolean isDebug() {
		return debug;
	}
}