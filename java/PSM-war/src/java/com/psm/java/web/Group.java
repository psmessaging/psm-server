/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.psm.java.web;

import com.psm.java.managers.GroupManager;
import com.psm.java.support.Static;
import com.psm.java.managers.UserManager;
import java.util.ArrayList;
import java.util.HashMap;
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
@Path("group")
@Produces(MediaType.APPLICATION_JSON)
public class Group {

    @EJB
    private GroupManager mGroupM;
    @EJB
    private UserManager mUserM;
    
    @GET
    @Path("self")
    @Produces(MediaType.APPLICATION_JSON)
    public String getOwnedGroups(@QueryParam("iToken") String iToken, @QueryParam("limit") @DefaultValue("50") int limit)
    {
        //TODO: Check that the token is actually valid for all methods
        
        String insiderId = mUserM.getIdFromToken(iToken);
        if(insiderId == null)
            return Static.generateErrorMessage("Invalid access token");
        
        JSONArray array = mGroupM.getGroupList(insiderId);
        return array.toString();
        
    }
    
    
    @POST
    @Path("create")
    @Produces(MediaType.APPLICATION_JSON)
    public String createGroup(@FormParam("iToken") String iToken, @FormParam("groupName") String iGroupName, 
                    @FormParam("facebookIds") JSONArray fbmembers,
                    @FormParam("members") JSONArray members,
                    @FormParam("isSecret") @DefaultValue("false") boolean isSecret)
    {
        
        String insiderId = mUserM.getIdFromToken(iToken);
        if(insiderId == null)
            return Static.generateErrorMessage("Invalid access token");
        
        ArrayList<String> fbids = new ArrayList<String>();
        ArrayList<String> inids = new ArrayList<String>();
        
        try {
            for(int x=0; x <  fbmembers.length();x++)
            {
                fbids.add(fbmembers.getString(x));
            }
        }catch(Exception ex) {}
        
        try {
            for(int x=0; x <  members.length();x++)
            {
                inids.add(members.getString(x));
            }
        }catch(Exception ex) {}
        
        inids.add(insiderId);
        ArrayList<String> ret = mGroupM.createGroup(insiderId, iGroupName, isSecret, fbids, inids);
        
        try {
            JSONObject obj = new JSONObject();
            obj.accumulate("result", true);
            JSONArray array = new JSONArray(ret);
            obj.accumulate("invite", array);
            return obj.toString();
        }catch(Exception ex) {}
        
        return "false";
    }
    
    
    
    
}
