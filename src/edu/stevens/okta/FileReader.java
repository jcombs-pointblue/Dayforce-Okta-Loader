/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package edu.stevens.okta;

import java.io.BufferedReader;
import java.lang.Readable;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.List;
import com.pointbluetech.oktadriver.json.JSONObject;
import com.pointbluetech.oktadriver.json.JSONArray;
import java.util.Properties;

import java.lang.RuntimeException;
import java.util.HashMap;
import java.util.Iterator;

/**
 *
 * @author jcombs
 *
 * Reads CSV and creates update table records
 *
 * File must have all the fields in the initial implementation WITH the same
 * names. Additional fields may be added but must be named using their Okta
 * schema name. Only strings are supported!
 *
 * Header line:
 * universalID,firstName,middleName,lastName,displayName,email,homeEmail,academicLevel,classStanding,affiliation,isAlum,isFormerStudent,graduationDate
 */
public class FileReader implements Runnable {

    /**
     *
     */
    public String filePath;

    /**
     *
     */
    public int threadNumber;

    /**
     *
     */
    public int threadCount;

    /**
     *
     */
    public long backoffMs;

    /**
     *
     */
    public Logger log;
    private ArrayList<String> fileHeaderArray = new ArrayList();

    /**
     *
     * @param filePath
     * @param threadNumber
     * @param threadCount
     * @param backoffMs
     * @param log
     */
    public FileReader(String filePath, int threadNumber, int threadCount, long backoffMs, Logger log) {
        this.filePath = filePath;
        this.threadNumber = threadNumber;
        this.threadCount = threadCount;
        this.backoffMs = backoffMs;
        this.log = log;

    }

