/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.psm.java.web;

import com.psm.java.support.Static;
import com.psm.java.managers.UserManager;
import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.LocalBean;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import org.codehaus.jettison.json.JSONObject;

/**
 *
 * @author Sean
 */
@Stateless
@LocalBean
@Path("user")
public class User {
    
    /*
     * 
     * User Name Methods
     * 
     * 
     */
    @EJB
    private UserManager mUserM;
    
    @GET
    @Path("self")
    @Produces(MediaType.APPLICATION_JSON)
    public String selfInfo(@QueryParam("iToken") String iToken)
    {
        String insiderId = mUserM.getIdFromToken(iToken);
        
        
            
        JSONObject obj = new JSONObject();
        if(insiderId == null)
            return obj.toString();
        
        if(!mUserM.isLoggedIn(insiderId))
            return obj.toString();
        
        if(insiderId == null)
            return obj.toString();
        
        try {
            obj.accumulate("isSetup", mUserM.isUserSetup(insiderId));
            obj.accumulate("user", mUserM.getBasicInsiderInfo(insiderId));
        }catch(Exception ex) {}
        
        //JSONObject array = new JSONObject(mUserM.getInsiderInfo(insiderId));
        return obj.toString();
    }
    
    
    @GET
    @Path("available")
    @Produces(MediaType.APPLICATION_JSON)
    public String checkUsername(@QueryParam("iToken") String iToken , @QueryParam("userName") String username)
    {
        
          boolean avail = mUserM.isUsernameAvailable(username);
          
          return String.valueOf(avail);
        
        
    }

    /*
     * 
     * Authorization Methods
     * 
     */
    @POST
    @Path("authorize")
    @Produces(MediaType.APPLICATION_JSON)
    public String userLogin(@FormParam("access_token") String access_token)
    {
        try {
            
            JSONObject obj = new JSONObject(mUserM.checkFacebook(access_token));
            if(obj.has("error") || !obj.has("id"))
                return obj.toString();
                    
            String id = mUserM.isExistingUser(obj.getString("id"));
            
            boolean isSetup = false;
            if(id.compareToIgnoreCase("false") == 0)
            {
                id = mUserM.addUserWithFacebook(obj.getString("id"), obj.getString("name"));
            }
            else
                 isSetup = mUserM.isUserSetup(id);
            
            mUserM.logPinOut(id);
            
            long expiration = System.currentTimeMillis()+10800000;
            
            if(mUserM.rememberPin(id))
            {
                expiration = -1;
            }
            
            obj.remove("name");
            obj.accumulate("isSetup", isSetup);
           
            obj.accumulate("iToken", mUserM.generateAccessToken(id,expiration));
            
            obj.accumulate("usesPin", mUserM.usesPin(id));
            
            obj.accumulate("insiderId", id);
            obj.accumulate("insiderExpires", expiration);
            
            return obj.toString();
            
        }catch(Exception ex) {return ex.getMessage(); }
        
    }
    
    @POST
    @Path("setinfo")
    @Produces(MediaType.APPLICATION_JSON)
    public String userSetInfo(@FormParam("iToken") String iToken, @FormParam("firstName") String firstName, @FormParam("lastName") String lastName,
                                @FormParam("countryCode") String countryCode, @FormParam("phoneNumber") String phoneNumber,
                                    @FormParam("eMail") String eMail, @FormParam("userName") String userName)
    {
        String insiderId = mUserM.getIdFromToken(iToken);
        System.out.println(insiderId);
        if(insiderId == null)
        {
            return "false"; //TODO: Change to something else
        }
        else
        {
            Integer ccode = Integer.parseInt(countryCode.replaceAll("[\\+]", ""));
            mUserM.setUserAccountInfo(insiderId, firstName, lastName, ccode, Static.stripPhoneNumber(phoneNumber), eMail, userName);
        }
        
        return "true";
    }
    /*
     * TODO: Remove
     * 
     * Test Method
     */
    @GET
    @Path("authorize_test")
    @Produces(MediaType.APPLICATION_JSON)
    public String userLoginTest(@QueryParam("access_token") String access_token)
    {
        return userLogin(access_token); 
    }
    
    public static JSONObject prePackageResponse(Object object)
    {
        JSONObject robj = new JSONObject();
        try {
            robj.accumulate("data", object);
        } catch (Exception ex) { }
        return robj;    
    }
    
    @GET
    @Path("full")
    @Produces(MediaType.APPLICATION_JSON)
    public String userFullInfo(@QueryParam("id") String insiderId)
    {
        try{
            JSONObject array = new JSONObject(mUserM.getFullInsiderInfo(insiderId));
            
        return array.toString();
        }catch(Exception ex) {
            System.out.println(ex.getMessage());
        }
        return "false";
    }
    
    @POST
    @Path("pin/login")
    @Produces(MediaType.APPLICATION_JSON)
    public String loginPin(@FormParam("iToken") String iToken, @FormParam("pin") String pin, 
                @FormParam("rememberPin") @DefaultValue("false") boolean rememberMe )
    {
        String insiderId = mUserM.getIdFromToken(iToken);
        return String.valueOf(mUserM.loginPin(insiderId, pin, rememberMe));
    }
    
    @GET
    @Path("pin/use")
    @Produces(MediaType.APPLICATION_JSON)
    public String usesPin(@QueryParam("iToken") String iToken)
    {
        String insiderId = mUserM.getIdFromToken(iToken);
        
        return String.valueOf(mUserM.hasPin(insiderId));
        
    }
    
    @POST
    @Path("pin/set")
    @Produces(MediaType.APPLICATION_JSON)
    public String setPin(@FormParam("iToken") String iToken, @FormParam("pin") String pin)
    {
        if(iToken == null)
            return "false";
        
        String insiderId = mUserM.getIdFromToken(iToken);
        boolean didpin = mUserM.setPin(insiderId, pin);
        
        return String.valueOf(didpin);
    }
}
