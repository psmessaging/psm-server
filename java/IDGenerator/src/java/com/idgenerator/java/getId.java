/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.idgenerator.java;

import javax.ejb.LocalBean;
import javax.ejb.Stateless;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;

/**
 *
 * @author Sean
 */
@Stateless
@LocalBean
@Path("/")
public class getId {

    /**
     * Change param 1 or param 2 or both for each instance of this application
     */
    private static Generator generator = new Generator(1, 1);
    
    @GET
    @Path("nextid")
    @Produces("text/plain")
    public String nextId() {
    	return Long.toString(generator.nextId());
    }    
}
