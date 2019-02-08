import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.Scanner;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JFrame;
import javax.swing.JPanel;

public class DataHandler implements Serializable{
	private static final long serialVersionUID = 1L;
	static MongoUploader mongoUploader;
	static ArrayList<ESP8266> storedDevices;
	static String[] commandList = {
			"setup\t\t: Setup a new insole to connect to my network.",
			"setSide\t: Set existing insole side (Right or Left).",
			"add\t\t: Add a new Insole using its IP address (must be in my network)", 
			"addAll\t: Add all detectable devices in my network to my StoredDevices",
			"remove\t: Remove an insole from the program AND from the network",
			"removeAll\t: Remove all devices from my StoredDevices file",
			"swap\t\t: Swap device locations",
			"stop\t\t: Force stop stream from ALL Insoles in the network",
			"showAll\t: Show all stored devices",
			"refresh\t: Get current network settings from stored devices",
			"export\t: Export the last stored collection to *.csv",
			"autoExp\t: Toggle Auto-export last stored collection",
			"imu\t\t: Toggle IMU display",
			"exit\t\t: Exit the program"};
	static Scanner sc;
	static boolean imuEnabled = false;
	static boolean autoExportEnabled = false;
	
    public static void main(String[] args) {
    	storedDevices = readStoredDevices();
    	System.out.println("System ready!");
    	sc = new Scanner(System.in);
    	String command = "";
    	do{
    		System.out.print("dh> ");
    		command = sc.nextLine().toLowerCase().trim();
    		switch(command){
    			case "start":
    				startStream();
    				break;
    			case "setup":
    				setupDevice();
    				break;
    			case "add":
    				addNewDevice();
    				break;
    			case "addall":
    				addAll();
    				break;
    			case "setside":
    				setSide();
    				break;
    			case "remove":
    				removeDevice();
    				break;
    			case "removeall":
    				removeAll();
    				break;
    			case "stop":
    				forceStopAll();
    				break;
    			case "showall":
    				showAll();
    				break;
    			case "refresh":
    				refresh();
    				break;
    			case "export":
    				exportResults();
    				break;
    			case "imu":
    				toggleIMUDisplay();
    				break;
    			case "autoexport":
    			case "autoexp":
    				toggleAutoExport();
    				break;
    			case "swap":
    				swapDevices();
    				break;
    			case "exit":
    				exit();
    				break;
    			case "":
    				break;
    			default:
    				System.out.println("Incorrect command. List of possible commands:");
    				for(int i=0; i<commandList.length; i++){
    					System.out.print(" -");
    					System.out.println(commandList[i]);
    				}
    		}
    	}while(command != null);
    }
    
    static void setSide(){
    	System.out.println("Stored devices:");
    	for(int i=0; i<storedDevices.size(); i++){
    		System.out.println(i+1+") "+storedDevices.get(i).getName()+"\tSide: "+(char)storedDevices.get(i).getSide());
    	}
    	System.out.println("Which device's side do you want to set? ");
    	int choice = sc.nextInt()-1;
    	System.out.println("Is "+storedDevices.get(choice).getName()+ " a RIGHT or LEFT insole? ");
    	storedDevices.get(choice).setSide(sc.next().trim().toUpperCase().charAt(0));
    	System.out.println("Updated:\n"+storedDevices.get(choice));
    	updateStoredDevices();
    }
    
    static void refresh(){
    	/*
    	 * update device list by getting current network status (stat) from ESP devices
    	 */
    	System.out.println("Refreshing device settings...");
    	 storedDevices = readStoredDevices();//get stored device list
    	 for(int i=0; i<storedDevices.size(); i++){
    		 if(storedDevices.get(i).isOnline()){
    			 storedDevices.get(i).getStatus();
    		 }
    	 }
    	 updateStoredDevices();
    }
    
    static boolean isReachable(String address, int timeout_ms){
    	System.out.println("pinging "+address);
        try {
        	boolean pingResult = InetAddress.getByName(address).isReachable(timeout_ms);
            if (pingResult){
                return true;
            } else {
                return false;
            }
        }catch (Exception e){
            e.printStackTrace();
            return false;
        }
    }
	
