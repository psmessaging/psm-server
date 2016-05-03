/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.psm.java.managers;

import com.psm.java.support.Static;
import java.util.ArrayList;
import java.util.regex.Pattern;
import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.LocalBean;
import me.prettyprint.cassandra.serializers.BooleanSerializer;
import me.prettyprint.cassandra.serializers.StringSerializer;
import me.prettyprint.hector.api.Cluster;
import me.prettyprint.hector.api.Keyspace;
import me.prettyprint.hector.api.factory.HFactory;
import me.prettyprint.hector.api.mutation.Mutator;
import org.codehaus.jettison.json.JSONArray;

/**
 *
 * @author Sean
 */
@Stateless
@LocalBean
public class ChatManager {

    @EJB
    private UserManager mUserMan;
    
    @EJB
    private GroupManager mGroupMan;
    
    public JSONArray roomList(String insiderId)
    {
        
        return null;
    }
    
    
    public String createRoom(String insiderId, String roomName, ArrayList<String> insiderIds, ArrayList<String> groupIds)
    {
        
        StringSerializer ser = StringSerializer.get();
        BooleanSerializer bser = BooleanSerializer.get();
        
        Cluster cluster = HFactory.getOrCreateCluster(Static.CLUSTER_NAME, Static.CLUSTER_ADDRT);
        Keyspace keyspace = HFactory.createKeyspace(Static.KEYSPACE_NAME, cluster);
        Mutator mutator = HFactory.createMutator(keyspace, ser);
        
        String newRoomId = Static.generateUniqueId();
        Pattern userNamePattern = Pattern.compile("^[A-Za-z]");
        
        mutator.addInsertion(insiderId, Static.COLUMN_CHATROOMS, HFactory.createStringColumn(newRoomId, roomName));
        ArrayList<String> allIds = new ArrayList<String>();
        int counter = 0;
        //Add insiderids to the list
        for(String string : insiderIds)
        {
            if(allIds.contains(string))
                continue;
            
            if(mUserMan.isInsiderIdValid(string))
            {
                allIds.add(string);
                counter++;
            }
        }
        
        for(String id : allIds)
        {
           // mutator.addInsertion(id, id, null)
        }
        /*for(String string : groupIds)
        {
            ArrayList<String> members = mGroupMan.getGroupMemberIds(insiderId, string, 100, null);
            for(String id : members)
            {
                if(mUserMan.isInsiderIdValid(string))
                {
                    allIds.add(string);
                    counter++;
                }
            }
        }*/
        
        //public static String COLUMN_CHATROOMS    = "Chat";
        //public static String COLUMN_CHATMEMBERS = "ChatMembers";
        return "";
    }
}
