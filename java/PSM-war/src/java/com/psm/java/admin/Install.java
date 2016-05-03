/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.psm.java.admin;

import com.psm.java.support.Static;
import java.util.Date;
import javax.ejb.LocalBean;
import javax.ejb.Stateless;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import me.prettyprint.cassandra.model.BasicColumnDefinition;
import me.prettyprint.cassandra.model.BasicColumnFamilyDefinition;
import me.prettyprint.cassandra.serializers.StringSerializer;
import me.prettyprint.cassandra.service.ThriftCfDef;
import me.prettyprint.hector.api.Cluster;
import me.prettyprint.hector.api.Keyspace;
import me.prettyprint.hector.api.beans.CounterSlice;
import me.prettyprint.hector.api.beans.HCounterColumn;
import me.prettyprint.hector.api.ddl.*;
import me.prettyprint.hector.api.factory.HFactory;
import me.prettyprint.hector.api.mutation.Mutator;
import me.prettyprint.hector.api.query.QueryResult;
import me.prettyprint.hector.api.query.SliceCounterQuery;


@Stateless
@LocalBean
@Path("server")
public class Install {
    
    
    /**
     * TODO: After using this endpoint, delete or comment it out and
     * rebuild/redeploy this web application so that nobody else can
     * run the install again.  Or if you are using Apache HTTP, just make this
     * non-accessable.
     *
     */
    @Path("install")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public String firstInstall()
    {
        Cluster cluster = null;
        try {
            cluster = HFactory.getOrCreateCluster(Static.CLUSTER_NAME, Static.CLUSTER_ADDRT);
        }catch(Exception ex)
        {
            System.out.println(ex.getMessage());
            return ex.getMessage();
        }

        if(cluster == null) {
            return "Install Failed...";
        }
        KeyspaceDefinition keyspaceDef = cluster.describeKeyspace(Static.KEYSPACE_NAME);
        //if (keyspaceDef == null) {
        try {
            setupDB();
        }catch(Exception ex) {
            return ex.getMessage();
        }
        //}
        //start();
        
        //return System.getProperty("java.classpath");
        return "success " + System.getProperty("java.classpath");
    }
    
    @Path("start")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public String start()
    {
        if(Static.PCluster == null)
            Static.PCluster = HFactory.getOrCreateCluster(Static.CLUSTER_NAME, Static.CLUSTER_ADDRT);
        
        return "success";
    }

    /**
     *
     * TODO: Remove for production servers.
     */
    @Path("test")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public String test()
    {
        return Static.generateUniqueId();
    }
    
