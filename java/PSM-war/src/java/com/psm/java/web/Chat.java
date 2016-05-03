/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.psm.java.web;

import javax.ejb.Stateless;
import javax.ejb.LocalBean;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

/**
 *
 * @author Sean
 */
@Stateless
@LocalBean
@Path("chat")
public class Chat {

    
    @GET
    @Path("self")
    @Produces(MediaType.APPLICATION_JSON)
    public String roomList(@PathParam("iToken") String iToken)
    {
        
        return "";
    }
    
}
