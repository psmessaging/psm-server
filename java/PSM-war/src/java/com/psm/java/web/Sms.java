/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.psm.java.web;

import com.psm.java.managers.SmsManager;
import com.psm.java.support.Static;
import com.psm.java.managers.UserManager;
import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.LocalBean;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONObject;

/**
 *
 * @author Sean
 */
@Stateless
@LocalBean
@Path("sms")
public class Sms {


    @EJB
    UserManager mUserMan;
    @EJB
    SmsManager mSmsMan;
    
    @POST
    @Path("add")
    @Produces(MediaType.APPLICATION_JSON)
    public String addSmsMessage(@FormParam("iToken") String iToken, @FormParam("pubmessage") String pubmessage, @FormParam("message") String message,
                                    @FormParam("from_phone") String from_phone, @FormParam("to_phone") String dest_phone)
    {
        String insiderId = mUserMan.getIdFromToken(iToken);
        
        try {
            JSONObject object = new JSONObject();
            String newId = mSmsMan.addSms(dest_phone, from_phone, pubmessage, message, insiderId);
            object.accumulate("id", newId);
            return object.toString();
        }catch(Exception ex){ return Static.generateErrorMessage("Unknown Error: " + ex.getMessage() );}
        
        //return Static.generateErrorMessage("Unknown Error: ");
        //return ""; 
    }
    
    @POST
    @Path("batch")
    @Produces(MediaType.APPLICATION_JSON)
    public String batchRetrieveSms(@FormParam("iToken") String iToken, @FormParam("keys") JSONArray idArray)
    {
        JSONObject obj = new JSONObject();
        String insiderId = mUserMan.getIdFromToken(iToken);
        if(insiderId == null)
            return Static.generateErrorMessage("Invalid User Token");
        
        if(!mUserMan.isLoggedIn(insiderId))
            return Static.generateErrorMessage("Invalid User State");
        
        try {
            for(int x = 0;x < idArray.length();x++)
            {
                String pubId = idArray.getString(x);
                if(obj.has(pubId))
                    continue;
                JSONObject robj = mSmsMan.getSms(pubId, insiderId);
                if(robj != null)
                    obj.accumulate(pubId, robj);
            }
        }catch(Exception ex) {}
        
        return obj.toString();
    }
    
    @GET
    @Path("test")
    @Produces(MediaType.APPLICATION_JSON)
    public String testMessage()
    {
        return mSmsMan.generateSmsUnique();
    }
 
}
