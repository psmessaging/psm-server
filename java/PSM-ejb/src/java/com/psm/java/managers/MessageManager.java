/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.psm.java.managers;

import com.psm.java.helpers.JStatistics;
import com.psm.java.support.Static;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.Date;
import javax.ejb.EJB;
import javax.ejb.LocalBean;
import javax.ejb.Stateless;
import me.prettyprint.cassandra.model.IndexedSlicesQuery;
import me.prettyprint.cassandra.serializers.ByteBufferSerializer;
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
import me.prettyprint.hector.api.query.ColumnQuery;
import me.prettyprint.hector.api.query.QueryResult;
import org.apache.commons.io.IOUtils;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONObject;

/**
 *
 * @author Sean
 */
@Stateless
@LocalBean
public class MessageManager {
    
    @EJB
    GroupManager mGroupM;
    
        
    public String putMessage(String insiderId, String publicId, String privateMsg, int NetworkId, String groupId)
    {
        StringSerializer ser = StringSerializer.get();
        IntegerSerializer iser = IntegerSerializer.get();
        LongSerializer lser = LongSerializer.get();
        
        Date date = new Date();
        Cluster cluster = HFactory.getCluster(Static.CLUSTER_NAME);
        Keyspace keyspace = HFactory.createKeyspace(Static.KEYSPACE_NAME, cluster);
        Mutator mutator = HFactory.createMutator(keyspace, ser);
        String id = Static.generateUniqueId();
        
        mutator.addInsertion(id, Static.COLUMN_MSGS, HFactory.createStringColumn("pubId", publicId));
        mutator.addInsertion(id, Static.COLUMN_MSGS, HFactory.createStringColumn("message", privateMsg));
        mutator.addInsertion(id, Static.COLUMN_MSGS, HFactory.createStringColumn("insiderId", insiderId));
        mutator.addInsertion(id, Static.COLUMN_MSGS, HFactory.createColumn("network", NetworkId, ser, iser));
        mutator.addInsertion(id, Static.COLUMN_MSGS, HFactory.createColumn("created_time", date.getTime(), ser, lser));
        mutator.addInsertion(id, Static.COLUMN_MSGS, HFactory.createColumn("expires", 0L, ser, lser));
        mutator.addInsertion(id, Static.COLUMN_MSGS, HFactory.createColumn("type", Static.TYPE_STATUS, ser, ser));
        mutator.addInsertion(id, Static.COLUMN_MSGS, HFactory.createStringColumn("groupId", groupId));
        mutator.execute();
        if(NetworkId == Static.NETWORK_FACEBOOK)
        {
            JStatistics.incrementCounter(keyspace, Static.CR_COUNTERS_FACEBOOK);
            JStatistics.incrementCounter(keyspace, Static.CR_COUNTERS_FBSTATUS);
        }
        if(groupId.compareTo("0") == 0)
            JStatistics.incrementCounter(keyspace, Static.CR_COUNTERS_USESGROUP);
        return id;
    }
    
    public String putCheckin(String insiderId, String publicId, String privateMsg, String publicVenueId, String privateVenueId, int NetworkId, String groupId)
    {
        StringSerializer ser = StringSerializer.get();
        IntegerSerializer iser = IntegerSerializer.get();
        LongSerializer lser = LongSerializer.get();
        
        Date date = new Date();
        Cluster cluster = HFactory.getCluster(Static.CLUSTER_NAME);
        Keyspace keyspace = HFactory.createKeyspace(Static.KEYSPACE_NAME, cluster);
        Mutator mutator = HFactory.createMutator(keyspace, ser);
        String id = Static.generateUniqueId();
        
        mutator.addInsertion(id, Static.COLUMN_MSGS, HFactory.createStringColumn("pubId", publicId));
        mutator.addInsertion(id, Static.COLUMN_MSGS, HFactory.createStringColumn("message", privateMsg));
        mutator.addInsertion(id, Static.COLUMN_MSGS, HFactory.createStringColumn("insiderId", insiderId));
        mutator.addInsertion(id, Static.COLUMN_MSGS, HFactory.createStringColumn("publicVenueId", publicVenueId));
        mutator.addInsertion(id, Static.COLUMN_MSGS, HFactory.createStringColumn("privateVenueId", privateVenueId));
        mutator.addInsertion(id, Static.COLUMN_MSGS, HFactory.createColumn("network", NetworkId, ser, iser));
        mutator.addInsertion(id, Static.COLUMN_MSGS, HFactory.createColumn("created_time", date.getTime(), ser, lser));
        mutator.addInsertion(id, Static.COLUMN_MSGS, HFactory.createColumn("expires", 0L, ser, lser));
        mutator.addInsertion(id, Static.COLUMN_MSGS, HFactory.createColumn("type", Static.TYPE_CHECKIN, ser, ser));
        mutator.addInsertion(id, Static.COLUMN_MSGS, HFactory.createStringColumn("groupId", groupId));
        mutator.execute();
        
        if(NetworkId == Static.NETWORK_FACEBOOK)
        {
            JStatistics.incrementCounter(keyspace, Static.CR_COUNTERS_FACEBOOK);
            JStatistics.incrementCounter(keyspace, Static.CR_COUNTERS_FBCHECKINS);
        }
        else if(NetworkId == Static.NETWORK_FOURSQUARE)
        {
            JStatistics.incrementCounter(keyspace, Static.CR_COUNTERS_FOURSQUARE);
        }
        if(groupId.compareTo("0") != 0)
            JStatistics.incrementCounter(keyspace, Static.CR_COUNTERS_USESGROUP);
        return id;
    }
    
