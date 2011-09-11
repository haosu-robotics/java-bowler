package com.neuronrobotics.addons.driving;

import com.neuronrobotics.sdk.dyio.peripherals.ServoChannel;
import com.neuronrobotics.sdk.pid.IPIDEventListener;
import com.neuronrobotics.sdk.pid.PIDChannel;
import com.neuronrobotics.sdk.pid.PIDEvent;
import com.neuronrobotics.sdk.pid.PIDLimitEvent;

public class AckermanBot extends AbstractDrivingRobot {
	private final AckermanConfiguration config = new AckermanConfiguration();
	private int currentDriveTicks=0;
	/**
	 * steeringAngle in radians
	 */
	protected double steeringAngle=0;
	ServoChannel steering;
	PIDChannel drive;
	public AckermanBot(){
		
	}
	
	public AckermanBot(ServoChannel s,PIDChannel d) {
		drive=d;
		steering=s;
		drive.addPIDEventListener(this);
		
	}
	
	public void setSteeringAngle(double s) {
		steeringAngle = s;
		steering.SetPosition((int) (steeringAngle*config.getSteerAngleToServo()));
	}
	public double getSteeringAngle() {
		return steeringAngle;
	}
	protected void SetDriveDistance(int ticks, double seconds){
		drive.SetPIDSetPoint(ticks, seconds);
	}
	protected void ResetDrivePosition(){
		drive.ResetPIDChannel(0);
	}
	
	@Override
	public void DriveStraight(double cm, double seconds) {
		ResetDrivePosition();
		setSteeringAngle(0);
		SetDriveDistance((int) (cm*config.getCmtoTicks()),seconds);
	}
	@Override
	public void DriveArc(double cmRadius, double degrees, double seconds) {
		ResetDrivePosition();
		double archlen = cmRadius*((2*Math.PI*degrees)/(360));
		System.out.println("Running archLen="+archlen);
		double steerAngle =((config.getWheelbase()/cmRadius));
		setSteeringAngle(steerAngle);
		SetDriveDistance((int) (archlen*config.getCmtoTicks()),seconds);
	}

	public double getMaxTicksPreSecond() {
		return config.getMaxTicksPerSeconds();
	}

	@Override
	public void onPIDEvent(PIDEvent e) {
		System.out.println("\n\n");
		double differenceTicks = (e.getValue()-currentDriveTicks);
		double archLen = differenceTicks/config.getCmtoTicks();
		
		double radiusOfCurve=0;
		double centralAngleRadians=0;
		double deltLateral=0;
		double deltForward=0;
		if(getSteeringAngle() !=0){
			radiusOfCurve = config.getWheelbase()/getSteeringAngle();
			centralAngleRadians = archLen/radiusOfCurve;
			System.out.println("Central angle of motion was: "+Math.toDegrees(centralAngleRadians) + " Radius of curve = "+radiusOfCurve);
			deltLateral = -1*(radiusOfCurve*Math.sin(centralAngleRadians));
			deltForward = radiusOfCurve-(radiusOfCurve*Math.cos(centralAngleRadians));
		}else{
			System.out.println("Steering angle of 0, moving forward");
			deltLateral =  0;
			deltForward =  archLen;
		}
		
		System.out.println("Relative motion delta Ticks="+differenceTicks+", forward="+deltForward+", lateral="+deltLateral);
		
		double x = getCurrentX();
		double y = getCurrentY();
		double o = getCurrentTheta();
		
		x+=deltForward*Math.cos(o);
		y+=deltForward*Math.sin(o);
		
		x+=deltLateral*Math.sin(o);
		y+=deltLateral*Math.cos(o);
		
		setCurrentX(x);
		setCurrentY(y);
		setCurrentTheta(o+centralAngleRadians);
		
		currentDriveTicks=e.getValue();
	}

	@Override
	public void onPIDLimitEvent(PIDLimitEvent e) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onPIDReset(int group, int currentValue) {
		if(group==0){
			currentDriveTicks=0;
		}
	}
}
