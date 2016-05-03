/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.psm.java.managers;


import com.psm.java.support.Static;
import com.psm.java.helpers.JStatistics;
import javax.ejb.Stateless;
import javax.ejb.LocalBean;
import me.prettyprint.cassandra.model.IndexedSlicesQuery;
import me.prettyprint.cassandra.serializers.IntegerSerializer;
import me.prettyprint.cassandra.serializers.LongSerializer;
import me.prettyprint.cassandra.serializers.StringSerializer;
import me.prettyprint.hector.api.Cluster;
import me.prettyprint.hector.api.Keyspace;
import me.prettyprint.hector.api.beans.HColumn;
import me.prettyprint.hector.api.beans.OrderedRows;
import me.prettyprint.hector.api.beans.Row;
import me.prettyprint.hector.api.factory.HFactory;
import me.prettyprint.hector.api.mutation.Mutator;
import me.prettyprint.hector.api.query.QueryResult;
import org.codehaus.jettison.json.JSONObject;

/**
 *
 * @author Sean
 */
@Stateless
@LocalBean
public class SmsManager {

    
    
    public String generateSmsUnique()
    {
        String unique = Static.generateUniqueId();
        //Unique is 18 digits get it down to 10
        String end = unique.substring(unique.length()-10);
        return end;
        
    }
    
    public String addSms(String to_phone, String from_phone, String pmsg, String primsg, String insiderId)
    {
        StringSerializer ser = StringSerializer.get();
        LongSerializer lser = LongSerializer.get();
        IntegerSerializer iser = IntegerSerializer.get();
        
        Cluster cluster = HFactory.getCluster(Static.CLUSTER_NAME);
        Keyspace keyspace = HFactory.createKeyspace(Static.KEYSPACE_NAME, cluster);
        Mutator mutator = HFactory.createMutator(keyspace, ser);
        
        String id = Static.generateUniqueId();
        String pubId = generateSmsUnique();
        
        mutator.addInsertion(id, Static.COLUMN_MSGS, HFactory.createStringColumn("pubId", pubId));
        mutator.addInsertion(id, Static.COLUMN_MSGS, HFactory.createStringColumn("message", primsg));
        mutator.addInsertion(id, Static.COLUMN_MSGS, HFactory.createStringColumn("pubmessage", pmsg));
        mutator.addInsertion(id, Static.COLUMN_MSGS, HFactory.createStringColumn("to_address", pmsg));
        mutator.addInsertion(id, Static.COLUMN_MSGS, HFactory.createStringColumn("from_address", pmsg));
        mutator.addInsertion(id, Static.COLUMN_MSGS, HFactory.createStringColumn("insiderId", insiderId));
        mutator.addInsertion(id, Static.COLUMN_MSGS, HFactory.createStringColumn("lockedId", "0"));
        mutator.addInsertion(id, Static.COLUMN_MSGS, HFactory.createColumn("network", Static.NETWORK_SMS, ser, iser));
        mutator.addInsertion(id, Static.COLUMN_MSGS, HFactory.createColumn("created_time", System.currentTimeMillis(), ser, lser));
        mutator.addInsertion(id, Static.COLUMN_MSGS, HFactory.createColumn("expires", 0L, ser, lser));
        mutator.addInsertion(id, Static.COLUMN_MSGS, HFactory.createColumn("type", Static.TYPE_SMS, ser, ser));
        mutator.execute();
        
        JStatistics.incrementCounter(keyspace, Static.CR_COUNTERS_SMS);
        return pubId;
    }
    
    public JSONObject getSms(String pubId, String insiderId)
    {
        
        StringSerializer ser = StringSerializer.get();
        LongSerializer lser = LongSerializer.get();
        IntegerSerializer iser = IntegerSerializer.get();
        
        Cluster cluster = HFactory.getCluster(Static.CLUSTER_NAME);
        Keyspace keyspace = HFactory.createKeyspace(Static.KEYSPACE_NAME, cluster);
        
        
        IndexedSlicesQuery<String,String,String> query = HFactory.createIndexedSlicesQuery(keyspace, ser, ser, ser);
        query.addEqualsExpression("pubId", pubId);
        query.setColumnFamily(Static.COLUMN_MSGS);
        query.setColumnNames("message", "insiderId", "lockedId");
        
        Boolean doLock = false;
        String key = "0";
        Boolean idMatch = false;
        JSONObject obj = new JSONObject();
        
        QueryResult<OrderedRows<String,String,String>> results = query.execute();
        if(results.get().getCount() > 0)
        {
            Row<String,String, String> row = results.get().getList().get(0);
            String lockedId = row.getColumnSlice().getColumnByName("lockedId") != null ? row.getColumnSlice().getColumnByName("lockedId").getValue() : "0";
            String postedId = row.getColumnSlice().getColumnByName("insiderId").getValue();
            key = row.getKey();
            if(lockedId.compareTo("0") == 0)
            {
                //Not locked in yet
                doLock = true;
            }
            
            if(lockedId.compareTo(insiderId) == 0 || postedId.compareTo(insiderId) == 0)
            {
                idMatch = true;
                doLock = false;
            }
                
            if(doLock || idMatch)
            {
                try {
                    obj.accumulate("id", key);
                    obj.accumulate("pubId", pubId);
                    obj.accumulate("message", row.getColumnSlice().getColumnByName("message").getValue());
                }catch(Exception ex){}
            }
        }
        
        if(doLock)
        {
            Mutator mutator = HFactory.createMutator(keyspace, ser);
            mutator.addInsertion(key, Static.COLUMN_MSGS, HFactory.createStringColumn("lockedId", insiderId));
            mutator.execute();
        }
        if(!doLock && !idMatch)
            return null;
        else
            return obj;
        
    }
    
}
