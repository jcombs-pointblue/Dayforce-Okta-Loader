/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.pointbluetech.okta.csv;

import com.pointbluetech.oktadriver.json.JSONObject;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.LinkedHashMap;
import java.util.Map;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;

/**
 * Reads a CSV file and pushes adds/modifies into Okta.
 *
 * <p>The CSV must contain a column whose name matches {@code matchKey}
 * (csvSync.properties, default {@code universalID}); that column joins each
 * row to the corresponding Okta user.
 *
 * <p>All other CSV→Okta field mappings are driven by
 * {@code attributeMap.properties} (CSV-header-name = Okta-attribute-name).
 * Per-field date format conversion may be configured in csvSync.properties as
 * {@code dateField.<csvHeader>.fromFormat} / {@code .toFormat} using
 * {@link SimpleDateFormat} patterns.
 */
public class FileReader implements Runnable {

    public static final String UTF8_BOM = "﻿";

    public String filePath;
    public int threadNumber;
    public int threadCount;
    public long backoffMs;
    public Logger log;

    private final String matchKey;
    private final String loginSuffix;

    public FileReader(String filePath, int threadNumber, int threadCount, long backoffMs, Logger log) {
        this.filePath = filePath;
        this.threadNumber = threadNumber;
        this.threadCount = threadCount;
        this.backoffMs = backoffMs;
        this.log = log;
        this.matchKey = CsvSync.props.getProperty("matchKey", "universalID");
        this.loginSuffix = CsvSync.props.getProperty("loginSuffix", "");
    }

    public void run() {

        System.out.println("FR CU Array size: " + CsvSync.currentOktaUsers.size());

        try (Reader reader = openCsvReader(filePath);
             CSVParser parser = CSVFormat.DEFAULT.builder()
                     .setHeader()
                     .setSkipHeaderRecord(true)
                     .setIgnoreEmptyLines(true)
                     .setIgnoreSurroundingSpaces(true)
                     .setTrim(true)
                     .get()
                     .parse(reader))
        {
            System.out.println("Header line: " + parser.getHeaderNames());

            int nextLine = threadNumber;
            int lineNumber = 0;
            for (CSVRecord record : parser)
            {
                lineNumber++;
                if (lineNumber != nextLine)
                {
                    continue;
                }
                nextLine = lineNumber + threadCount;

                Map<String, String> recordMap = toRecordMap(parser, record);

                String matchValue = recordMap.get(matchKey);
                if (matchValue == null || matchValue.isEmpty())
                {
                    log.Log(Logger.NORMAL, "Skipping row " + lineNumber + ": missing " + matchKey);
                    continue;
                }

                if (CsvSync.currentOktaUsers.containsKey(matchValue))
                {
                    log.Log(Logger.DEBUG, "Found user: " + matchValue);
                    String oktaID = CsvSync.currentOktaUsers.get(matchValue).getString("id");

                    JSONObject modObj = getUpdateObj(recordMap);
                    if (modObj.getJSONObject("profile").isEmpty())
                    {
                        //nothing to update
                        continue;
                    }

                    Client myClient = new Client(log, CsvSync.props);
                    String url = CsvSync.props.getProperty("oktaURL") + "/api/v1/users/" + oktaID;
                    try
                    {
                        myClient.post(url, modObj);
                        log.Log(Logger.NORMAL, "User modified: " + matchValue);
                    } catch (Exception ex)
                    {
                        ex.printStackTrace();
                        log.Log(Logger.NORMAL, "User modify failed: " + matchValue + " : " + ex.getMessage());
                    }
                } else
                {
                    System.out.println("Did not find user: " + matchValue);

                    JSONObject addObject = getCreateObj(recordMap);
                    System.out.println(addObject.toString(2));

                    Client myClient = new Client(log, CsvSync.props);
                    String url = CsvSync.props.getProperty("oktaURL") + "/api/v1/users?activate=false";
                    try
                    {
                        myClient.post(url, addObject);
                        log.Log(Logger.NORMAL, "User created: " + matchValue);
                    } catch (Exception ex)
                    {
                        ex.printStackTrace();
                        log.Log(Logger.NORMAL, "User creation failed: " + matchValue);
                    }
                }
            }
        } catch (Exception ex)
        {
            ex.printStackTrace();
        }
    }

