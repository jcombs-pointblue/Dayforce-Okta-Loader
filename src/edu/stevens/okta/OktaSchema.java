package edu.stevens.okta;

//import com.novell.nds.dirxml.driver.XmlDocument;
//import com.novell.nds.dirxml.driver.util.DSAttribute;
//import com.novell.nds.dirxml.driver.util.DSEntry;
//import com.novell.nds.dirxml.driver.util.DSValue;

//import com.novell.nds.dirxml.driver.util.DSValueOperation;
import com.pointbluetech.oktadriver.json.JSONObject;
import java.util.Iterator;
import org.w3c.dom.Element;
//import com.novell.xml.dom.*;
import java.util.Vector;
//import org.w3c.dom.*;
import java.util.HashMap;
import java.util.Random;

/**
 *
 * @author jcombs
 */
public class OktaSchema
{
    
    //get the schema from Okta
    //load the schema map file
    //if any Okta attributes are unmapped, error and log, if map has less columns than file then error
    //Okta attrs that are not used should be "unused" in the map.
    //map  <OKTA ATTR>|<FILE HEADER>|<ATTR TYPE, STRING,DATE,INT>  MUST BE IN FILE column ORDER

    public JSONObject schemaJSON;
    public HashMap<String, OktaAttribute> oktaAttributes = new HashMap();
    private String passFlow;

    public OktaSchema(JSONObject schemaJSON)
    {
        this.schemaJSON = schemaJSON;
        loadSchema();
    }
    
    public boolean isOktaSchemaValid()
    {
        return oktaAttributes.size()>3;
    }

//    public JSONObject buildCreateJSON(Element event)
//    {
//        //"foo":"bar"
//        //"doh": 123445
//        //"obj": {"some":"thing"}
//        //"array"["qqq","bbb"]
//
//        UserRequest output = new UserRequest();
//
//        DSEntry entry = new DSEntry(event);
//        tracer.trace("Created DSEntry", 3);
//
//        Vector attrVector = entry.getAttributes();
//        Iterator attrIter = attrVector.iterator();
//
//        while (attrIter.hasNext())
//        {
//            DSAttribute theAttr = (DSAttribute) attrIter.next();
//            OktaAttribute oktaAttr = oktaAttributes.get(theAttr.getAttributeName());
//            if (oktaAttr == null)
//            {
//                tracer.trace("attribute not found in Okta schema: " + theAttr.getAttributeName(), 3);
//                continue;
//            }
//
//            if (oktaAttr.type.equals("numericArray") || oktaAttr.type.equals("stringArray"))
//            {
//                Iterator valueIter = theAttr.getValues().iterator();
//
//                //This WILL allow you to put multiple values in a single valued attribute
//                while (valueIter.hasNext())
//                {
//                    String theValue = ((DSValue) valueIter.next()).getValue();
//
//                    if (oktaAttr.type.equals("stringArray"))
//                    {
//                        output.addStringArrayAttributeValue(theAttr.getAttributeName(), theValue);
//                    }
//                    if (oktaAttr.type.equals("numericArray"))
//                    {
//                        output.addNumericArrayAttributeValue(theAttr.getAttributeName(), theValue);
//                    }
//                }
//            }
//            else
//            {
//                String theValue = theAttr.getLastValue().getValue();
//
//                if (oktaAttr.type.equals("string"))
//                {
//                    output.setStringAttributeValue(theAttr.getAttributeName(), theValue);
//                }
//                if (oktaAttr.type.equals("numeric"))
//                {
//                    output.setNumericAttributeValue(theAttr.getAttributeName(), theValue);
//                }
//            }
//
//        }
//
//        if (passFlow.equals("normal"))
//        {
//            //handle password
//            Iterator passwordIter = attrVector.iterator();
//            while (passwordIter.hasNext())
//            {
//                DSAttribute theAttr = (DSAttribute) passwordIter.next();
//                if (theAttr.getAttributeName().equals("password"))
//                {
//                    String pwValue = theAttr.getLastValue().getValue();
//                    if (pwValue.equalsIgnoreCase("random"))
//                    {
//                        pwValue = getPWString();
//                    }
//                    output.addPassword(pwValue);
//                }
//            }
//        }
//
//        if (passFlow.equals("inital") || passFlow.equals("random"))
//        {
//            //set random password                
//
//            output.addPassword(getPWString());
//
//        }
//
//        System.out.println(">>>> " + output);
//
//        return output;
//
//    }

//    public JSONObject buildModifyJSON(Element event, JSONObject oktaUser)
//    {
//        //"foo":"bar"
//        //"doh": 123445
//        //"obj": {"some":"thing"}
//        //"array"["qqq","bbb"]
//
//        UserRequest output = new UserRequest(oktaUser);
//
//        DSEntry entry = new DSEntry(event);
//        tracer.trace("Created DSEntry", 3);
//
//        Vector attrVector = entry.getAttributes();
//        Iterator attrIter = attrVector.iterator();
//
//        while (attrIter.hasNext())
//        {
//            DSAttribute theAttr = (DSAttribute) attrIter.next();
//
//            Vector theOps = theAttr.getValueOperations();
//            Iterator theOpsIterator = theOps.iterator();
//            while (theOpsIterator.hasNext())
//            {
//                DSValueOperation theOp = (DSValueOperation) theOpsIterator.next();
//                System.out.println("op value: " + theOp.getOperationType());
//
//            }
//
//            OktaAttribute oktaAttr = oktaAttributes.get(theAttr.getAttributeName());
//            if (oktaAttr == null)
//            {
//                tracer.trace("attribute not found in Okta schema: " + theAttr.getAttributeName(), 3);
//                continue;
//            }
//            tracer.trace("attribute found in Okta schema: " + theAttr.getAttributeName(), 3);
//
//            // tracer.trace("processing single valued attr", 3);
////                if (((DSValueOperation) theAttr.getValueOperations().lastElement()).getOperationType() == DSValueOperation.REMOVE_ALL_VALUES)
////                {
////                    //this should only occur if there is ONLY a remove-all
////                    output.removeAllValues(theAttr.getAttributeName());
////                }
//            //DSValue dsValue = theAttr.getLastValue();
//            Vector opVector = theAttr.getValueOperations();
//            Iterator opIter = opVector.iterator();
//            while (opIter.hasNext())
//            {
//                DSValueOperation theOp = (DSValueOperation) opIter.next();
//
//                //This should always be the first op
//                if (theOp.getOperationType() == DSValueOperation.REMOVE_ALL_VALUES)
//                {
//                    output.removeAllValues(theAttr.getAttributeName());
//                    continue;
//                }
//                if (theOp.getOperationType() == DSValueOperation.REMOVE_VALUE)
//                {
//                    Vector values = theOp.getValues();
//                    Iterator valIter = values.iterator();
//                    while (valIter.hasNext())
//                    {
//                        //output.addValue(oktaAttr, ((DSValue) valIter.next()).getValue());
//                        output.removeValue(oktaAttr, ((DSValue) valIter.next()).getValue());
//                    }
//                }
//                if (theOp.getOperationType() == DSValueOperation.ADD_VALUE)
//                {
//                    Vector values = theOp.getValues();
//                    System.out.println("att vector length: " + values.size());
//                    Iterator valIter = values.iterator();
//                    while (valIter.hasNext())
//                    {
//                        output.addValue(oktaAttr, ((DSValue) valIter.next()).getValue());
//                    }
//
//                }
//
//            }
//
//        }
//
//        if (passFlow.equals("random"))
//        {
//            //set random password                
//           //Do we really want this on every modify?
//            
//            //Don't update on a modify
//           // output.addPassword(getPWString());
//
//        }
//        else
//        {
//            if (!passFlow.equals("never"))
//            {
//                //handle password
//                Iterator passwordIter = attrVector.iterator();
//                while (passwordIter.hasNext())
//                {
//                    DSAttribute theAttr = (DSAttribute) passwordIter.next();
//                    if (theAttr.getAttributeName().equals("password"))
//                    {
//                        Vector theOps = theAttr.getValueOperations();
//                        Iterator theOpsIterator = theOps.iterator();
//                        while (theOpsIterator.hasNext())
//                        {
//                            DSValueOperation theOp = (DSValueOperation) theOpsIterator.next();
//
//                            String pwValue = theOp.getFirstValue().getValue();
//                            if (pwValue.equalsIgnoreCase("random"))
//                            {
//                                pwValue = getPWString();
//                            }
//
//                            output.addPassword(pwValue);
//
//                        }
//                    }
//                }
//            }
//        }
//
//        System.out.println(">>>> " + output);
//
//        return output;
//    }

