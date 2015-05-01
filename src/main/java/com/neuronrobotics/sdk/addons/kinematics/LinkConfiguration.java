package com.neuronrobotics.sdk.addons.kinematics;

import org.w3c.dom.Element;

import com.neuronrobotics.sdk.addons.kinematics.xml.XmlFactory;
import com.neuronrobotics.sdk.common.Log;
//import org.w3c.dom.Node;
//import org.w3c.dom.NodeList;
import com.neuronrobotics.sdk.namespace.bcs.pid.IPidControlNamespace;
import com.neuronrobotics.sdk.pid.PIDConfiguration;



public class LinkConfiguration {
	private String name;// = getTagValue("name",eElement);
	private String type;
	private int index;// = Double.parseDouble(getTagValue("index",eElement));
	private int totlaNumberOfLinks=0;
	private int linkIndex = 0;
	//private double length;// = Double.parseDouble(getTagValue("length",eElement));
	private double scale;// = Double.parseDouble(getTagValue("scale",eElement));
	private double upperLimit;// = Double.parseDouble(getTagValue("upperLimit",eElement));
	private double lowerLimit;// = Double.parseDouble(getTagValue("lowerLimit",eElement));
	private double k[] = new double[3];
	private boolean inverted;
	private boolean isLatch;
	private int indexLatch=0;
	private boolean isStopOnLatch;
	private int homingTicksPerSecond;
	private double upperVelocity = 100000000;
	private double lowerVelocity = -100000000;
	
	public LinkConfiguration(Element eElement){
    	setName(XmlFactory.getTagValue("name",eElement));
    	setIndex(Integer.parseInt(XmlFactory.getTagValue("index",eElement)));
    	setScale(Double.parseDouble(XmlFactory.getTagValue("scale",eElement)));
    	setUpperLimit(Double.parseDouble(XmlFactory.getTagValue("upperLimit",eElement)));
    	setLowerLimit(Double.parseDouble(XmlFactory.getTagValue("lowerLimit",eElement)));
    	try{
    		setType(XmlFactory.getTagValue("type",eElement));
    	}catch (NullPointerException e){
    		setType("pid");
    	}
    	if(getType().contains("pid")){
	    	k[0]=Double.parseDouble(XmlFactory.getTagValue("pGain",eElement));
	    	k[1]=Double.parseDouble(XmlFactory.getTagValue("iGain",eElement));
	    	k[2]=Double.parseDouble(XmlFactory.getTagValue("dGain",eElement));
	    	inverted=XmlFactory.getTagValue("isInverted",eElement).contains("true");
	    	setHomingTicksPerSecond(Integer.parseInt(XmlFactory.getTagValue("homingTPS",eElement)));
    	}
    	
    	try{
    		setUpperVelocity(Double.parseDouble(XmlFactory.getTagValue("upperVelocity",eElement)));
    		setLowerVelocity(Double.parseDouble(XmlFactory.getTagValue("lowerVelocity",eElement)));
    	}catch (Exception e){
    		
    	}
    	
    	isLatch=XmlFactory.getTagValue("isLatch",eElement).contains("true");
    	indexLatch=Integer.parseInt(XmlFactory.getTagValue("indexLatch",eElement));
    	isStopOnLatch=XmlFactory.getTagValue("isStopOnLatch",eElement).contains("true");
    	if(indexLatch>getUpperLimit() || indexLatch<getLowerLimit() )
    	    throw new RuntimeException("PID group "+getHardwareIndex()+" Index latch is "+indexLatch+" but needs to be between "+getUpperLimit()+" and "+getLowerLimit());
    	//System.out.println("Interted"+ inverted);
	}
	public LinkConfiguration(Object[] args) {
		setName((String)args[6]);
    	setIndex((Integer)args[0]);
    	setScale((Double)args[5]);
    	setUpperLimit((Integer)args[4]);
    	setLowerLimit((Integer)args[3]);
    	setType("pid");
    	setTotlaNumberOfLinks((Integer)args[1]);
	}
	public String toString(){
		String s="LinkConfiguration: \n\tName: "+getName();
		s+=	"\n\tType: "+getType();
		s+=	"\n\tHardware Board Index: "+getHardwareIndex();
		s+=	"\n\tScale: "+getScale();
		s+=	"\n\tUpper Limit: "+getUpperLimit();
		s+=	"\n\tLower Limit: "+getLowerLimit();
		s+=	"\n\tHoming Ticks Per Second: "+getHomingTicksPerSecond();
		return s;
	}
	

