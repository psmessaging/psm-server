/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.psm.java.support;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import javax.ejb.LocalBean;
import javax.ejb.Stateless;
import me.prettyprint.cassandra.serializers.StringSerializer;
import me.prettyprint.hector.api.Cluster;
import me.prettyprint.hector.api.Keyspace;
import me.prettyprint.hector.api.beans.ColumnSlice;
import me.prettyprint.hector.api.beans.OrderedRows;
import me.prettyprint.hector.api.beans.Row;
import me.prettyprint.hector.api.factory.HFactory;
import me.prettyprint.hector.api.mutation.Mutator;
import me.prettyprint.hector.api.query.ColumnQuery;
import me.prettyprint.hector.api.query.QueryResult;
import me.prettyprint.hector.api.query.RangeSlicesQuery;
import me.prettyprint.hector.api.query.SliceQuery;

/**
 *
 * @author Sean
 */
@Stateless
@LocalBean
public class Monitor {

    public ArrayList<String> getRecentMessages() {
        StringSerializer ser = StringSerializer.get();
        Cluster cluster = HFactory.getCluster(Static.CLUSTER_NAME);
        Keyspace keyspace = HFactory.createKeyspace(Static.KEYSPACE_NAME, cluster);
        
        ArrayList<String> msgs = new ArrayList<String>();
        
        RangeSlicesQuery<String,String,String> query = HFactory.createRangeSlicesQuery(keyspace, ser, ser, ser);
        query.setColumnFamily(Static.COLUMN_MSGS);
        query.setRange(null, null, true, 50);
        query.setColumnNames("pubmessage", "message");
        QueryResult<OrderedRows<String,String,String>> result = query.execute();
        
        List<Row<String,String,String>> rows = result.get().getList();
        for (Iterator<Row<String, String, String>> it = rows.iterator(); it.hasNext();) {
            Row<String, String, String> row = it.next();
            msgs.add(row.getColumnSlice().getColumnByName("pubmessage").getValue() + "/" + row.getColumnSlice().getColumnByName("message").getValue());
        }
        
        return msgs;
    }
    
}
