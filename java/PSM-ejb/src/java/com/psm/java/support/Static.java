
package com.psm.java.support;

import com.psm.java.helpers.HttpUtils;
import me.prettyprint.hector.api.Cluster;

/**
 *Error validating access token: Session has expired at unix time 1337886000. The current unix time is 1337903043.
 * @author Sean
 */
public class Static {

    //TODO: Change to point to your own IDGenerator or use the included one
    public static String IDGENERATOR_URL = "http://127.0.0.1:8080/id/nextid";

    public static Cluster PCluster;
    
    
    public static String KEYSPACE_NAME = "PSMKeySpace";
        
    //Cluster Adress and Name
    public static String CLUSTER_NAME = "PSM Cluster";
    public static String CLUSTER_ADDRT = "127.0.0.1:9160";
    
    public static String COLUMN_USERS   = "Users";
    public static String COLUMN_MSGS    = "Messages";
    
    public static String COLUMN_LOGS    = "Logs";
    public static String COLUMN_GROUPS  = "Groups";
    public static String COLUMN_GROUPMEMBERS = "GroupMembers";
    public static String COLUMN_GROUPMEMBERSSUPER = "SuperGroupMembers"; //Unused At the moment
    public static String COLUMN_CHATROOMS    = "Chat";
    public static String COLUMN_CHATMEMBERS = "ChatMembers";
    public static String COLUMN_CHATMSGS    = "ChatMessages";
    public static String COLUMN_COUNTERS    = "Counters";
    
    public static String CR_COUNTERS_POSTS = "posts";
    public static String CR_COUNTERS_FACEBOOK = "facebook";
    public static String CR_COUNTERS_FOURSQUARE = "foursquare";
    public static String CR_COUNTERS_FBCHECKINS = "checkins";
    public static String CR_COUNTERS_FBSTATUS = "status";
    public static String CR_COUNTERS_SMS = "sms";
    public static String CR_COUNTERS_USERS = "users";
    public static String CR_COUNTERS_PINS = "pins";
    public static String CR_COUNTERS_GROUPS = "groups";
    public static String CR_COUNTERS_PRIVATEGROUPS = "privategroups";
    public static String CR_COUNTERS_PUBLICGROUPS = "publicgroups";
    public static String CR_COUNTERS_USESGROUP  = "usesgroup";
    public static String CR_COUNTERS_DELETED    = "deleted";
    
    public static String TYPE_STATUS   = "status";
    public static String TYPE_LINK     = "link";
    public static String TYPE_PHOTO    = "photo";
    public static String TYPE_CHECKIN  = "checkin";
    public static String TYPE_SMS      = "sms";
    public static String TYPE_CHAT     = "chat";
    
    /**
     * NETWORK_UNKNOWN = 22
     */
    public static int NETWORK_UNKNOWN       = 22;
    /**
     * NETWORK_FACEBOOK = 33
     */
    public static int NETWORK_FACEBOOK      = 33;
    /**
     * NETWORK_FOURSQUARE = 44
     */
    public static int NETWORK_FOURSQUARE    = 44;
    /**
     * NETWORK_SMS = 55
     */
    public static int NETWORK_SMS           = 55;
    /**
     * NETWORK_CHAT = 66
     */
    public static int NETWORK_CHAT          = 66;
    
    public static String AuthError = "Invalid Token";
    
    public static String generateErrorMessage(String error)
    {
        return String.format("{\n\"error\": {\n \"message\": \"%s\"\n}\n}", error);
    }
    
    public static String generateUniqueId()
    {

        //TODO: Change to point to your own IDGenerator or use the included one
        return HttpUtils.openUrl(IDGENERATOR_URL);
        
    }
    
    public static int getNetworkId(String netWork)
    {
        if(netWork.startsWith("face") || netWork.startsWith("fb"))
            return NETWORK_FACEBOOK;
        else if(netWork.startsWith("four") || netWork.startsWith("fs"))
            return NETWORK_FOURSQUARE;
        else if(netWork.startsWith("sms"))
            return NETWORK_SMS;
        else if(netWork.startsWith("chat"))
            return NETWORK_CHAT;
        else
            return NETWORK_UNKNOWN;
    }
    
    public static String stripPhoneNumber(String phonenumber)
    {
        String pn = phonenumber.replaceAll("[\\+]", "").replaceAll("[\\-]", "")
                            .replaceAll("[()]", "");
        return pn;
    }
}
