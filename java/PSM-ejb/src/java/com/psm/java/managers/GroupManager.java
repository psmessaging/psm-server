/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.psm.java.managers;

import com.psm.java.support.Static;
import com.psm.java.helpers.JStatistics;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.LocalBean;
import me.prettyprint.cassandra.model.IndexedSlicesQuery;
import me.prettyprint.cassandra.serializers.BooleanSerializer;
import me.prettyprint.cassandra.serializers.StringSerializer;
import me.prettyprint.cassandra.serializers.LongSerializer;
import me.prettyprint.hector.api.Cluster;
import me.prettyprint.hector.api.Keyspace;
import me.prettyprint.hector.api.beans.ColumnSlice;
import me.prettyprint.hector.api.beans.HColumn;
import me.prettyprint.hector.api.beans.HCounterColumn;
import me.prettyprint.hector.api.factory.HFactory;
import me.prettyprint.hector.api.mutation.Mutator;
import me.prettyprint.hector.api.query.*;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONObject;

/**
 *
 * @author Sean
 */
@Stateless
@LocalBean
public class GroupManager {


    @EJB
    private UserManager mUserM;
    
    public ArrayList<String> createGroup(String insiderId, String name, boolean isSecret,
                                ArrayList<String> groupFbIds, ArrayList<String> groupInIds)
    {
        
        StringSerializer ser = StringSerializer.get();
        BooleanSerializer bser = BooleanSerializer.get();
        LongSerializer lser = LongSerializer.get();
        Cluster cluster = HFactory.getOrCreateCluster(Static.CLUSTER_NAME, Static.CLUSTER_ADDRT);
        Keyspace keyspace = HFactory.createKeyspace(Static.KEYSPACE_NAME, cluster);
        Mutator mutator = HFactory.createMutator(keyspace, ser);
        
        String newGroupId = Static.generateUniqueId();
        Pattern userNamePattern = Pattern.compile("^[A-Za-z]");        
        //Add to column groups for this user because they own it
        mutator.addInsertion(insiderId, Static.COLUMN_GROUPS, HFactory.createStringColumn(newGroupId, name));
         
        ArrayList<String> notInsider = new ArrayList<String>();  
        long counter = 1;
        ArrayList<String> ids = new ArrayList<String>();
        for(String inid : groupInIds)
        {
            
            if(!ids.contains(inid))
            {
                Matcher matcher = userNamePattern.matcher(inid);
                if(matcher.matches())
                {
                    //Starts with a letter is a username
                    String realId = mUserM.getIdFromUsername(inid);
                    if(realId != null)
                    {
                        if(mUserM.isInsiderIdValid(realId))
                        {
                            if(!ids.contains(realId) && !realId.equals(insiderId))
                            {
                                ids.add(realId);
                                counter++;
                            }
                        }
                    } 
                }
                else
                {
                    if(mUserM.isInsiderIdValid(inid))
                    {
                        if(!ids.contains(inid) && !inid.equals(insiderId))
                        {
                            ids.add(inid);
                            counter++;
                        }
                    }
                }
            }    
        }
        for(String fbid : groupFbIds)
        {
            String iid = mUserM.getIdFromFbId(fbid);
            if(iid != null)
            {
                if(!ids.contains(iid))
                {
                    ids.add(iid);
                    counter++;
                }
            }
            else
            {
                notInsider.add(fbid);
            }
        }
        
        for(String string : ids)
        {
            if(!isSecret)
                mutator.addInsertion(string, Static.COLUMN_GROUPS, HFactory.createStringColumn(newGroupId, name));
            mutator.addInsertion(newGroupId, Static.COLUMN_GROUPMEMBERS, HFactory.createColumn(string, false, ser, bser));
            
        }
        
        mutator.addInsertion(newGroupId, Static.COLUMN_GROUPMEMBERS, HFactory.createColumn("name", name, ser, ser));
        mutator.addInsertion(newGroupId, Static.COLUMN_GROUPMEMBERS, HFactory.createColumn("private", isSecret, ser, bser));
        mutator.addInsertion(newGroupId, Static.COLUMN_GROUPMEMBERS, HFactory.createColumn(insiderId, true, ser, bser)); //true means they can delete it
        mutator.addInsertion(newGroupId, Static.COLUMN_GROUPMEMBERS, HFactory.createColumn("created_time", System.currentTimeMillis(), ser, lser));
        mutator.addInsertion(newGroupId, Static.COLUMN_GROUPMEMBERS, HFactory.createColumn("updated_time", System.currentTimeMillis(), ser, lser));
        //mutator.addInsertion(newGroupId, Static.COLUMN_GROUPMEMBERS, HFactory.createColumn("count", counter, ser, lser));
        //System.out.println(counter);
        mutator.addCounter(newGroupId, Static.COLUMN_COUNTERS, HFactory.createCounterColumn("members", counter, ser));
        mutator.execute();
        JStatistics.incrementCounter(keyspace, Static.CR_COUNTERS_GROUPS);
        if(isSecret)
            JStatistics.incrementCounter(keyspace, Static.CR_COUNTERS_PRIVATEGROUPS);
        else
            JStatistics.incrementCounter(keyspace, Static.CR_COUNTERS_PUBLICGROUPS);
        
        return notInsider;
    }
    
