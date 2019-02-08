import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

class HTTPRequest extends Thread{ //Parameters (params), Progress, Result(doInBackground return)
	String response;
    public HTTPRequest(String urlString){
    	System.out.println("[HTTPRequest]Executing "+urlString);
        response = "";
        HttpURLConnection connection = null;
        try{
            //Create connection
            URL url = new URL(urlString);
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
            connection.setRequestProperty("Content-Language", "en-US");
            connection.setUseCaches(false);
            connection.setDoOutput(true);

            //Send request
            DataOutputStream wr = new DataOutputStream(connection.getOutputStream());
            wr.close();

            //Get Response
            if(connection.getResponseCode() == 200) {
                InputStream is = connection.getInputStream();
                BufferedReader rd = new BufferedReader(new InputStreamReader(is));
                String line;
                while((line = rd.readLine()) != null) {
                    response+=(line+"\n");
                }
                rd.close();
            }
            else{
                response = "HTTP Error "+ connection.getResponseCode();
            }
        } catch(Exception e){
            response = "ERROR: Unreachable";
            response+= "\n\n"+e.toString();
        } finally{
            if (connection != null){
                connection.disconnect();
            }
        }
    }
    
    public String getResponse(long timeout){
    	long startTime = System.currentTimeMillis();
    	while(response==null && System.currentTimeMillis()-startTime<timeout){} //wait
    	if(response == null){
    		return "No response";
    	}
    	return response;
    }
}