    /**
     * Opens the CSV file as UTF-8 and discards a leading BOM if present so it
     * doesn't bleed into the first header name.
     */
    private static Reader openCsvReader(String path) throws java.io.IOException {
        BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(path), StandardCharsets.UTF_8));
        br.mark(1);
        int first = br.read();
        if (first != 0xFEFF)
        {
            br.reset();
        }
        return br;
    }

    /**
     * Materializes a record into a name→value map, using header names that
     * have been trimmed and BOM-stripped. Missing trailing fields become "".
     */
    private static Map<String, String> toRecordMap(CSVParser parser, CSVRecord record) {
        Map<String, String> map = new LinkedHashMap<>();
        for (String header : parser.getHeaderNames())
        {
            String key = header == null ? "" : header.replace(UTF8_BOM, "").trim();
            String value = record.isMapped(header) && record.isSet(header) ? record.get(header) : "";
            map.put(key, value);
        }
        return map;
    }

    private JSONObject getCreateObj(Map<String, String> recordMap) {
        JSONObject profile = new JSONObject();

        // Apply every CSV→Okta mapping declared in attributeMap.properties.
        for (String csvKey : recordMap.keySet())
        {
            String oktaKey = CsvSync.attrProps.getProperty(csvKey);
            if (oktaKey == null)
            {
                continue;
            }
            profile.put(oktaKey, transformValue(csvKey, recordMap.get(csvKey)));
        }

        // matchKey and login are computed, not mapped via attrProps.
        String matchValue = recordMap.get(matchKey);
        profile.put(matchKey, matchValue);
        profile.put("login", matchValue + loginSuffix);

        JSONObject addObject = new JSONObject();
        addObject.put("profile", profile);
        return addObject;
    }

    private JSONObject getUpdateObj(Map<String, String> recordMap) {
        JSONObject oktaUser = CsvSync.currentOktaUsers.get(recordMap.get(matchKey));
        JSONObject oktaProfile = oktaUser.getJSONObject("profile");
        JSONObject profile = new JSONObject();

        System.out.println("Okta profile: " + oktaProfile.toString());

        for (String csvKey : recordMap.keySet())
        {
            String oktaKey = CsvSync.attrProps.getProperty(csvKey);
            if (oktaKey == null)
            {
                continue;
            }
            String newValue = transformValue(csvKey, recordMap.get(csvKey));
            String currentValue = oktaProfile.optString(oktaKey, "");
            if (!currentValue.equals(newValue))
            {
                profile.put(oktaKey, newValue);
            }
        }

        System.out.println("update profile: " + profile);

        JSONObject modObject = new JSONObject();
        modObject.put("profile", profile);
        return modObject;
    }

    /**
     * Applies a per-field date reformat if {@code dateField.<csvKey>.fromFormat}
     * and {@code .toFormat} are configured. Empty / unconfigured values pass through
     * unchanged. Unparseable dates are logged and passed through as-is.
     */
    private String transformValue(String csvKey, String value) {
        if (value == null || value.isEmpty())
        {
            return value == null ? "" : value;
        }
        String fromFormat = CsvSync.props.getProperty("dateField." + csvKey + ".fromFormat");
        String toFormat = CsvSync.props.getProperty("dateField." + csvKey + ".toFormat");
        if (fromFormat == null || toFormat == null)
        {
            return value;
        }
        try
        {
            SimpleDateFormat in = new SimpleDateFormat(fromFormat);
            in.setLenient(false);
            SimpleDateFormat out = new SimpleDateFormat(toFormat);
            return out.format(in.parse(value));
        } catch (ParseException pe)
        {
            log.Log(Logger.NORMAL, "Date parse failed for " + csvKey + "='" + value + "' (" + fromFormat + "): " + pe.getMessage());
            return value;
        }
    }

    private void deactivateUser(String oktaID) throws Exception {
        Client myClient = new Client(log, CsvSync.props);
        String url = CsvSync.props.getProperty("oktaURL") + "/api/v1/users/" + oktaID + "/lifecycle/deactivate";
        myClient.post(url, new JSONObject());
    }
}