    public ArrayList<String> getGroupMemberIds(String groupId, String insiderId, int limit, String last)
    {
        ArrayList<String> list = new ArrayList<String>();
        
        if(isMember(insiderId, groupId))
        {
            StringSerializer ser = StringSerializer.get();
            BooleanSerializer bser = BooleanSerializer.get();

            Cluster cluster = HFactory.getOrCreateCluster(Static.CLUSTER_NAME, Static.CLUSTER_ADDRT);
            Keyspace keyspace = HFactory.createKeyspace(Static.KEYSPACE_NAME, cluster);
            
            SliceQuery<String,String,Boolean> squery = HFactory.createSliceQuery(keyspace, ser, ser, bser);
            squery.setColumnFamily(Static.COLUMN_GROUPMEMBERS);
            squery.setKey(groupId);
            squery.setRange(null, null, true, limit);
            
            QueryResult<ColumnSlice<String,Boolean>> slice = squery.execute();
            if(slice.get() != null)
            {
                for(HColumn<String,Boolean> col : slice.get().getColumns())
                {
                    list.add(col.getName());
                }
            }
        }
        
        return list;
        
    }
    
    public boolean isMember(String insiderId, String groupId)
    {
        StringSerializer ser = StringSerializer.get();
        BooleanSerializer bser = BooleanSerializer.get();
        
        Cluster cluster = HFactory.getOrCreateCluster(Static.CLUSTER_NAME, Static.CLUSTER_ADDRT);
        Keyspace keyspace = HFactory.createKeyspace(Static.KEYSPACE_NAME, cluster);
        
        ColumnQuery<String,String,String> query = HFactory.createColumnQuery(keyspace, ser, ser, ser);
        query.setColumnFamily(Static.COLUMN_GROUPMEMBERS);
        query.setKey(groupId);
        query.setName(insiderId);
        
        QueryResult<HColumn<String,String>> results = query.execute();
        if(results.get() == null)
            return false;
        else
            return true;
       
    }
    