    @SuppressWarnings("unchecked")
	private static ArrayList<ESP8266> readStoredDevices(){
    	ArrayList<ESP8266> list = new ArrayList<ESP8266>();
    	try {
			ObjectInputStream ois = new ObjectInputStream(new FileInputStream("DevConfig.ser"));
			list = (ArrayList<ESP8266>)ois.readObject();
			ois.close();
		}catch(FileNotFoundException e){
			new File("DevConfig.ser");
		}catch (Exception e) {
			e.printStackTrace();
		}
    	return list;
    }
    
	private static void updateStoredDevices(){
    	try{
    		ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream("DevConfig.ser"));
    		oos.writeObject(storedDevices);
    		oos.close();
    	}
    	catch(Exception e){
    		e.printStackTrace();
    	}
    }

	private static void setupDevice(){
		System.out.println("Connect to that device's WiFi then press ENTER to continue...");
		sc.next();
		ESP8266 esp = new ESP8266("192.168.4.1"); //default ESP IP address in its local network
		System.out.print("Enter SSID to connect: ");
		String SSID = sc.next();
		System.out.print("Enter password for "+SSID+": ");
		String PW = sc.next();
		System.out.println("Connecting, please wait...");
		if(esp.connectWiFi(SSID, PW)){
			storedDevices.add(esp);
			updateStoredDevices();
			System.out.println("Device connected successfully");
		}
		else{
			System.out.println("ERROR: Cannot complete process. Operation cancelled.");
		}
	}
	
    private static void addNewDevice(){
    	System.out.print("Enter new device IP: ");
    	String deviceIP = sc.nextLine();
    	System.out.println("Adding device ("+deviceIP+"), please wait...");
    	boolean pingResult = isReachable(deviceIP, 1000);
    	
    	if(pingResult == true){
    		ESP8266 esp = new ESP8266(deviceIP); //temporary name it by its IP address
        	esp.setLocalIP(deviceIP);
        	storedDevices.add(esp);
			esp.getStatus(); //updates device info
			updateStoredDevices();
			System.out.println(esp.getName()+" successfully added!");
    	}else{
    		System.out.println("ERROR:" + deviceIP +" cannot be reached.");
    	}
    }
    
    private static void addAll(){
    	System.out.println("Run automatic device detection? (Y/N)");
    	String input = sc.nextLine().trim().toLowerCase();
    	System.out.println("How many devices are expected?");
    	int maxDevices = sc.nextInt();
    	ESP8266 esp;
    	int count = 0;
    	if(input.contains("y")){
    		System.out.println("Scanning for devices in your network...");
    		String subIP = "192.168.0.";
    		for(int i=100; i<150 && count<maxDevices; i++){
	            if(isReachable(subIP+i, 1000)){
	            	esp = new ESP8266("");
            		esp.setLocalIP(subIP+i);
	            	try{
	            		esp.getStatus();
	            		if(esp.getGatewaySSID().contains("ESP")){//it's an ESP device	            			
	            			if(!storedDevices.contains(esp)){//it's not already stored
		            			storedDevices.add(esp);
		            			updateStoredDevices();
		            			System.out.println(esp.getName()+" added");
		            			count++;
	            			}
	            		}
	            	}
	            	catch(Exception e){
	            		e.printStackTrace();
	            	}
	            }
    		}
    		System.out.println(count + " devices added to StoredDevices!");
    	}
    	else{
    		System.out.println("Auto-detection cancelled");
    	}
    }
    
    private static void removeDevice(){
    	System.out.print("Enter device IP or Name: ");
    	String deviceDesc = sc.nextLine();
    	boolean success = false;
    	ESP8266 esp;
    	for(int i=0; i<storedDevices.size(); i++){
    		esp = storedDevices.get(i);
    		if(esp.getName().equals(deviceDesc) || esp.getLocalIP().equals(deviceDesc)){
    			//esp.disconnect();
    			storedDevices.remove(i);
    			updateStoredDevices();
    			success = true;
    		}
    	}
    	if(success){
    		System.out.println(deviceDesc + " successfully removed");
    	}
    	else{
    		System.out.println("ERROR: "+ deviceDesc + " cannot be found");
    	}
    }
    
    private static void removeAll(){
    	System.out.println("Remove all stored devices? (Y/N)");
    	String input = sc.nextLine().trim().toLowerCase();
    	if(input.contains("y")){
    		System.out.println("Removing " + storedDevices.size() + " devices removed from storage...");
    		while(storedDevices.size()>0){
	    		storedDevices.remove(storedDevices.size()-1);
	    	}
    		System.out.println("Remove complete!");
	    	updateStoredDevices();
	    	
    	}
    	else{
    		System.out.println("Operation cancelled");
    	}
    }
    
    private static void swapDevices(){
    	System.out.println("Stored devices:");
    	for(int i=0; i<storedDevices.size(); i++){
    		System.out.println(i+1+") "+storedDevices.get(i).getName()+"\tSide: "+(char)storedDevices.get(i).getSide());
    	}
    	System.out.println("Enter the numbers of the 2 devices to swap");
    	System.out.print("1st device #: ");
    	int dev1 = sc.nextInt()-1;
    	System.out.print("2nd device #: ");
    	int dev2 = sc.nextInt()-1;
    	Collections.swap(storedDevices, dev1, dev2);
    	System.out.println("Swapped devices:");
    	System.out.println(storedDevices.get(dev2).getName()+" <-> "+storedDevices.get(dev1).getName());
    	updateStoredDevices();
    }
	
    private static void forceStopAll(){
    	System.out.println("Force stop all devices? (Y/N)");
    	String input = sc.nextLine().trim().toLowerCase();
    	if(input.contains("y")){
    		System.out.println("Force stop in progress...");
    		try{
	    		DatagramSocket socket = new DatagramSocket();
	    		String subIP = "192.168.0.";
	    		for(int i=1; i<255; i++){
		            byte[] stopBuffer = "STOP".getBytes();
		            DatagramPacket stopPacket = new DatagramPacket(stopBuffer, stopBuffer.length, InetAddress.getByName(subIP+i), 55555); //55555 is default ESP stream port
		            for(int j=0; j<10; j++){//send STOP signal 10 times
		            	socket.send(stopPacket);
		            }
		            if(i%10==0){
		            	System.out.print(".");
		            }
	    		}
	    		socket.close();
	    		System.out.println("\nForce stop complete!");
    		}catch(Exception e){
    			e.printStackTrace();
    		}
    	}
    	else{
    		System.out.println("Force stop cancelled");
    	}
    }
    
    private static void showAll(){
    	int count=0;
    	System.out.println(storedDevices.size()+" Stored Device(s):\n");
		for(int i=0; i<storedDevices.size(); i++){
			if(storedDevices.get(i).isOnline()){
				System.out.println(i+1+") ONLINE");
				count++;
			}
			else{
				System.out.println(i+1+") OFFLINE");
			}
			System.out.println(storedDevices.get(i));
		}
		System.out.println(count +"/"+storedDevices.size()+" devices ONLINE!\n");
    }

    static void exportResults(){
    	try {
    		if(mongoUploader!=null){
	    		String exportString = "mongoexport -h localhost:27017 -d "+mongoUploader.getDatabaseName()+" -c "+mongoUploader.getCollectionName()+" -o "+mongoUploader.getCollectionName()+".csv";
	        	int result = Runtime.getRuntime().exec(exportString).waitFor();
	        	if(result == 0){
	        		System.out.println("Export succeessful. File name: "+mongoUploader.getCollectionName()+".csv");
	        	}
	        	else{
	        		System.out.println("Export not successful");
	        	}
    		}
    		else{
    			System.out.println("No mongoUploader instance found.");
    		}
        }catch (Exception e){
        	System.out.println("ERROR: Could not export!");
            e.printStackTrace();
        }
    }
    
    private static void toggleIMUDisplay(){
    	if(imuEnabled){
    		imuEnabled = false;
    		System.out.println("IMU display DISABLED");
    	}
    	else{
    		imuEnabled = true;
    		System.out.println("IMU display ENABLED");
    	}
    }
    
    private static void toggleAutoExport(){
    	if(autoExportEnabled){
    		autoExportEnabled = false;
    		System.out.println("Auto-export DISABLED");
    	}
    	else{
    		autoExportEnabled = true;
    		System.out.println("Auto-export ENABLED");
    	}
    }
    
    private static void exit(){
    	System.out.println("bye");
    	try {
			Thread.sleep(1000);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
    	sc.close();
    	System.exit(0);
    }
    
    private static void startStream(){
    	ArrayList<ESP8266> displayDevices = new ArrayList<ESP8266>();
		
    	//Create Main Panel
    	JFrame frame = new JFrame();
    	JPanel mainPanel = new JPanel();
    	frame.add(mainPanel);
    	mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.X_AXIS));
    	
    	//Check who's online
		String temp = "Starting stream from: ";
		//add online devices to displayDevices list
		for(int i=0; i<storedDevices.size(); i++){
			if(storedDevices.get(i).isOnline()){
				displayDevices.add(storedDevices.get(i));
				temp+= storedDevices.get(i).getName() + ", ";
			}			
		}
		System.out.println(temp+"\nClose frame to stop streaming");
		
		//check database connectivity
		
		
		//Create display panels
		HeatmapPanel[] heatmaps = new HeatmapPanel[displayDevices.size()];
		IMUVisualizerPanel[] cuboids = new IMUVisualizerPanel[displayDevices.size()];
		
		for(int i=0; i<displayDevices.size(); i++){
			if(displayDevices.get(i).getSide()==ESP8266.RIGHT){
				heatmaps[i] = new HeatmapPanel(HeatmapPanel.RIGHT);
				heatmaps[i].title.setText(displayDevices.get(i).getName()+" (RIGHT)");
			}
			else{//LEFT by default
				heatmaps[i] = new HeatmapPanel(HeatmapPanel.LEFT);
				heatmaps[i].title.setText(displayDevices.get(i).getName()+" (LEFT)");
			}
			if(imuEnabled){
				cuboids[i] = new IMUVisualizerPanel();
			}
			mainPanel.add(Box.createHorizontalStrut(10));
			mainPanel.add(heatmaps[i]);
			if(imuEnabled){
				mainPanel.add(cuboids[i]);
			}
			mainPanel.add(Box.createHorizontalStrut(10));
		}
    	//Final Frame setup
    	frame.pack();
    	frame.setResizable(false);
    	frame.setTitle("Live Data Stream: "+ new SimpleDateFormat("kk:mm:ss").format(new Date()));
    	frame.setVisible(true);
    	frame.setLocation(250, 250);
    	frame.toFront();
    	
        //create displayThread: updates all fields
        Thread displayThread = new Thread(){
        	@Override
        	public void run(){
        		ESP8266 currentDevice; //placeholder for ESP device
        		
                while(!isInterrupted()){
                	for(int deviceIndex=0; deviceIndex<displayDevices.size(); deviceIndex++){
                		currentDevice = displayDevices.get(deviceIndex);
                		for(int sensorIndex=0; sensorIndex<ESP8266.numberOfFSRSensors; sensorIndex++) {
            				heatmaps[deviceIndex].heatpoints[sensorIndex].setValue(currentDevice.FSRReadings[sensorIndex]);
                		}
                		if(imuEnabled){
                			cuboids[deviceIndex].repaint(currentDevice.IMUReadings);
                		}
                		for(int sensorIndex=0; sensorIndex<ESP8266.numberOfIMUSensors; sensorIndex++){
                			heatmaps[deviceIndex].imuFields[sensorIndex].setText(currentDevice.IMUReadings[sensorIndex]+"");
                		}
                	}
                }
                
                for(int i=0; i<displayDevices.size(); i++){
                	displayDevices.get(i).stopSensorStream();
                    System.out.println("[DataHandler]"+displayDevices.get(i).getName()+":Stream stop requested");
                }
        	}
        };
        
        if(displayDevices.size()>0){ //if there's at least one alive device
        	mongoUploader = new MongoUploader();//local db uploader
        	mongoUploader.start();
        	//start stream reachable devices
    		for(int i=0; i<displayDevices.size(); i++){
    			displayDevices.get(i).startSensorStream(5000+i);
    		}
        }
        else{
        	System.out.println("ERROR: No devices defined OR devices cannot be reached. Cannot start stream");
        }
        
        //start display thread
        displayThread.start();

        frame.setVisible(true);
        
        //Stop streaming if frame closed
    	frame.addWindowListener(new java.awt.event.WindowAdapter() {
    	    @Override
    	    public void windowClosing(java.awt.event.WindowEvent windowEvent) {
    	    	if(mongoUploader!=null){
    	    		mongoUploader.finish();
    	    	}
    	    	if(displayThread != null && displayThread.isAlive()){
    	    		displayThread.interrupt();
    	    	}
    	    }});
    }
}