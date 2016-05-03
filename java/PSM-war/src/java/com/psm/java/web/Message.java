/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.psm.java.web;

import com.psm.java.managers.MessageManager;
import com.psm.java.managers.UserManager;
import com.psm.java.support.Static;
import com.sun.jersey.core.header.FormDataContentDisposition;
import com.sun.jersey.multipart.FormDataParam;
import java.io.InputStream;
import javax.ejb.EJB;
import javax.ejb.LocalBean;
import javax.ejb.Stateless;
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
@Path("msg")
@Produces(MediaType.APPLICATION_JSON)
public class Message {
    
    @EJB
    MessageManager mMsgMan;
    @EJB
    UserManager mUserMan;
    
    @POST
    @Path("{network}")
    @Produces(MediaType.APPLICATION_JSON)
    public String postMessage(
                @PathParam("network") String network, 
                /*@QueryParam("itoken") String user_token,*/
                @FormParam("iToken") String puser_token,
                @FormParam("message") String message, 
                @FormParam("id") String publicId ,
                @FormParam("groupId") @DefaultValue("0") String groupId )
    {
        if(puser_token == null) return Static.generateErrorMessage("Requires access token");
        
        String insiderId = mUserMan.getIdFromToken(puser_token);
        if(insiderId == null) return Static.generateErrorMessage("Invalid access token");

        if(!mUserMan.isLoggedIn(insiderId))
            return Static.generateErrorMessage("Invalid User State");
        
        String msgId = mMsgMan.putMessage(insiderId, publicId, message, Static.getNetworkId(network), groupId);
        JSONObject object = new JSONObject();
        try {
            object.accumulate("id", msgId);
        }catch(Exception ex) {}
        
        return object.toString();
    }
    
    @Path("{network}/checkin")
    @POST
    @Produces(MediaType.APPLICATION_JSON)
    public String postCheckin(@FormParam("iToken") String iToken, @FormParam("id") String publicId, @FormParam("publicVenueId") String pubVenueId,
                @FormParam("privateVenueId") String privateMessage, @FormParam("message") @DefaultValue("") String message, 
                    @PathParam("network") String network, @FormParam("groupId") @DefaultValue("0") String groupId)
    {
        String insiderId = mUserMan.getIdFromToken(iToken);
        if(insiderId == null)
        {
            return Static.generateErrorMessage("Invalid access token");
        }
        if(!mUserMan.isLoggedIn(insiderId))
            return Static.generateErrorMessage("Invalid User State");
        
        String result = mMsgMan.putCheckin(insiderId, publicId, message, pubVenueId, 
                            privateMessage, Static.getNetworkId(network), groupId);
        return "{id:\"" + result + "\"}";
    }
    
    @POST
    @Path("{network}/photo")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Produces(MediaType.APPLICATION_JSON)
    public String postPhoto(@QueryParam("iToken") @DefaultValue("890809213") String iToken, @QueryParam("id") @DefaultValue("1") String publicId, 
                @FormDataParam("file") InputStream fileis
                ,@FormDataParam("file") FormDataContentDisposition fileDetail
                ,@FormDataParam("groupId") @DefaultValue("0") String groupId)
    {
        String id = mMsgMan.postImage(publicId, fileis);
        
        return id;
    }
    
    @GET
    @Path("photo")
    @Produces("image/jpeg")
    public InputStream getPhoto(@QueryParam("id") String id)
    {
        return mMsgMan.getImage(id);
    }
            
    @GET
    @Path("{network}")
    @Produces(MediaType.APPLICATION_JSON)
    public String getMessage(@PathParam("iToken") String iToken, @PathParam("network") String network, @QueryParam("id") String pubId)
    {
        String insiderId = mUserMan.getIdFromToken(iToken);
        
        if(insiderId == null)
            return Static.generateErrorMessage("Invalid access token");
        
        if(!mUserMan.isLoggedIn(insiderId))
            return Static.generateErrorMessage("Invalid User State");
        
        int networkId = Static.getNetworkId(network);
        
        
        //String privateMsg = mMsgMan.getMessage(insiderId, pubId, networkId);
        JSONObject returnObject = mMsgMan.getMessage(insiderId, pubId, networkId);
        try {
            if(returnObject != null)
                return returnObject.toString();
        }catch(Exception ex) {}
        
        return prePackageResponse(returnObject).toString();
    }
    
    
    @POST
    @Path("{network}/batch")
    @Produces(MediaType.APPLICATION_JSON)
    public String getMessages(@FormParam("iToken") String iToken, @PathParam("network") String network, @FormParam("ids") JSONArray jsonIdList)
    {
        
        String insiderId = mUserMan.getIdFromToken(iToken);
        
        if(insiderId == null)
            return Static.generateErrorMessage("Invalid access token");
        
        JSONObject returnObject = new JSONObject();
        if(!mUserMan.isLoggedIn(insiderId))
            return Static.generateErrorMessage("Invalid User State");
        
        int networkId = Static.getNetworkId(network);
        try {
            for(int i = 0; i < jsonIdList.length(); i++)
            {
                String id = jsonIdList.getString(i);
                JSONObject message = mMsgMan.getMessage( insiderId, id, networkId );
                if(message != null)
                    returnObject.accumulate(id, message);
            }
        }catch(Exception ex) {}
        
        return prePackageResponse(returnObject).toString();
        
    }
    
    @DELETE
    @Path("{messageid}")
    @Produces(MediaType.APPLICATION_JSON)
    public String deleteMessage(@QueryParam("iToken") String iToken, @PathParam("messageid") String messageId)
    {
        String insiderId = mUserMan.getIdFromToken(iToken);
        if(insiderId == null) return Static.generateErrorMessage("Invalid access token");
        boolean results = mMsgMan.deleteMessage(insiderId,messageId);
        return String.valueOf(results);
    }

    /**
     *
     * @param insiderId
     * @return
     */
    @GET
    @Path("all")
    @Produces(MediaType.APPLICATION_JSON)
    public String getAllMsgs(@QueryParam("id") String insiderId)
    {
        JSONArray array = mMsgMan.getAllMessages(insiderId);
        return array.toString();
    }
    
    public static JSONObject prePackageResponse(Object object)
    {
        JSONObject robj = new JSONObject();
        try {
            robj.accumulate("data", object);

        } catch (Exception ex) {
            
        }
        return robj;    
    }
}