    public JSONArray getGroupList(String insiderId)
    {
        StringSerializer ser = StringSerializer.get();
        LongSerializer lser = LongSerializer.get();
        
        Cluster cluster = HFactory.getOrCreateCluster(Static.CLUSTER_NAME, Static.CLUSTER_ADDRT);
        Keyspace keyspace = HFactory.createKeyspace(Static.KEYSPACE_NAME, cluster);
        
        SliceQuery<String,String,String> query = HFactory.createSliceQuery(keyspace, ser, ser, ser);
        query.setColumnFamily(Static.COLUMN_GROUPS);
        query.setKey(insiderId);
        query.setRange(null, null, true, 50);
        JSONArray arrayList = new JSONArray();
        
        QueryResult<ColumnSlice<String,String>> result =  query.execute();
        if(result.get().getColumns().size() > 0)
        {
            for(int x = 0; x < result.get().getColumns().size();x++)
            {
                //HashMap<String,String> hm = new HashMap<String,String>();
                String key = result.get().getColumns().get(x).getName();
                CounterQuery<String,String> cquery = HFactory.createCounterColumnQuery(keyspace, ser, ser);
                //CountQuery<String, Long> cquery = HFactory.createCountQuery(keyspace, ser, lser);
                cquery.setKey(key);
                cquery.setColumnFamily(Static.COLUMN_COUNTERS);
                cquery.setName("members");
                //cquery.setRange(null, null, 10);
                QueryResult<HCounterColumn<String>> qr = cquery.execute();
                try {
                    JSONObject obj = new JSONObject();
                    
                    obj.accumulate("id", key);
                    obj.accumulate("name", result.get().getColumns().get(x).getValue());
                    obj.accumulate("count", qr.get().getValue());
                    //JSONArray array = getGroupMembers(key);
                    //obj.accumulate("members", array);
                    
                    JSONObject info = getGroupInfo(key);
                    if(info != null)
                    {
                        if(info.has("private"))
                            obj.accumulate("private", info.getBoolean("private"));
                        if(info.has("created_time"))
                            obj.accumulate("created_time", info.getLong("created_time"));
                        if(info.has("updated_time"))
                            obj.accumulate("updated_time", info.getLong("updated_time"));
                    }
                    
                    arrayList.put(obj);
                }catch(Exception ex){}
            }
        }
        
        return arrayList;
    }
    
    public JSONObject getGroupInfo(String groupId)
    {
        BooleanSerializer bser = BooleanSerializer.get();
        LongSerializer lser = LongSerializer.get();
        StringSerializer ser = StringSerializer.get();
        
        Cluster cluster = HFactory.getOrCreateCluster(Static.CLUSTER_NAME, Static.CLUSTER_ADDRT);
        Keyspace keyspace = HFactory.createKeyspace(Static.KEYSPACE_NAME, cluster);
        SliceQuery<String,String,String> squery = HFactory.createSliceQuery(keyspace, ser, ser, ser);
        squery.setKey(groupId);
        squery.setColumnFamily(Static.COLUMN_GROUPMEMBERS);
        squery.setColumnNames("name","private","created_time","updated_time");
        
        QueryResult<ColumnSlice<String,String>> results = squery.execute();
        
        //HashMap<String,String> map = new HashMap<String, String>();
        JSONObject obj = new JSONObject();
        try {
            
            obj.put("name", results.get().getColumnByName("name").getValue());
            obj.put("private", bser.fromByteBuffer(results.get().getColumnByName("private").getValueBytes()));
            obj.put("created_time", lser.fromByteBuffer(results.get().getColumnByName("created_time").getValueBytes()));
            obj.put("updated_time", lser.fromByteBuffer(results.get().getColumnByName("updated_time").getValueBytes()));
            return obj;
        }catch(Exception ex) {}
        return null;
    }
    
    public JSONArray getGroupMembers(String groupId)
    {
        StringSerializer ser = StringSerializer.get();
        Cluster cluster = HFactory.getOrCreateCluster(Static.CLUSTER_NAME, Static.CLUSTER_ADDRT);
        Keyspace keyspace = HFactory.createKeyspace(Static.KEYSPACE_NAME, cluster);
        
        SliceQuery<String,String,String> squery = HFactory.createSliceQuery(keyspace, ser, ser, ser);
        squery.setKey(groupId);
        squery.setColumnFamily(Static.COLUMN_GROUPMEMBERS);
        squery.setRange(null, null, true, 25);
        
        QueryResult<ColumnSlice<String,String>> result = squery.execute();
        JSONArray arrayList = new JSONArray();
        
        for(HColumn<String,String> col : result.get().getColumns())
        {
            try {
                JSONObject obj = new JSONObject();
                String itemId = col.getName();
                if(itemId == null) continue;
                if(!itemId.equalsIgnoreCase("name") && !itemId.equalsIgnoreCase("updated_time")
                        && !itemId.equalsIgnoreCase("private") && !itemId.equalsIgnoreCase("created_time")
                        && !itemId.equalsIgnoreCase("null"))
                {
                    obj.accumulate("id", itemId);
                    String username = mUserM.getUsernameFromId(itemId);
                    if(username != null)
                    {
                        if(username.length() > 2)
                            obj.accumulate("userName", username);
                    }
                    arrayList.put(obj);
                }
            }catch(Exception ex) {}
            
        }
        return arrayList;
    }
    