    public String setupDB()
    {
        
        Cluster cluster = HFactory.getOrCreateCluster(Static.CLUSTER_NAME, Static.CLUSTER_ADDRT);
        //cluster.dropKeyspace(Static.KEYSPACE_NAME, true); //Drop keyspace before creating
        HFactory.createKeyspace(Static.KEYSPACE_NAME, cluster);
        KeyspaceDefinition newKeyspace = HFactory.createKeyspaceDefinition(Static.KEYSPACE_NAME);
        
        ColumnFamilyDefinition def1 = HFactory.createColumnFamilyDefinition(
                                        Static.KEYSPACE_NAME, 
                                        Static.COLUMN_USERS, 
                                        ComparatorType.BYTESTYPE);
        
        ColumnFamilyDefinition def2 = HFactory.createColumnFamilyDefinition(
                                        Static.KEYSPACE_NAME, 
                                        Static.COLUMN_CHATROOMS,
                                        ComparatorType.BYTESTYPE);
        
        ColumnFamilyDefinition def3 = HFactory.createColumnFamilyDefinition(
                                        Static.KEYSPACE_NAME, 
                                        Static.COLUMN_MSGS,
                                        ComparatorType.BYTESTYPE);
        
        ColumnFamilyDefinition def4 = HFactory.createColumnFamilyDefinition(
                                        Static.KEYSPACE_NAME, 
                                        Static.COLUMN_GROUPS,
                                        ComparatorType.BYTESTYPE);
        
        
        ColumnFamilyDefinition def5 = HFactory.createColumnFamilyDefinition(
                                        Static.KEYSPACE_NAME, 
                                        Static.COLUMN_GROUPMEMBERS,
                                        ComparatorType.BYTESTYPE);
        
        ColumnFamilyDefinition def6 = HFactory.createColumnFamilyDefinition(
                                        Static.KEYSPACE_NAME, 
                                        Static.COLUMN_GROUPMEMBERSSUPER,
                                        ComparatorType.BYTESTYPE);
        def6.setColumnType(ColumnType.SUPER);
        
        ColumnFamilyDefinition def7 = HFactory.createColumnFamilyDefinition(
                                        Static.KEYSPACE_NAME, 
                                        Static.COLUMN_COUNTERS,
                                        ComparatorType.BYTESTYPE);
        def7.setDefaultValidationClass(ComparatorType.COUNTERTYPE.getClassName());
        //def7.setColumnType(ColumnType.STANDARD);
        
        ColumnFamilyDefinition def8 = HFactory.createColumnFamilyDefinition(
                                        Static.KEYSPACE_NAME, 
                                        Static.COLUMN_CHATMEMBERS,
                                        ComparatorType.BYTESTYPE);
        
        ColumnFamilyDefinition def9 = HFactory.createColumnFamilyDefinition(
                                        Static.KEYSPACE_NAME, 
                                        Static.COLUMN_CHATMSGS,
                                        ComparatorType.BYTESTYPE);
        
        cluster.addColumnFamily(def1, true);
        cluster.addColumnFamily(def2, true);
        cluster.addColumnFamily(def3, true);
        cluster.addColumnFamily(def4, true);
        cluster.addColumnFamily(def5, true);
        cluster.addColumnFamily(def6, true);
        cluster.addColumnFamily(def7, true);
        cluster.addColumnFamily(def8, true);
        cluster.addColumnFamily(def9, true);
        
        addIndexes();
        setupCounters();
        return "Successfully Setup";
    }
    
    
    public String setupCounters()
    {
        StringSerializer ser = StringSerializer.get();
        Cluster cluster = HFactory.getOrCreateCluster(Static.CLUSTER_NAME, Static.CLUSTER_ADDRT);
        Keyspace keyspace = HFactory.createKeyspace(Static.KEYSPACE_NAME, cluster);
        Mutator mutator = HFactory.createMutator(keyspace, ser);
        mutator.addCounter("1", Static.COLUMN_COUNTERS, HFactory.createCounterColumn(Static.CR_COUNTERS_POSTS, 0L));
        mutator.addCounter("1", Static.COLUMN_COUNTERS, HFactory.createCounterColumn(Static.CR_COUNTERS_FACEBOOK, 0L));
        mutator.addCounter("1", Static.COLUMN_COUNTERS, HFactory.createCounterColumn(Static.CR_COUNTERS_FOURSQUARE, 0L));
        mutator.addCounter("1", Static.COLUMN_COUNTERS, HFactory.createCounterColumn(Static.CR_COUNTERS_FBCHECKINS, 0L));
        mutator.addCounter("1", Static.COLUMN_COUNTERS, HFactory.createCounterColumn(Static.CR_COUNTERS_FBSTATUS, 0L));
        mutator.addCounter("1", Static.COLUMN_COUNTERS, HFactory.createCounterColumn(Static.CR_COUNTERS_SMS, 0L));
        mutator.addCounter("1", Static.COLUMN_COUNTERS, HFactory.createCounterColumn(Static.CR_COUNTERS_USERS, 0L));
        mutator.addCounter("1", Static.COLUMN_COUNTERS, HFactory.createCounterColumn(Static.CR_COUNTERS_PINS, 0L));
        mutator.addCounter("1", Static.COLUMN_COUNTERS, HFactory.createCounterColumn(Static.CR_COUNTERS_GROUPS, 0L));
        mutator.addCounter("1", Static.COLUMN_COUNTERS, HFactory.createCounterColumn(Static.CR_COUNTERS_PRIVATEGROUPS, 0L));
        mutator.addCounter("1", Static.COLUMN_COUNTERS, HFactory.createCounterColumn(Static.CR_COUNTERS_PUBLICGROUPS, 0L));
        mutator.addCounter("1", Static.COLUMN_COUNTERS, HFactory.createCounterColumn(Static.CR_COUNTERS_USESGROUP, 0L));
        mutator.addCounter("1", Static.COLUMN_COUNTERS, HFactory.createCounterColumn(Static.CR_COUNTERS_DELETED, 0L));
        
        
        mutator.execute();
        return "Success";
    }

    /**
     * TODO: Please! remove this for production servers.
     * 
     */
    @Path("stats")
    @GET
    @Produces("text/html")
    public String stats()
    {
        StringSerializer ser = StringSerializer.get();
        Cluster cluster = HFactory.getOrCreateCluster(Static.CLUSTER_NAME, Static.CLUSTER_ADDRT);
        Keyspace keyspace = HFactory.createKeyspace(Static.KEYSPACE_NAME, cluster);
        SliceCounterQuery q = HFactory.createCounterSliceQuery(keyspace, ser, ser);
        q.setKey("1");
        q.setColumnFamily(Static.COLUMN_COUNTERS);
        q.setRange(null, null, true, 25);
        QueryResult<CounterSlice<String>> result = q.execute();
        StringBuilder sb = new StringBuilder();
        sb.append("<b>Created</b> ");
        sb.append(new Date().toString());
        sb.append("<br/>");
        
        for(HCounterColumn<String> col : result.get().getColumns())
        {
            sb.append(col.getName());
            sb.append(": ");
            sb.append(String.valueOf(col.getValue()));
            sb.append("<br/>");
        }
        
         return sb.toString();
    }
    