    private void loadSchema()
    {
       // tracer.trace("getXDSSchema");


        if (schemaJSON.has("definitions"))
        {

            JSONObject baseDefs = schemaJSON.getJSONObject("definitions").getJSONObject("base");
            Iterator<String> baseKeys = baseDefs.getJSONObject("properties").keys();

            while (baseKeys.hasNext())
            {
                String propertyKey = baseKeys.next();
                JSONObject property = baseDefs.getJSONObject("properties").getJSONObject(propertyKey);

                String required = "false";

                if (baseDefs.getJSONArray("required").toString().contains("\"" + propertyKey + "\"")) //base properties only have reqired key if value is true. Custom has an array
                {
                    required = "true";
                }

                OktaAttribute theAttr = new OktaAttribute();
                theAttr.name = propertyKey;
                theAttr.required = Boolean.getBoolean(required);
                theAttr.setType(property);
                oktaAttributes.put(propertyKey, theAttr);
               
            }

            if (schemaJSON.getJSONObject("definitions").has("custom"))
            {
                JSONObject customDefs = schemaJSON.getJSONObject("definitions").getJSONObject("custom");
                Iterator<String> customKeys = customDefs.getJSONObject("properties").keys();
                while (customKeys.hasNext())
                {
                    String propertyKey = customKeys.next();
                    JSONObject property = customDefs.getJSONObject("properties").getJSONObject(propertyKey);

                    String required = "false";

                    if (customDefs.getJSONArray("required").toString().contains("\"" + propertyKey + "\"")) //base properties only have reqired key if value is true. Custom has an array
                    {
                        required = "true";
                    }
                    //name       required naming
                    OktaAttribute theAttr = new OktaAttribute();
                    theAttr.name = propertyKey;
                    theAttr.required = Boolean.getBoolean(required);
                    theAttr.setType(property);
                    oktaAttributes.put(propertyKey, theAttr);


                  
                }

            }



        }


    }


    protected String getPWString()
    {
        String SALTCHARS = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ1234567890";
        StringBuilder salt = new StringBuilder();
        Random rnd = new Random();
        while (salt.length() < 30)
        { // length of the random string.
            int index = (int) (rnd.nextFloat() * SALTCHARS.length());
            salt.append(SALTCHARS.charAt(index));
        }
        String saltStr = salt.toString();
        return saltStr;

    }

    public void setPasswordFlow(String passFlow)
    {
        this.passFlow = passFlow;
    }

}