    public boolean addFbUser(String insiderId, String groupId, String fbId)
    {
        StringSerializer ser = StringSerializer.get();
        BooleanSerializer bser = BooleanSerializer.get();
        
        String toAddInsiderId = mUserM.getIdFromFbId(fbId); //InsiderId of the user to add
        if(toAddInsiderId == null)
            return false;
        
        Cluster cluster = HFactory.getOrCreateCluster(Static.CLUSTER_NAME, Static.CLUSTER_ADDRT);
        Keyspace keyspace = HFactory.createKeyspace(Static.KEYSPACE_NAME, cluster);
        Mutator mutator = HFactory.createMutator(keyspace, ser);
        String groupname = getGroupName(groupId);
        mutator.addInsertion(toAddInsiderId, Static.COLUMN_GROUPS, HFactory.createStringColumn(groupId, groupname));
        mutator.addInsertion(groupId, Static.COLUMN_GROUPMEMBERS, HFactory.createColumn(toAddInsiderId, false, ser, bser)); //true means they can delete it
        mutator.incrementCounter(groupId, Static.COLUMN_COUNTERS, "members", 1L);
        mutator.execute();
        
        return true;
    }
    
    public boolean addInUser(String insiderId, String groupId, String inId)
    {
        StringSerializer ser = StringSerializer.get();
        BooleanSerializer bser = BooleanSerializer.get();
        
        String toAddInsiderId = inId; //InsiderId of the user to add
        if(toAddInsiderId == null)
            return false;
        
        Cluster cluster = HFactory.getOrCreateCluster(Static.CLUSTER_NAME, Static.CLUSTER_ADDRT);
        Keyspace keyspace = HFactory.createKeyspace(Static.KEYSPACE_NAME, cluster);
        Mutator mutator = HFactory.createMutator(keyspace, ser);
        String groupname = getGroupName(groupId);
        mutator.addInsertion(toAddInsiderId, Static.COLUMN_GROUPS, HFactory.createStringColumn(groupId, groupname));
        mutator.addInsertion(groupId, Static.COLUMN_GROUPMEMBERS, HFactory.createColumn(toAddInsiderId, false, ser, bser)); //true means they can delete it
        mutator.incrementCounter(groupId, Static.COLUMN_COUNTERS, "members", 1L);
        mutator.execute();
        return true;
    }
    
    public String getGroupName(String groupId)
    {
        StringSerializer ser = StringSerializer.get();
        Cluster cluster = HFactory.getOrCreateCluster(Static.CLUSTER_NAME, Static.CLUSTER_ADDRT);
        Keyspace keyspace = HFactory.createKeyspace(Static.KEYSPACE_NAME, cluster);
        
        ColumnQuery<String,String,String> query = HFactory.createColumnQuery(keyspace, ser, ser, ser);
        query.setColumnFamily(Static.COLUMN_GROUPMEMBERS);
        query.setName("name");
        query.setKey(groupId);
        
        QueryResult<HColumn<String,String>> result = query.execute();
        if(result.get() != null)
        {
            return result.get().getValue();
        }
        else
            return null;
    }
    
    /*
    public JSONArray getGroups(String insiderId, int limit, String last)
    {
        StringSerializer ser = StringSerializer.get();
        Cluster cluster = HFactory.getOrCreateCluster(Static.CLUSTER_NAME, Static.CLUSTER_ADDRT);
        Keyspace keyspace = HFactory.createKeyspace(Static.KEYSPACE_NAME, cluster);
        
        SliceQuery<String,String,String> query = HFactory.createSliceQuery(keyspace, ser, ser, ser);
        query.setColumnFamily(Static.COLUMN_GROUPS);
        query.setKey(insiderId);
        if(limit <= 0)
            limit = 25;
        
        query.setRange(last, null, true, limit);
        
        QueryResult<ColumnSlice<String,String>> result = query.execute();
        
        JSONArray array = new JSONArray();
        if(result.get() == null) return array;
        
        for(HColumn<String,String> col : result.get().getColumns())
        {
            try {
                JSONObject obj = new JSONObject();
                obj.accumulate(col.getName(), col.getValue());
                array.put(obj);
            }catch(Exception ex){}
        }
        
        return array;
        
    }*/
    
}