    private void addIndexes()
    {
        Cluster cluster = HFactory.getOrCreateCluster(Static.CLUSTER_NAME, Static.CLUSTER_ADDRT);
        KeyspaceDefinition keyspaceDef = cluster.describeKeyspace(Static.KEYSPACE_NAME);
        
        for(ColumnFamilyDefinition cfd : keyspaceDef.getCfDefs())
        {
            if(cfd.getName().compareToIgnoreCase(Static.COLUMN_USERS) == 0)
            {
                //Add Username Index
                ColumnFamilyDefinition cdf = cfd;
                
                BasicColumnFamilyDefinition columnFamilyDefinition = new BasicColumnFamilyDefinition(cdf);
                
                BasicColumnDefinition bcdf = new BasicColumnDefinition();
                bcdf.setName(StringSerializer.get().toByteBuffer("fbId"));
                bcdf.setIndexName("idx_fbid");
                bcdf.setIndexType(ColumnIndexType.KEYS);
                bcdf.setValidationClass(ComparatorType.BYTESTYPE.getClassName());
                columnFamilyDefinition.addColumnDefinition(bcdf);
                //cluster.updateColumnFamily(new ThriftCfDef(columnFamilyDefinition));
                
                BasicColumnDefinition bcdf1 = new BasicColumnDefinition();
                bcdf1.setName(StringSerializer.get().toByteBuffer("iToken"));
                bcdf1.setIndexName("idx_itoken");
                bcdf1.setIndexType(ColumnIndexType.KEYS);
                bcdf1.setValidationClass(ComparatorType.BYTESTYPE.getClassName());
                columnFamilyDefinition.addColumnDefinition(bcdf1);
                
                BasicColumnDefinition bcdf2 = new BasicColumnDefinition();
                bcdf2.setName(StringSerializer.get().toByteBuffer("phoneNumber"));
                bcdf2.setIndexName("idx_phone");
                bcdf2.setIndexType(ColumnIndexType.KEYS);
                bcdf2.setValidationClass(ComparatorType.BYTESTYPE.getClassName());
                columnFamilyDefinition.addColumnDefinition(bcdf2);
                
                BasicColumnDefinition bcdf3 = new BasicColumnDefinition();
                bcdf3.setName(StringSerializer.get().toByteBuffer("userName"));
                bcdf3.setIndexName("idx_username");
                bcdf3.setIndexType(ColumnIndexType.KEYS);
                bcdf3.setValidationClass(ComparatorType.BYTESTYPE.getClassName());
                columnFamilyDefinition.addColumnDefinition(bcdf3);
                
                
                try {
                    cluster.updateColumnFamily(new ThriftCfDef(columnFamilyDefinition));
                }catch(Exception ex) {}
            }
            else if(cfd.getName().compareToIgnoreCase(Static.COLUMN_MSGS) == 0)
            {
                //pubId
                ColumnFamilyDefinition cdf = cfd;
                
                BasicColumnFamilyDefinition columnFamilyDefinition = new BasicColumnFamilyDefinition(cdf);
                BasicColumnDefinition bcdf = new BasicColumnDefinition();
                bcdf.setName(StringSerializer.get().toByteBuffer("pubId"));
                bcdf.setIndexName("idx_pubid");
                bcdf.setIndexType(ColumnIndexType.KEYS);
                bcdf.setValidationClass(ComparatorType.BYTESTYPE.getClassName());
                columnFamilyDefinition.addColumnDefinition(bcdf);
                
                BasicColumnDefinition bcdf1 = new BasicColumnDefinition();
                bcdf1.setName(StringSerializer.get().toByteBuffer("insiderId"));
                bcdf1.setIndexName("idx_insiderId");
                bcdf1.setIndexType(ColumnIndexType.KEYS);
                bcdf1.setValidationClass(ComparatorType.BYTESTYPE.getClassName());
                columnFamilyDefinition.addColumnDefinition(bcdf1);
                
                BasicColumnDefinition bcdf2 = new BasicColumnDefinition();
                bcdf2.setName(StringSerializer.get().toByteBuffer("created_time"));
                bcdf2.setIndexName("idx_created");
                bcdf2.setIndexType(ColumnIndexType.KEYS);
                bcdf2.setValidationClass(ComparatorType.BYTESTYPE.getClassName());
                columnFamilyDefinition.addColumnDefinition(bcdf2);
                
                try {
                    cluster.updateColumnFamily(new ThriftCfDef(columnFamilyDefinition));
                }catch(Exception ex) {}
            }
            else if(cfd.getName().compareToIgnoreCase(Static.COLUMN_GROUPS) == 0)
            {
                //pubId
                ColumnFamilyDefinition cdf = cfd;
                
                BasicColumnFamilyDefinition columnFamilyDefinition = new BasicColumnFamilyDefinition(cdf);
                BasicColumnDefinition bcdf = new BasicColumnDefinition();
                bcdf.setName(StringSerializer.get().toByteBuffer("ownerId"));
                bcdf.setIndexName("idx_ownerid");
                bcdf.setIndexType(ColumnIndexType.KEYS);
                bcdf.setValidationClass(ComparatorType.BYTESTYPE.getClassName());
                columnFamilyDefinition.addColumnDefinition(bcdf);
                try {
                    cluster.updateColumnFamily(new ThriftCfDef(columnFamilyDefinition));
                }catch(Exception ex) {}
            }
            
        }
        
        
        
    }
}
