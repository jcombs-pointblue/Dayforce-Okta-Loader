/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Main.java to edit this template
 */
package edu.stevens.okta;

import com.pointbluetech.oktadriver.json.JSONObject;
import java.util.HashMap;
import java.util.Properties;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.ConcurrentHashMap;

/**
 *
 * @author jcombs
 */
public class StevensStudentSync {

    //read config
    //read schema map
    //get Okta schema
    //load CSV
    //validate schema
    //Keep DB of user state. Loss of DB requires full resynch. do a full resync periodically on a rate limited thread
    //option to do a full sync on demand
    //auto adjust for API rate limiting
    //run multiple threads for speed
    //
    //public static OktaSchema oktaSchema;
    //public static HashMap<String, String> attributeMap = new HashMap(); //read from file <file header>:<okta attr key>
    public static Properties props = new Properties();
    public static ConcurrentHashMap<String, JSONObject> currentOktaUsers = new ConcurrentHashMap(100000);
    public static Properties attrProps = new Properties();

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) throws Exception {

        try ( InputStream input = new FileInputStream("studentSynch.properties")) 
        {

            // load a properties file
            props.load(input);

            // get the property value and print it out
            System.out.println(props.getProperty("oktaURL"));
            System.out.println(props.getProperty("token"));
            System.out.println(props.getProperty("inputFilePath"));
            System.out.println(props.getProperty("logPath"));
            System.out.println(props.getProperty("threads"));

        } catch (IOException ex) 
        {
            ex.printStackTrace();
            System.exit(-1);
        }


        try ( InputStream input = new FileInputStream("attributeMap.properties")) 
        {

            // load a properties file
            attrProps.load(input);
            
            attrProps.list(System.out);
            System.out.println();

           

        } catch (IOException ex) 
        {
            ex.printStackTrace();
            System.exit(-1);
        }
        int threadCount = Integer.parseInt(props.getProperty("threads"));

        //JSONObject oktaSchemaJSON = new JSONObject(); //This will be retrieved from web svc

        //oktaSchema = new OktaSchema(oktaSchemaJSON);
        
        
        
        Logger log = new Logger(Logger.DEBUG,"student.log");
        
        readCurrentOktaData(log, props);
        
        System.out.println("Current user Array size: "+ currentOktaUsers.size());

        // create a pool of threads,  will execute in parallel
        ExecutorService threadPool = Executors.newFixedThreadPool(threadCount);
        // submit jobs to be executing by the pool
        for (int i = 0; i < threadCount; i++) 
        {
            threadPool.submit(new FileReader(props.getProperty("inputFilePath"), i+1, threadCount, 0l, log));
        }
        // once you've submitted your last job to the service it should be shut down
        threadPool.shutdown();
        // wait for the threads to finish if necessary
        threadPool.awaitTermination(Long.MAX_VALUE, TimeUnit.MILLISECONDS);

//        Thread t1 = new Thread(new FileReader(props.getProperty("inputFilePath"), 2, 3, 0l, new Trace()));
//        t1.start();

    }
    
    
    private static void readCurrentOktaData(Logger log, Properties props) throws IOException
    {
       //get all users from Okta /api/v1/users defaults to 200 per page
        //user okta-reponse header with omitCredentials,omitCredentialsLinks,omitTransitioningToStatus
        //put in hashmap
        
        Client myClient = new Client(log,props);
        
        myClient.loadUsers();
        
        
    }

  

}
