/*
 * This class is a thread associated with a single Insole (ESP8266 class). The thread is responsible for:
 * -Receiving UDP packets from the associated Insole
 * -Processing UDP packets by unwrapping bytes and converting them to understandable values
 * -Storing sensor values in the the associated ESP8266 instance's data holder(s)
 * -Doing sensor-based calculations: Center of mass, Step detection, etc. 
 */
import java.io.Serializable;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketTimeoutException;

import com.mongodb.BasicDBObject;

class UDPReceiver extends Thread implements Serializable{
	private static final long serialVersionUID = 1L;
	ESP8266 esp;
    boolean stopRequested = false;
    DatagramSocket socket = null;
    byte[] buffer = new byte[28];   //incoming buffer
    int count=0; //packet counter
    int sum=0; //summation of FSR readings to calculate average
    long startTime = System.currentTimeMillis();
    BasicDBObject document; //sensor readings to be uploaded to mongoDB (single row of data)
    
    int localPort, serverPort;
    float gyrModifier = (float)(250/Math.pow(2,15));
    float accModifier = (float)(16/Math.pow(2,15)*9.81);

    UDPReceiver(ESP8266 esp, int localPort, int serverPort){
    	this.esp = esp;
        this.localPort = localPort;
        this.serverPort = serverPort;
    }

    @Override
    public void run(){
        System.out.println("[UDPReceiver]"+esp.getName()+": Starting UDP receiver on port "+localPort);
        DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
        try {
            //prepare socket to receive UDP
            socket = new DatagramSocket(localPort);
            socket.setSoTimeout(20000);
            socket.setReuseAddress(true);
            //listen until stopRequested
            while (!stopRequested) {
//====================================================If system halts here: disable Firewall!
                socket.receive(packet);//put the received packet in buffer[]
//====================================================
                unwrap(); //convert buffer to doubles and put them in sensorReadings[]
                DataHandler.mongoUploader.upload(esp);//upload this esp's sensorReadings[]
            }
        }catch(SocketTimeoutException e){
            System.out.println("[UDPReceiver]"+esp.getName()+": UDP Socket Timeout");
        }catch (Exception e){
            e.printStackTrace();
        }finally {
        	try{
	        	//send "STOP" to this.getIP() port serverPort
	            byte[] stopBuffer = "STOP".getBytes();
	            DatagramPacket stopPacket = new DatagramPacket(stopBuffer, stopBuffer.length, InetAddress.getByName(esp.getIP()), serverPort);
	            socket.setSoTimeout(500);
	            while(true){//only exit when there's a SocketTimeoutException
	                socket.send(stopPacket);
	                socket.receive(packet);//timeout if server stopped
	            }
        	}catch(SocketTimeoutException e){
        		//stop successful, do nothing..
        	}
        	catch(Exception e){
        		e.printStackTrace();
        	}
            socket.close();
            System.out.println("[UDPReceiver]"+esp.getName()+": "+count + " datagrams received in " + (System.currentTimeMillis()-startTime) + " ms");
        }
    }

    void stopStream(){
        stopRequested = true;
    }

