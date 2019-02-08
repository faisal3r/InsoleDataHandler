/*
 * Every instance of the ESP8266 class is a profile that describes an Insole device
 * Contains data holders for sensor readings, as well as all necessary variables needed to communicate to that Insole
 */
import java.io.Serializable;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.Collections;
import java.util.List;

class ESP8266 implements Serializable{
	private static final long serialVersionUID = 1L;
	final static char LEFT = 'L';
	final static char RIGHT = 'R';
	final static char NOT_SET = 'N';
	private char side = NOT_SET; //Left, Right, or Not set
	private String gatewaySSID = "";
    private final String gatewayIP = "192.168.4.1";
    private String connectedSSID = "";
    private String localIP = "0.0.0.0";
    private String MACAddress = "";
    private String name = "";
    private final int ESP_NOT_FOUND = 0; //Error code
    private String response;
    private UDPReceiver udpThread;
    final static int numberOfFSRSensors = 12;
    final static int numberOfIMUSensors = 6;
    final int[][] FSRLocationsLeft = { //location array to calculate center of mass
    		{2,7},	//FSR00
    		{1,5},	//FSR01
    		{2,3},	//FSR02
    		{3,1},	//FSR03
    		{6,7},	//FSR04
    		{6,5},	//FSR05
    		{7,3},	//FSR06
    		{8,1},	//FSR07
    		{12,2},	//FSR08
    		{19,6},	//FSR09
    		{20,4},	//FSR10
    		{19,3}	//FSR11
    		};
    final int[][] FSRLocationsRight = { //location array to calculate center of mass
    		{2,2},	//FSR00
    		{1,4},	//FSR01
    		{2,6},	//FSR02
    		{3,8},	//FSR03
    		{6,1},	//FSR04
    		{6,3},	//FSR05
    		{7,6},	//FSR06
    		{8,8},	//FSR07
    		{12,7},	//FSR08
    		{19,3},	//FSR09
    		{20,4},	//FSR10
    		{19,6}	//FSR11
    		};
    int[] FSRReadings = new int[numberOfFSRSensors]; //live FSR sensor readings are stored here when startSensorStream() is called
    float[] IMUReadings = new float[numberOfIMUSensors]; //live IMU sensor readings are stored here when startSensorStream() is called
    float[] centerOfMass = new float[2]; //X,Y location of center of mass
    int averageForce;// live average of FSR values

    ESP8266(String gatewaySSID){
        this.gatewaySSID = gatewaySSID;
    }

    //SETTERS
    private void setGatewaySSID(String gatewaySSID){
        this.gatewaySSID = gatewaySSID;
    }
    private void setConnectedSSID(String SSID){
        this.connectedSSID = SSID;
    }
    void setLocalIP(String IP){
        this.localIP = IP;
    }
    private void setMACAddress(String MACAddress){
        this.MACAddress = MACAddress;
    }
    void setName(String name){
        //i.e. left insole, right dumbbell, oven, etc.
        this.name = name;
    }
    void setSide(char side){
    	if(side==RIGHT || side ==LEFT){ //'R' or 'L'
    		this.side = side;
    	}
    	else{
    		this.side = NOT_SET; //incorrect setting received. N: Not set
    	}
    }

    //GETTERS
    String getGatewaySSID(){
        return this.gatewaySSID;
    }
    String getGatewayIP(){
        return this.gatewayIP;
    }
    String getConnectedSSID(){
        return this.connectedSSID;
    }
    String getLocalIP(){
        return this.localIP;
    }
    String getMACAddress(){
        return this.MACAddress;
    }
    String getName(){
        if(this.name.equals("")) {
            return this.gatewaySSID;
        }
        else{
            return this.name;
        }
    }
    int getSide(){
    	return this.side;
    }

    //PUBLIC METHODS
    String getIP(){
        if(this.localIP.equals("0.0.0.0")){
            return gatewayIP; //default IP for ESP;
        }
        else{
            return this.localIP;
        }
    }
    public String toString(){
    	String str = "GTWY SSID\t: "+gatewaySSID+"-"+side;
    	str+= "\nGTWY IP\t\t: "	+gatewayIP
                + "\nWIFI SSID\t: "	+connectedSSID
                + "\nLOCAL IP\t: "	+localIP
                + "\nMAC ADDR\t: "	+MACAddress;
    	return str;
        
    }
    boolean isOnline(){
    	boolean pingResult;
    	try {
    		pingResult = InetAddress.getByName(this.getIP()).isReachable(1000);
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
    boolean connectWiFi(final String SSID, final String PW){
        boolean connectionResult;
        response = executePost("/conn?data="+SSID+","+PW);
        if(response.toLowerCase().contains("connected")){
            connectionResult = true;}
        else{
            executePost("/disc");
            connectionResult = false;
        }
        this.getStatus();
        return connectionResult;
    }
    String getStatus(){
        //Update Status
        String[] items = this.executePost("/stat").split("\\s*,\\s*");
        this.setGatewaySSID(items[0]);
        //gatewayIP (items[1]) is final 192.168.4.1
        if(items.length>3) {
            this.setConnectedSSID(items[2]);
            this.setLocalIP(items[3]);
            this.setMACAddress(items[4]);
        }
        else {
            this.setConnectedSSID("");
            this.setLocalIP("0.0.0.0");
            this.setMACAddress("");
        }
        return java.util.Arrays.toString(items);
    }
    String disconnect(){
        return executePost("/disc");
    }
    void startSensorStream(int localPort){
        //Request Stream and get serverPort
        String response = this.executePost("/data?data="+getIPAddress()+","+localPort);
        try{
	        int serverPort = Integer.parseInt(response.trim());
	        udpThread = new UDPReceiver(this, localPort, serverPort);
	        udpThread.start();
        }catch(NumberFormatException e){
        	
        }
    }
    void stopSensorStream() {
        if(udpThread != null && udpThread.isAlive()){
            udpThread.stopStream();
        }
    }
    String getAvailableNetworks(){
        return executePost("/scan");
    }

    //PRIVATE METHODS
    private String errorMessage(int msg_id){
        response = "Error!\n";
        switch(msg_id){
            case ESP_NOT_FOUND:
                response+= "["+this.getName()+"] Cannot reach device!\n\n";
                break;
            default:
                response+= "REQUEST ERROR!\n\n";
                break;
        }
        return response;
    }
    private String executePost(final String targetURL){
        HTTPRequest request = new HTTPRequest("http://"+this.getIP()+targetURL);

        if(targetURL.equals("/disc")){//response not required when "disconnect" requested
            response = "No reponse!";
        }
        else {
            try {
                response = request.getResponse(2000); //wait for the HTTPRequest to complete
            } catch (Exception e) {
                response = errorMessage(ESP_NOT_FOUND);
                response += e.toString();
            }
        }

        return response;
    }
    
    private String getIPAddress() {
        try {
            List<NetworkInterface> interfaces = Collections.list(NetworkInterface.getNetworkInterfaces());
            for (NetworkInterface intf : interfaces) {
                List<InetAddress> addrs = Collections.list(intf.getInetAddresses());
                for (InetAddress addr : addrs) {
                    if (!addr.isLoopbackAddress()) {
                        String sAddr = addr.getHostAddress();
                        if(sAddr.contains(this.getIP().substring(0,this.getIP().indexOf(".")))){ //check if we're on the same network
                        	return sAddr;
                        }
                    }
                }
            }
        } catch (Exception ex) { } // for now eat exceptions
        return "";
    }
}
