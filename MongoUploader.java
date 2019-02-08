/*
 * MongoUploader class is a thread that does the following:
 * -Maintains connection to the database
 * -Creates JSON files passively (when upload() is called) and adds the file to a shared upload list
 * -Monitors shared upload list and uploads when it reaches a certain threshold  
 */
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

import com.mongodb.BasicDBObject;
import com.mongodb.MongoClient;
import com.mongodb.MongoClientURI;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;

class MongoUploader extends Thread{
	final SimpleDateFormat date = new SimpleDateFormat("yyyy-MMM-dd");
	final SimpleDateFormat time = new SimpleDateFormat("HH:mm:ss.SSS");
    private long uploadCount;
    private ArrayList<BasicDBObject> sharedList;	//list shared between all active producer threads (ESP devices)
    private ArrayList<BasicDBObject> uploadList;	//temporary copy of sharedList to upload. To avoid concurrent modification on one list
    private boolean finished = false;
    private String username, password, address, port, databaseName, collectionName;
    private MongoClientURI uri;
    private MongoClient client;
    boolean local;	//local vs remote database
    DecimalFormat df = new DecimalFormat("00");
    
    MongoUploader(String[] serverAttributes){
    	local = false;
        sharedList = new ArrayList<>();
        this.username = serverAttributes[0];
        this.password = serverAttributes[1];
        this.address = serverAttributes[2];
        this.port = serverAttributes[3];
        this.databaseName = serverAttributes[4];
        this.collectionName = serverAttributes[5];
        uri = new MongoClientURI("mongodb://"+username+":"+password+"@"+address+":"+port+"/"+databaseName);
    }
    
    MongoUploader(){
    	local = true;
    	sharedList = new ArrayList<>();
    	databaseName = "DataHandlerDB";
    	collectionName = "_" + new SimpleDateFormat("yyyyMMdd_kkmmss").format(new Date());
    }
    
    String getCollectionName(){
    	return this.collectionName;
    }
    
    String getDatabaseName(){
    	return this.databaseName;
    }
    
    void upload(ESP8266 esp){
        //prepare DB Object
    	long timeStamp = System.currentTimeMillis();
        BasicDBObject document = new BasicDBObject();
        document.put("DATE", date.format(timeStamp));
        document.put("TIME", time.format(timeStamp));
        document.put("DEV_NM", esp.getName()+"-"+(char)esp.getSide());
        for(int i=0; i<esp.FSRReadings.length; i++){
	        document.put("FSR_"+df.format(i), esp.FSRReadings[i]);
        }
        document.put("GYR_X", esp.IMUReadings[0]);
        document.put("GYR_Y", esp.IMUReadings[1]);
        document.put("GYR_Z", esp.IMUReadings[2]);
        document.put("ACC_X", esp.IMUReadings[3]);
        document.put("ACC_Y", esp.IMUReadings[4]);
        document.put("ACC_Z", esp.IMUReadings[5]);
        document.put("AVG", esp.averageForce);
        document.put("COM_X", esp.centerOfMass[0]);
        document.put("COM_Y",esp.centerOfMass[1]);
        sharedList.add(document);
        uploadCount++;
    }

    @Override
    public void run() {
        if(local){
        	client = new MongoClient("localhost", 27017);
        	System.out.println("[MongoUploader]Starting upload to local DB "+databaseName+" C:"+collectionName);
        }
        else{
        	client = new MongoClient(uri);
        	System.out.println("[MongoUploader]Starting upload to remote DB: "+address+":"+port+" DB:"+databaseName+" C:"+collectionName);
        }
        try{
	        MongoDatabase db = client.getDatabase(databaseName);
	        MongoCollection<BasicDBObject> collection = db.getCollection(collectionName, BasicDBObject.class);
	        while(!finished){
	        	try {
	        		if(sharedList.size()>=1000){
	                	System.out.println("[MongoUploader]Uploading...");
	                    uploadList = new ArrayList<>(sharedList); //to avoid ConcurrentModificationException
	                    sharedList.clear();
	                    collection.insertMany(uploadList);
	                    uploadList = new ArrayList<>();
	                }
					Thread.sleep(100);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
	        }
	        System.out.println("[MongoUploader]Finishing upload...");
	        if(!sharedList.isEmpty()){ //upload remaining documents
	            uploadList = new ArrayList<>(sharedList); //to avoid ConcurrentModificationException
	            sharedList.clear();
	            collection.insertMany(uploadList);
	        }
	        client.close();
	        System.out.println("[MongoUploader]Total documents uploaded " + uploadCount);
	        if(DataHandler.autoExportEnabled){
	        	System.out.println("Exporting results to CSV...");
	    		DataHandler.exportResults();
	        }
        }
        catch(Exception e){
        	System.out.println("ERROR: Cannot connect to MongoDB");
        	e.printStackTrace();
        }
    }

    public void finish(){
        finished = true;
    }
}