    private void unwrap(){
    	//Received packet is: 12 bytes for FSR, 4 bytes unused, 12 bytes for IMU
        //Bytes are received as a characters ==> convert to byte first by subtracting 0x30
        //FSR readings are 1 byte each
        //IMU readings are 2 bytes each
        //Convert each byte to int by ANDing with 0xFF
    	
        count++;//count every unwrapped packet
        
        for(int i=0; i<ESP8266.numberOfFSRSensors; i++){
            esp.FSRReadings[i] = (buffer[i]-0x30) & 0xFF; //convert FSR readings from signed char to unsigned int
            sum+=esp.FSRReadings[i];
        }
        esp.averageForce = sum/ESP8266.numberOfFSRSensors; //This produces incorrect results.. needs revision
        //calculateCoM();
        //sum=0;//DO NOT RESET BEFORE calculateCoM()
        //stepMonitor();
        
        esp.IMUReadings[0] = toSignedInt((((buffer[16]-0x30)&0xFF)<<8) | ((buffer[17]-0x30)&0xFF)) *gyrModifier; //GYR_X
        esp.IMUReadings[1] = toSignedInt((((buffer[18]-0x30)&0xFF)<<8) | ((buffer[19]-0x30)&0xFF)) *gyrModifier; //GYR_Y
        esp.IMUReadings[2] = toSignedInt((((buffer[20]-0x30)&0xFF)<<8) | ((buffer[21]-0x30)&0xFF)) *gyrModifier; //GYR_Z
        esp.IMUReadings[3] = toSignedInt((((buffer[22]-0x30)&0xFF)<<8) | ((buffer[23]-0x30)&0xFF)) *accModifier; //ACC_X
        esp.IMUReadings[4] = toSignedInt((((buffer[24]-0x30)&0xFF)<<8) | ((buffer[25]-0x30)&0xFF)) *accModifier; //ACC_Y
        esp.IMUReadings[5] = toSignedInt((((buffer[26]-0x30)&0xFF)<<8) | ((buffer[27]-0x30)&0xFF)) *accModifier; //ACC_Z
        if(esp.getSide()==ESP8266.LEFT){ //modify accelerometer data for LEFT side insole
        	esp.IMUReadings[2]*=-1;
        	esp.IMUReadings[5]*=-1;
        }
        else if(esp.getSide()==ESP8266.RIGHT){ //modify accelerometer data for RIGHT side insole
        	esp.IMUReadings[2]*=-1;
        	esp.IMUReadings[5]*=-1;
        }
    }
    private int toSignedInt(int num){
    	//2^15 = 0x8000 = 32768
    	int snum;
    	if(num<0x8000){
    		snum = num; //num is positive
    	}
    	else{
    		//snum = (num-0x8000)*-1; //1's complement
    		//2's complement:
    		snum = num ^ 0xFFFF;
    		snum = (snum+1)*-1;
    	}
    	return snum;
    }
    
    private void calculateCoM(){
    	//calculate Center of Mass for this insole
    	if(esp.getSide()=='R'){
    		esp.centerOfMass[0] = 0; //X location
    		esp.centerOfMass[1] = 0; //Y location
    		for(int i=0; i<ESP8266.numberOfFSRSensors; i++){
    			esp.centerOfMass[0] += esp.FSRReadings[i]*esp.FSRLocationsRight[i][0];
    			esp.centerOfMass[1] += esp.FSRReadings[i]*esp.FSRLocationsRight[i][1];
    		}
            esp.centerOfMass[0] /= sum;
            esp.centerOfMass[1] /= sum;
    	}
    	else{ //left by default
    		esp.centerOfMass[0] = 0; //X location
    		esp.centerOfMass[1] = 0; //Y location
    		for(int i=0; i<ESP8266.numberOfFSRSensors; i++){
    			esp.centerOfMass[0] += esp.FSRReadings[i]*esp.FSRLocationsLeft[i][0];
    			esp.centerOfMass[1] += esp.FSRReadings[i]*esp.FSRLocationsLeft[i][1];
    		}
            esp.centerOfMass[0] /= sum;
            esp.centerOfMass[1] /= sum;    		
    	}
    }
    
    int curAvg;
    int avgAvg = 0; //average of all averages, used as a threshold to detect a step
    int sumAvg = 0; //summation of all averages
    boolean stepDetected = false;
    int stepCount = 0;  //counter for steps done by the associated insole
    
    private void stepMonitor(){ //Theoretical
    	//Theory: a step is detected when the average of FSR sensors readings goes above a certain threshold
    	//stepThreshold is set at the average of all averages
    	
    	curAvg = esp.averageForce; //current average reading of all FSR sensors
    	sumAvg+=curAvg;
    	avgAvg = sumAvg/count;
    	
    	//Step detection
    	if(curAvg>avgAvg){
    		stepDetected = true;
    	}
    	if(stepDetected && curAvg<avgAvg){ //-10 for to avoid errors
    		stepDetected = false;
    		stepCount++;
    		System.out.println(esp.getName()+" Steps = "+stepCount);
    	}
    }
}