/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.pointbluetech.okta.csv;

import com.pointbluetech.oktadriver.json.JSONObject;
import com.pointbluetech.oktadriver.json.JSONArray;
//import com.pointbluetech.nds.dirxml.driver.okta.OktaAttribute;


/**
 *
 * @author jcombs
 */
public class UserRequest extends JSONObject
{

    JSONObject profile = new JSONObject();

    /**
     *
     */
    public UserRequest()
    {

        this.put("profile", profile);

    }

    /**
     *
     * @param oktatUser
     */
    public UserRequest(JSONObject oktatUser)
    {

        this.put("profile", oktatUser.getJSONObject("profile"));

    }

    /**
     *
     * @param name
     * @param value
     */
    public void removeArrayValue(String name, Object value)
    {
        JSONArray theValues = this.getJSONObject("profile").optJSONArray(name);
        //there may not be such an array
        if (theValues == null)
        {
            return;
        }
        for (int i = 0; i < theValues.length(); i++)
        {
            if (theValues.get(i).equals(value))
            {
                theValues.remove(i);
            }
        }

        profile.put(name, theValues);
    }

    /**
     *
     * @param oktaAttr
     * @param value
     */
    public void addValue(OktaAttribute oktaAttr, String value)
    {
        System.out.println("Adding value: " + value + " to attr: " + oktaAttr.name);
        if (oktaAttr.type.equals("number"))
        {
            setNumericAttributeValue(oktaAttr.name, value);
        }
        if (oktaAttr.type.equals("string"))
        {
            setStringAttributeValue(oktaAttr.name, value);
        }
        if (oktaAttr.type.equals("numberArray"))
        {
            addNumericArrayAttributeValue(oktaAttr.name, value);
        }
        if (oktaAttr.type.equals("stringArray"))
        {
            addStringArrayAttributeValue(oktaAttr.name, value);
        }
    }

    /**
     *
     * @param oktaAttr
     * @param value
     */
    public void removeValue(OktaAttribute oktaAttr, String value)
    {
        if (oktaAttr.type.equals("number") || oktaAttr.type.equals("string"))
        {
            removeAllValues(oktaAttr.name);
            //setNumericAttributeValue(oktaAttr.name, value); //not sure why this was there
        }

        if (oktaAttr.type.equals("numberArray") || oktaAttr.type.equals("stringArray"))
        {
            removeArrayValue(oktaAttr.name, value);
        }

    }

    /**
     *
     * @param name
     * @param value
     */
    public void addStringArrayAttributeValue(String name, String value)
    {
                System.out.println("Adding value2: " + value + " to attr: " + name);

        this.getJSONObject("profile").append(name, value);

    }

    /**
     *
     * @param name
     * @param value
     */
    public void addNumericArrayAttributeValue(String name, String value)
    {
        this.getJSONObject("profile").append(name, Integer.parseInt(value));

    }

    /**
     *
     * @param name
     * @param value
     */
    public void setStringAttributeValue(String name, String value)
    {
        //System.out.println("Adding value2: " + value + " to attr: " + name);

        //profile.put(name, value);
        this.getJSONObject("profile").put(name, value);
        System.out.println("Profile: "+ this.getJSONObject("profile"));


    }

    /**
     *
     * @param attrName
     */
    public void removeAllValues(String attrName)
    {
        this.getJSONObject("profile").remove(attrName);

    }

    /**
     *
     * @param name
     * @param value
     */
    public void setNumericAttributeValue(String name, String value)
    {
        this.getJSONObject("profile").put(name, Integer.parseInt(value));

    }

    /**
     *
     * @param pwValue
     */
    public void addPassword(String pwValue)
    {
        JSONObject credentials = new JSONObject();
        JSONObject password = new JSONObject();
        password.put("value", pwValue);
        credentials.put("password", password);
        this.put("credentials", credentials);
    }

//    "credentials": {
//    "password" : { "value": "{{password}}" }
//  }
}
