/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package edu.stevens.okta;

import com.pointbluetech.oktadriver.json.*;

/**
 *
 * @author jcombs
 */
public class OktaAttribute
{

    public String name;
    public boolean required;
    public String type; //String,Int, String Array
   

    public void setType(JSONObject oktaProperty)
    {
        String oktaType = oktaProperty.getString("type");
        if (oktaType.equals("string"))
        {
            type = "string";
        }

        if (oktaType.equals("number"))
        {
            type = "number";
        }

        if (oktaType.equals("array"))
        {
            if (oktaProperty.getJSONObject("items").getString("type").equals("number"))
            {
                type = "numberArray";

            }
            else
            {
                type = "stringArray";

            }
        }

    }

}
