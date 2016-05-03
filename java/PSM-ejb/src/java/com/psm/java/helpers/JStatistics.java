/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.psm.java.helpers;

import com.psm.java.support.Static;
import javax.ejb.Stateless;
import javax.ejb.LocalBean;
import me.prettyprint.cassandra.serializers.StringSerializer;
import me.prettyprint.hector.api.Cluster;
import me.prettyprint.hector.api.Keyspace;
import me.prettyprint.hector.api.factory.HFactory;
import me.prettyprint.hector.api.mutation.Mutator;

/**
 *
 * @author Sean
 */

public class JStatistics {

    /*public static String CR_COUNTERS_POSTS = "posts";
    public static String CR_COUNTERS_FACEBOOK = "facebook";
    public static String CR_COUNTERS_FOURSQUARE = "foursquare";
    public static String CR_COUNTERS_FBCHECKINS = "checkins";
    public static String CR_COUNTERS_FBSTATUS = "status";
    public static String CR_COUNTERS_SMS = "sms";
    public static String CR_COUNTERS_USERS = "users";
    */
    
    public static void incrementCounter( Keyspace keyspace, String Cr)
    {
        StringSerializer ser = StringSerializer.get();
        Mutator mutator = HFactory.createMutator(keyspace, ser);
        mutator.incrementCounter("1", Static.COLUMN_COUNTERS, Cr, 1L);
    }
}