    /**
     *
     */
    public void run() {

        System.out.println("FR CU Array size: " + StevensStudentSync.currentOktaUsers.size());

        try (BufferedReader br = new BufferedReader(new java.io.FileReader(filePath))) {
            int nextLine = threadNumber;
            String line;
            int lineNumber = 0;
            while ((line = br.readLine()) != null) {
                // System.out.println(line);
                if (line.length() == 0) {
                    continue;

                }

                if (lineNumber == 0) {
                    //Load file schema
                    String[] headerArray = line.split(",");
                    System.out.println("HeaderArray Size: " + headerArray.length);
                    System.out.println("Header line: " + line);

                    List headerList = Arrays.asList(headerArray);

                    //TODO: need to trim values
                    fileHeaderArray.addAll(headerList);
                    lineNumber++;
                    continue;
                }

                //System.out.println("lineNumer: " + lineNumber);
                //System.out.println("nextLine: " + nextLine);
                //process each line based on threading
                if (lineNumber == nextLine) {
                    //System.out.println("LineNumber: " + lineNumber + " " + line);
                    //System.out.println("LineNumber: "+ lineNumber);
                    String[] record = line.split(",");

                    //System.out.println("record length: " + record.length);
                    //check to see if record length matches heaer length
                    if (fileHeaderArray.size() > record.length + 1) {
                        //throw exception or log it?
                        throw new RuntimeException("INVALID RECORD: LINE: " + lineNumber);
                    }

                    ArrayList<String> recordArrayList = new ArrayList(Arrays.asList(record));
                    //System.out.println("recordArrayList length: " + recordArrayList.size());

                    if (fileHeaderArray.size() == record.length + 1) {
                        recordArrayList.add("");
                    }
                    //System.out.println("recordArrayList length2: " + recordArrayList.size());

                    HashMap<String, String> recordMap = new HashMap();

                    for (int i = 0; i < recordArrayList.size(); i++) {
                        //if (record[i].length() > 0) {
                        //System.out.println("key: " + fileHeaderArray.get(i) + " value: " + recordArrayList.get(i).replaceAll("\"", ""));
                        //String keyString = fileHeaderArray.get(i).trim();
                        //System.out.println(";" + keyString + ":");
                        //char space = keyString.toCharArray()[0];
                        //int code = (int) space;
                        //System.out.println("char code: " + code);
                        recordMap.put(removeUTF8BOM(fileHeaderArray.get(i)), recordArrayList.get(i).replaceAll("\"", ""));
                        //  }
                        //System.out.println(Arrays.toString(recordMap.keySet().toArray()));
//                        System.out.println(Arrays.toString(recordMap.entrySet().toArray()));
//                        System.out.println("universalID from rm1: " + recordMap.get(fileHeaderArray.get(i)));

                    }

                    //System.out.println(recordMap.size());
                    //System.out.println("universalID from rm2: " + recordMap.get(fileHeaderArray.get(0)));
                    //System.out.println(fileHeaderArray.get(0).equals("universalID"));
                    //System.out.println(":"+fileHeaderArray.get(0)+":");
                    //System.out.println(fileHeaderArray.get(0));
                    //get user from map by universalID
                    if (StevensStudentSync.currentOktaUsers.containsKey(recordMap.get("universalID"))) {
                        log.Log(Logger.DEBUG, "Found user: " + recordMap.get("universalID"));
                        String oktaID = StevensStudentSync.currentOktaUsers.get(recordMap.get("universalID")).getString("id");

                        //process modify
                        //determine if modify needed
                        JSONObject modObj = getUpdateObj(recordMap);
                        if (modObj.getJSONObject("profile").isEmpty()) {
                            //nothing to update
                        } else {
                            //modify it
                            Client myClient = new Client(log, StevensStudentSync.props);
                            // String oktaID = StevensStudentSync.currentOktaUsers.get(recordMap.get("universalID")).getString("id");

                            String url = StevensStudentSync.props.getProperty("oktaURL") + "/api/v1/users/" + oktaID;

                            //System.out.println(url);
                            try {
                                myClient.post(url, modObj);
                                log.Log(Logger.NORMAL, "User modified: " + recordMap.get("universalID"));

                            } catch (Exception ex) {
                                ex.printStackTrace();
                                log.Log(Logger.NORMAL, "User modify failed: " + recordMap.get("universalID") + " : " + ex.getMessage());

                            }

                        }

                        //Deactivate former students
//                        String status = StevensStudentSync.currentOktaUsers.get(recordMap.get("universalID")).getString("status");
//
//                        if (status.equals("ACTIVE") || status.equals("STAGED")) {
//
//                            try {
//                                deactivateUser(oktaID);
//                            } catch (Exception ex) {
//                                ex.printStackTrace();
//                                log.Log(Logger.NORMAL, "User deactivation failed: " + oktaID);
//                            }
//                        }

                        //log it
                    } else {
                        System.out.println("Did not find user: " + recordMap.get("universalID"));
                        //process add
                        //determine if add needed
                        // if (recordMap.get("email").equalsIgnoreCase("stevensacctmgt@gmail.com")) {

                        String affiliation = recordMap.get("affiliation");
                        //Only create student records. Alum and former-student not created.
                        if (affiliation.equals("student")) {

                            //build JSON
                            JSONObject profile = new JSONObject();
                            //map from record names to Okta names
                            //universalID is synced to universalID and to arrays sitid and studentid
                            //okta cn is constructed from appuser.firstName + " " + appuser.lastName + " - " + String.substringBefore(appuser.email, "@")

//universalID:universalID
//firstName:firstName
//middleName:initials
//lastName:lastName
//displayName:displayName
//email:email
//homeEmail:secondEmail
//academicLevel:academicLevel
//classStanding:classStanding
//eduPersonAffiliation:eduPersonAffiliation
//isAlum:isAlum
//isFormerStudent:isFormerStudent
//graduationDate:none
                            profile.put("email", recordMap.get("email"));
                            profile.put("firstName", recordMap.get("firstName"));
                            profile.put("lastName", recordMap.get("lastName"));
                            profile.put("secondEmail", recordMap.get("homeEmail"));
                            profile.put("middleName", recordMap.get("middleName"));
                            profile.put("academicLevel", recordMap.get("academicLevel"));
                            profile.put("classStanding", recordMap.get("classStanding"));
                            //profile.put("eduPersonAffiliation", recordMap.get("eduPersonAffiliation"));
                            profile.put("strEduPersonAffiliation", recordMap.get("affiliation"));

                            profile.put("login", recordMap.get("universalID") + "@stevens.edu");
                            //translate to boolean?

                            //no longer used
                            // profile.put("isAlum", recordMap.get("isAlum"));
                            // profile.put("isFormerStudent", recordMap.get("isFormerStudent"));
                            profile.put("universalID", recordMap.get("universalID"));

                            JSONArray sitid = new JSONArray();
                            sitid.put(recordMap.get("universalID"));
                            profile.put("sitid", sitid);

                            JSONArray studentid = new JSONArray();
                            studentid.put(recordMap.get("universalID"));
                            profile.put("studentid", studentid);

                            JSONObject addObject = new JSONObject();
                            addObject.put("profile", profile);

                            System.out.println(addObject.toString(2));

                            Client myClient = new Client(log, StevensStudentSync.props);

                            String url = StevensStudentSync.props.getProperty("oktaURL") + "/api/v1/users?activate=false";

                            //System.out.println(url);
                            try {
                                myClient.post(url, addObject);
                                log.Log(Logger.NORMAL, "User created: " + recordMap.get("universalID"));

                            } catch (Exception ex) {
                                ex.printStackTrace();
                                log.Log(Logger.NORMAL, "User creation failed: " + recordMap.get("universalID"));

                            }

                            ///api/v1/users?activate=false  may need to use main tenant URL
                        } else {
                            //log user was not found but did not have the new user email
                            log.Log(Logger.NORMAL, "User was not found but record does not have initial email: " + recordMap.get("universalID"));

                        }

                        //add
                        //log it
                    }

                    nextLine = lineNumber + threadCount;

                }

                lineNumber++;

            }

        } catch (Exception ex) {

            //need to increment line numebr for exceptions in the loop
            ex.printStackTrace();
        }

    }