    public JSONObject getMessage(String insiderId, String publicId, int NetworkId)
    {
        StringSerializer ser = StringSerializer.get();
        LongSerializer lser = LongSerializer.get();
        Cluster cluster = HFactory.getCluster(Static.CLUSTER_NAME);
        Keyspace keyspace = HFactory.createKeyspace(Static.KEYSPACE_NAME, cluster);
       
        IndexedSlicesQuery<String,String,String> iquery = HFactory.createIndexedSlicesQuery(keyspace, ser, ser, ser);
        iquery.addEqualsExpression("pubId", publicId)
            .setColumnFamily(Static.COLUMN_MSGS)
            .setRange(null, null, false, 15);
        
        QueryResult<OrderedRows<String,String,String>> results = iquery.execute();
        
        if(results.get().getCount() > 0)
        {
            Row<String,String,String> row = results.get().getList().get(0);
            if(canViewMessage(insiderId, row.getKey()))
            {
                JSONObject obj = new JSONObject();
                try {
                    obj.accumulate("id", row.getKey());  
                }catch(Exception ex) {}
                for(HColumn<String,String> col : row.getColumnSlice().getColumns())
                {
                    try {

                        if(col.getName().compareTo("created_time") == 0)
                        {
                            obj.accumulate(col.getName(), lser.fromByteBuffer(col.getValueBytes()));
                        }
                        else if(col.getName().compareTo("network") == 0)
                        {
                            //
                        }
                        else if(col.getName().compareTo("expires") == 0)
                        {
                            obj.accumulate(col.getName(), lser.fromByteBuffer(col.getValueBytes()));
                        }
                        else
                        {
                            obj.accumulate(col.getName(), col.getValue());
                        }

                    }catch(Exception ex) {}

                }
                return obj;
            }
            else
                return null;
        }
        else
        {
            return null;
        }
    }
        
    public String postImage(String publicId, InputStream is)
    {
        StringSerializer ser = StringSerializer.get();
        IntegerSerializer iser = IntegerSerializer.get();
        LongSerializer lser = LongSerializer.get();
        ByteBufferSerializer bbser = ByteBufferSerializer.get();
        
        Date date = new Date();
        Cluster cluster = HFactory.getCluster(Static.CLUSTER_NAME);
        Keyspace keyspace = HFactory.createKeyspace(Static.KEYSPACE_NAME, cluster);
        Mutator mutator = HFactory.createMutator(keyspace, ser);
        String id = Static.generateUniqueId();
        ByteBuffer data = read(is);
        mutator.addInsertion(id, Static.COLUMN_MSGS, HFactory.createColumn("data", data, ser, bbser));
        mutator.execute();
        return id;
    }
    
    public InputStream getImage(String id)
    {
        StringSerializer ser = StringSerializer.get();
        ByteBufferSerializer bbser = ByteBufferSerializer.get();
        
        Cluster cluster = HFactory.getCluster(Static.CLUSTER_NAME);
        Keyspace keyspace = HFactory.createKeyspace(Static.KEYSPACE_NAME, cluster);
        
        ColumnQuery<String,String,String> query = HFactory.createColumnQuery(keyspace, ser, ser, ser);
        QueryResult<HColumn<String,String>> result = query.execute();
        ByteBuffer bytes = result.get().getValueBytes();
        return newInputStream(bytes);
    }
    
    public static InputStream newInputStream(final ByteBuffer buf) {
        return new InputStream() {

            @Override
            public synchronized int read() throws IOException {
                if (!buf.hasRemaining()) {
                    return -1;
                }
                return (int) (buf.get() & 0xFF);
            }

            @Override
            public synchronized int read(byte[] bytes, int off, int len) throws IOException {
                // Read only what's left
                len = Math.min(len, buf.remaining());
                buf.get(bytes, off, len);
                return len;
            }
        };
    }
    