	public void setName(String name) {
		Log.info("Setting controller name: "+name);
		this.name = name;
	}
	public String getName() {
		return name;
	}
	public void setIndex(int index) {
		this.index = index;
	}
	public int getHardwareIndex() {
		return index;
	}

	public void setScale(double scale) {
		this.scale = scale;
	}
	public double getScale() {
		return scale;
	}

	public void setUpperLimit(double upperLimit) {
		this.upperLimit = upperLimit;
	}
	public double getUpperLimit() {
		return upperLimit;
	}
	public void setLowerLimit(double lowerLimit) {
		this.lowerLimit = lowerLimit;
	}
	public double getLowerLimit() {
		return lowerLimit;
	}
	public double getKP() {
		return k[0];
	}
	public double getKI() {
		return k[1];
	}
	public double getKD() {
		return k[2];
	}
	public void setKP(double kP) {
		k[0] = kP;
	}
	public void setKI(double kI) {
		k[1] = kI;
	}
	public void setKD(double kD) {
		k[2] = kD;
	}
	public void setInverted(boolean inverted) {
		this.inverted = inverted;
	}
	public boolean isInverted() {
		return inverted;
	}
	public void setIndexLatch(int indexLatch) {
		this.indexLatch = indexLatch;
	}
	public int getIndexLatch() {
		return indexLatch;
	}
	public void setLatch(boolean isLatch) {
		this.isLatch = isLatch;
	}
	public boolean isLatch() {
		return isLatch;
	}
	public void setStopOnLatch(boolean isStopOnLatch) {
		this.isStopOnLatch = isStopOnLatch;
	}
	public boolean isStopOnLatch() {
		return isStopOnLatch;
	}
	public void setHomingTicksPerSecond(int homingTicksPerSecond) {
		this.homingTicksPerSecond = homingTicksPerSecond;
	}
	public int getHomingTicksPerSecond() {
		return homingTicksPerSecond;
	}
	public void setType(String type) {
		if(type!=null)
			this.type = type;
		else
			this.type="pid";
	}
	public String getType() {
		return type.toLowerCase();
	}
	public void setUpperVelocity(double upperVelocity) {
		this.upperVelocity = upperVelocity;
	}
	public double getUpperVelocity() {
		return upperVelocity;
	}
	public void setLowerVelocity(double lowerVelocity) {
		this.lowerVelocity = lowerVelocity;
	}
	public double getLowerVelocity() {
		return lowerVelocity;
	}
	public int getLinkIndex() {
		return linkIndex;
	}
	public void setLinkIndex(int linkIndex) {
		this.linkIndex = linkIndex;
	}
	public int getTotlaNumberOfLinks() {
		return totlaNumberOfLinks;
	}
	public void setTotlaNumberOfLinks(int totlaNumberOfLinks) {
		this.totlaNumberOfLinks = totlaNumberOfLinks;
	}
	
	public PIDConfiguration getPidConfiguration(){
		PIDConfiguration pid = new PIDConfiguration();
		pid.setKD(getKD());
		pid.setGroup(getHardwareIndex());
		pid.setStopOnIndex(isStopOnLatch());
		pid.setKI(getKI());
		pid.setKP(getKP());
		pid.setIndexLatch(getIndexLatch());
		pid.setInverted(isInverted());
		return pid;
	}
	public void setPidConfiguration(IPidControlNamespace pid) {
		PIDConfiguration conf = pid.getPIDConfiguration(getHardwareIndex());
    	if(getType().contains("pid")){
	    	k[0]=conf.getKP();
	    	k[1]=conf.getKI();
	    	k[2]=conf.getKD();
	    	inverted=conf.isInverted();
	    	setHomingTicksPerSecond(10000);
    	}
    	
    	isLatch=conf.isUseLatch();
    	indexLatch=(int) conf.getIndexLatch();
    	isStopOnLatch=conf.isStopOnIndex();
//    	if(indexLatch>getUpperLimit() || indexLatch<getLowerLimit() )
//    	    throw new RuntimeException("PID group "+getHardwareIndex()+" Index latch is "+indexLatch+" but needs to be between "+getUpperLimit()+" and "+getLowerLimit());
    	
	}
	
}