    private void deactivateUser(String oktaID) throws Exception {
        Client myClient = new Client(log, StevensStudentSync.props);
        String url = StevensStudentSync.props.getProperty("oktaURL") + "/api/v1/users/" + oktaID + "/lifecycle/deactivate";

        myClient.post(url, new JSONObject());

    }

    private JSONObject getUser() {
        JSONObject user = new JSONObject();

        //make WS call
        return user;
    }

    private JSONObject getUpdateObj(HashMap<String, String> recordMap) {
        //StevensStudentSync.currentOktaUsers.containsKey(recordMap.get("universalID"))
        JSONObject oktaUser = StevensStudentSync.currentOktaUsers.get(recordMap.get("universalID"));

        //CWID won't change
        // affiliation becomes formerStudent then set isFormerStudentDate, if set back to STUDENT then delete isFormerStudentDate
        // if date is one year past then deactivate account
        //if graduationDate is 6 months past and affiliation then deactivate
        JSONObject profile = new JSONObject();

        String firstName = oktaUser.getJSONObject("profile").getString("firstName");

        System.out.println("Okta profile: " + oktaUser.getJSONObject("profile").toString());

        //System.out.println("rm: " + recordMap.get("firstName"));
        //fileHeaderArray contains all file field names
        //recordMap has all file fields and names as keys
        //StevensStudentSync.attrProp  has the mapping for non array okta fields. Arrays are only handled as special cases since CWID does not change and all current arrays are cwid
        Iterator<String> recKeysIter = recordMap.keySet().iterator();

        while (recKeysIter.hasNext()) {
            String key = recKeysIter.next();
            if (StevensStudentSync.attrProps.get(key) != null) //mapping exists
            {
                if (key.equals("graduationDate")) {
                    String currentGDValue = oktaUser.getJSONObject("profile").optString(StevensStudentSync.attrProps.getProperty(key), "");
                    String newGDValue = recordMap.get(key);
                    String txformGDValue = "";
                    //transform YYYY-MM-DD to MM-DD-YYYY
                    if (!newGDValue.equals("")) {
                        String[] dateArray = newGDValue.split("-");
                        txformGDValue = dateArray[1] + "-" + dateArray[2] + "-" + dateArray[0];
                    }

                    if (!currentGDValue.equals(txformGDValue)) {
                        profile.put((String) StevensStudentSync.attrProps.get(key), txformGDValue);
                    }

                } else {

                    if (!oktaUser.getJSONObject("profile").optString(StevensStudentSync.attrProps.getProperty(key), "").equals(recordMap.get(key))) {
                        profile.put((String) StevensStudentSync.attrProps.get(key), recordMap.get(key));

                    }
                }

            }
        }

        System.out.println("update profile: " + profile.toString());

        JSONObject modObject = new JSONObject();
        modObject.put("profile", profile);

        return modObject;
    }

    /**
     *
     */
    public static final String UTF8_BOM = "\uFEFF";

    private static String removeUTF8BOM(String s) {
        if (s.startsWith(UTF8_BOM)) {
            s = s.substring(1);
        }
        return s;
    }
}
