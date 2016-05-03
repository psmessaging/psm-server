/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.psm.java.stats;

import com.psm.java.support.Monitor;
import java.util.ArrayList;
import javax.ejb.EJB;
import javax.ejb.LocalBean;
import javax.ejb.Stateless;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

/**
 * TODO: This entire class should not exist on a production server,
 * Please delete this class if using on a production server.
 * @author Sean
 */
@Stateless
@LocalBean
@Path("monitor")
public class Stats {

    @EJB
    Monitor m_mon;
    
    @Path("get")
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String getRecentMessages() {
        ArrayList<String> list = m_mon.getRecentMessages();
        return list.toString();
    }

    @Path("test")
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String getTest()
    {
        return "Test";
    }
    
}