    public ByteBuffer read(InputStream is)
    {
        try {
            byte[] bytes = IOUtils.toByteArray(is);
            return ByteBuffer.wrap(bytes);
        }catch(Exception ex) {}
        return null;
        
    }
    
    public boolean deleteMessage(String insiderId, String keyId)
    {
        try {
            if(isMessageOwner(insiderId, keyId))
            {
                StringSerializer ser = StringSerializer.get();
                Cluster cluster = HFactory.getOrCreateCluster(Static.CLUSTER_NAME, Static.CLUSTER_ADDRT);
                Keyspace keyspace = HFactory.createKeyspace(Static.KEYSPACE_NAME, cluster);
                Mutator mutator = HFactory.createMutator(keyspace, ser);
                mutator.addDeletion(keyId, Static.COLUMN_MSGS, null, ser);
                mutator.execute();
                JStatistics.incrementCounter(keyspace, Static.CR_COUNTERS_DELETED);
            }
        }catch(Exception ex) { return false; }
        
        
        
        return true;
    }
    
    
    
    public JSONArray getAllMessages(String insiderId)
    {
        JSONArray array = new JSONArray();
        
        StringSerializer ser = StringSerializer.get();
        LongSerializer lser = LongSerializer.get();
        
        Cluster cluster = HFactory.getOrCreateCluster(Static.CLUSTER_NAME, Static.CLUSTER_ADDRT);
        Keyspace keyspace = HFactory.createKeyspace(Static.KEYSPACE_NAME, cluster);
        IndexedSlicesQuery<String,String,String> query = HFactory.createIndexedSlicesQuery(keyspace, ser, ser, ser);
        query.addEqualsExpression("insiderId", insiderId);
        query.setColumnFamily(Static.COLUMN_MSGS);
        query.setRange(null, null, false, 100);
        
        QueryResult<OrderedRows<String,String,String>> result = query.execute();
        for(Row<String,String,String> row :result.get().getList())
        {
            if(!canViewMessage(insiderId, row.getKey()))
                continue;
            JSONObject obj = new JSONObject();
            try {
                obj.accumulate("id", row.getKey());
            }catch(Exception ex) {}
            for(HColumn<String,String> col : row.getColumnSlice().getColumns())
            {
                try{
                    if(col.getName().compareTo("created_time") == 0)
                    {
                        obj.accumulate(col.getName(), lser.fromByteBuffer(col.getValueBytes()));
                    }
                    else if(col.getName().compareTo("network") == 0)
                    {
                        //
                    }
                    else if(col.getName().compareTo("expires") == 0)
                    {
                        obj.accumulate(col.getName(), lser.fromByteBuffer(col.getValueBytes()));
                    }
                    else
                    {
                        obj.accumulate(col.getName(), col.getValue());
                    }
                }catch(Exception ex) {}
                            
            }
            array.put(obj);
        }
        return array;
    }
    
    public boolean canViewMessage(String insiderId, String msgKey)
    {
        StringSerializer ser = StringSerializer.get();
        LongSerializer lser = LongSerializer.get();
        
        Cluster cluster = HFactory.getOrCreateCluster(Static.CLUSTER_NAME, Static.CLUSTER_ADDRT);
        Keyspace keyspace = HFactory.createKeyspace(Static.KEYSPACE_NAME, cluster);
        ColumnQuery<String,String,String> query = HFactory.createColumnQuery(keyspace, ser, ser, ser);
        query.setKey(msgKey);
        query.setColumnFamily(Static.COLUMN_MSGS);
        query.setName("groupId");
        
        QueryResult<HColumn<String,String>> result = query.execute();
        
        if(result.get() == null)
            return false;
        
        String strResult = result.get().getValue();
        return canViewMessageGroup(insiderId, strResult);
    }
    
    public boolean canViewMessageGroup(String insiderId, String groupId)
    {
        if(groupId.compareTo("0") == 0)
            return true;
        else
            return mGroupM.isMember(insiderId, groupId);
    }
    
    public boolean isMessageOwner(String insiderId, String msgKey)
    {
        StringSerializer ser = StringSerializer.get();
        Cluster cluster = HFactory.getOrCreateCluster(Static.CLUSTER_NAME, Static.CLUSTER_ADDRT);
        Keyspace keyspace = HFactory.createKeyspace(Static.KEYSPACE_NAME, cluster);
        ColumnQuery<String,String,String> query = HFactory.createColumnQuery(keyspace, ser, ser, ser);
        query.setKey(msgKey);
        query.setColumnFamily(Static.COLUMN_MSGS);
        query.setName("insiderId");
        
        QueryResult<HColumn<String,String>> result = query.execute();
        
        if(result.get() == null)
            return false;
        String returnInsiderId = result.get().getValue();
        if(returnInsiderId.compareTo(insiderId) == 0)
            return true;
        else
            return false;
    }
}
