/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.psm.java.managers;

import com.psm.java.support.Static;
import com.psm.java.helpers.JStatistics;
import com.psm.java.helpers.HttpUtils;
import java.awt.datatransfer.StringSelection;
import java.util.Date;
import java.util.HashMap;
import java.util.Random;
import javax.ejb.Stateless;
import javax.ejb.LocalBean;
import me.prettyprint.cassandra.model.IndexedSlicesQuery;
import me.prettyprint.cassandra.serializers.BooleanSerializer;
import me.prettyprint.cassandra.serializers.IntegerSerializer;
import me.prettyprint.cassandra.serializers.LongSerializer;
import me.prettyprint.cassandra.serializers.StringSerializer;
import me.prettyprint.hector.api.Cluster;
import me.prettyprint.hector.api.Keyspace;
import me.prettyprint.hector.api.beans.ColumnSlice;
import me.prettyprint.hector.api.beans.HColumn;
import me.prettyprint.hector.api.beans.OrderedRows;
import me.prettyprint.hector.api.factory.HFactory;
import me.prettyprint.hector.api.mutation.Mutator;
import me.prettyprint.hector.api.query.ColumnQuery;
import me.prettyprint.hector.api.query.QueryResult;
import me.prettyprint.hector.api.query.SliceQuery;
import org.codehaus.jettison.json.JSONObject;

/**
 *
 * @author Sean
 */
@Stateless
@LocalBean
public class UserManager {

    public String checkFacebook(String access_token)
    {
        String response = HttpUtils.openUrl("https://graph.facebook.com/me?fields=id,verified,name&access_token=" + access_token);
        return response;
    }
    
    public String generateAccessToken(String iId, long expires)
    {
        StringSerializer ser = StringSerializer.get();
        LongSerializer lser = LongSerializer.get();
        
        String iToken = Static.generateUniqueId();
        //System.out.println("Generate Token");
        Cluster cluster = HFactory.getOrCreateCluster(Static.CLUSTER_NAME, Static.CLUSTER_ADDRT);
        Keyspace keyspace = HFactory.createKeyspace(Static.KEYSPACE_NAME, cluster);
        
        Mutator mutator = HFactory.createMutator(keyspace, StringSerializer.get());
        mutator.addInsertion(iId, Static.COLUMN_USERS, HFactory.createStringColumn("iToken", iToken));
        mutator.addInsertion(iId, Static.COLUMN_USERS, HFactory.createColumn("iTokenExpires", expires, ser, lser));//HFactory.createStringColumn("iTokenExpires", iToken));
        mutator.addInsertion(iId, Static.COLUMN_USERS, HFactory.createColumn("lastLogin", System.currentTimeMillis(), ser, lser));
        mutator.execute();
        
        return iToken;
    }
    
    private String encodeAccessToken(String token)
    {
        
        Random rnd = new Random();
        long num = (long)((rnd.nextFloat()*30.f)+1.f);
        long num1 = (long)((rnd.nextFloat()*50.f)+1.f);
        //long g = Long.parseLong(Static.generateUniqueId());

        String token2 = encodeToLetters(token);
        String token1 = encodeToLetters(Static.generateUniqueId());
        String token3 = encodeToLetters(Static.generateUniqueId());
        String finaltoken = String.valueOf(num1) + token3 + token2 + token1 + String.valueOf(num);
        
        //g = g/num;
        //65-90 / 97-122
        return finaltoken;
    }
    
    private String encodeToLetters(String string)
    {
        String fd= "";
        for(int i=0;i<string.length();i+=2)
        {
            int parse = Integer.parseInt(string.substring(i, i+2));
            if((parse >=65 && parse <=90) || (parse>=97 && parse<=122))
            {
                fd += (char)parse;
            }
            else
                fd += string.substring(i,i+2);
        }
        return fd;
    }
        
    public String getUsernameFromId(String insiderId)
    {
        StringSerializer ser = StringSerializer.get();
        Cluster cluster = HFactory.getOrCreateCluster(Static.CLUSTER_NAME, Static.CLUSTER_ADDRT);
        Keyspace keyspace = HFactory.createKeyspace(Static.KEYSPACE_NAME, cluster);
        
        ColumnQuery<String,String,String> query = HFactory.createColumnQuery(keyspace, ser, ser, ser);
        query.setColumnFamily(Static.COLUMN_USERS);
        query.setKey(insiderId);
        query.setName("userName");
        
        QueryResult<HColumn<String,String>> result = query.execute();
        if(result.get() != null)
        {
            return result.get().getValue();
        }
            return null;
    }
    
    public JSONObject getBasicInsiderInfo(String insiderId)
    {
        //HashMap<String, Object> array = new HashMap<String, Object>();
        JSONObject array = new JSONObject();
        
        StringSerializer ser = StringSerializer.get();
        Cluster cluster = HFactory.getOrCreateCluster(Static.CLUSTER_NAME, Static.CLUSTER_ADDRT);
        Keyspace keyspace = HFactory.createKeyspace(Static.KEYSPACE_NAME, cluster);
        
        SliceQuery<String,String,String> query = HFactory.createSliceQuery(keyspace, ser, ser, ser);
        query.setColumnFamily(Static.COLUMN_USERS);
        query.setColumnNames("firstName", "lastName", "userName", "userNameFormat");
        query.setKey(insiderId);
        QueryResult<ColumnSlice<String, String>> result = query.execute();
        if(result.get() == null)
            return array;
        
        for(HColumn<String,String> col : result.get().getColumns())
        {
            try {
                String key = col.getName();
                array.accumulate(key, col.getValue());
                System.out.println(col.getName() + " / " + col.getValue());
            }catch(Exception ex) {}
        }
        
        try {
            array.accumulate("insiderId", insiderId);
        }catch(Exception ex) {}
        
        return array;
    }
    
    public HashMap<String, Object> getFullInsiderInfo(String insiderId)
    {
        HashMap<String, Object> array = new HashMap<String, Object>();
        
        StringSerializer ser = StringSerializer.get();
        Cluster cluster = HFactory.getOrCreateCluster(Static.CLUSTER_NAME, Static.CLUSTER_ADDRT);
        Keyspace keyspace = HFactory.createKeyspace(Static.KEYSPACE_NAME, cluster);
        
        SliceQuery<String,String,String> query = HFactory.createSliceQuery(keyspace, ser, ser, ser);
        query.setColumnFamily(Static.COLUMN_USERS);
        query.setRange(null, null, true, 50);
        query.setKey(insiderId);
        QueryResult<ColumnSlice<String, String>> result = query.execute();
        
        for(HColumn<String,String> col : result.get().getColumns())
        {
            String key = col.getName();
            array.put(key, col.getValue());
            //System.out.println(col.getName() + " / " + col.getValue());
        }
        return array;
    }
    
    /**
     * 
     * @param String - fbId (Facebook Id)
     * @return New Id of the user
     */
    public String addUserWithFacebook(String fbId, String name)
    {
        StringSerializer ser = StringSerializer.get();
        LongSerializer lser = LongSerializer.get();
        
        Cluster cluster = HFactory.getOrCreateCluster(Static.CLUSTER_NAME, Static.CLUSTER_ADDRT);
        Keyspace keyspace = HFactory.createKeyspace(Static.KEYSPACE_NAME, cluster);
        String id = Static.generateUniqueId();
        
        Mutator mutator = HFactory.createMutator(keyspace, StringSerializer.get());
        mutator.addInsertion(id, Static.COLUMN_USERS, HFactory.createStringColumn("fbId", fbId));
        mutator.addInsertion(id, Static.COLUMN_USERS, HFactory.createStringColumn("name", name));
        mutator.addInsertion(id, Static.COLUMN_USERS, HFactory.createColumn("create_time", System.currentTimeMillis(), ser, lser));
        mutator.execute();
        JStatistics.incrementCounter(keyspace, Static.CR_COUNTERS_USERS);
        return id;
    }
    
    /**
     * Check if an Insider account exists for the facebook id
     * @param fbId - (String) facebook id
     * @return String - returns insiderId or "false" if non existent
     */
    public String isExistingUser(String fbId)
    {
        StringSerializer ser = StringSerializer.get();
        
        Cluster cluster = HFactory.getOrCreateCluster(Static.CLUSTER_NAME, Static.CLUSTER_ADDRT);
        Keyspace keyspace = HFactory.createKeyspace(Static.KEYSPACE_NAME, cluster);
        IndexedSlicesQuery<String,String,String> iquery = HFactory.createIndexedSlicesQuery(keyspace, ser, ser, ser);
        iquery.addEqualsExpression("fbId", fbId)
            .setColumnFamily(Static.COLUMN_USERS)
            .setReturnKeysOnly();
        QueryResult<OrderedRows<String,String,String>> results = iquery.execute();
        
        if(results.get().getCount() > 0)
            return results.get().getList().get(0).getKey();
        else
            return "false";
    }
    
    public boolean hasPin(String insiderId)
    {
        if(insiderId == null)
            return false;
        
        StringSerializer ser = StringSerializer.get();
        BooleanSerializer bser = BooleanSerializer.get();
        
        Cluster cluster = HFactory.getOrCreateCluster(Static.CLUSTER_NAME, Static.CLUSTER_ADDRT);
        Keyspace keyspace = HFactory.createKeyspace(Static.KEYSPACE_NAME, cluster);
        
        ColumnQuery<String,String,Boolean> query = HFactory.createColumnQuery(keyspace, ser, ser, bser);
        query.setColumnFamily(Static.COLUMN_USERS);
        query.setKey(insiderId);
        query.setName("usesPin");
        QueryResult<HColumn<String,Boolean>> result = query.execute();
        if(result.get() == null)
            return false;
        
        if(result.get().getValue())
            return true;
        else
            return false;
        
    }
    
    public void logPinOut(String insiderId)
    {
        if(rememberPin(insiderId))
            return;
        
        StringSerializer ser = StringSerializer.get();
        BooleanSerializer bser = BooleanSerializer.get();
        
        Cluster cluster = HFactory.getOrCreateCluster(Static.CLUSTER_NAME, Static.CLUSTER_ADDRT);
        Keyspace keyspace = HFactory.createKeyspace(Static.KEYSPACE_NAME, cluster);
        
        Mutator mutator = HFactory.createMutator(keyspace, ser);
        mutator.addInsertion(insiderId, Static.COLUMN_USERS, HFactory.createColumn("isLoggedIn", false, ser, bser));
        mutator.execute();
        
    }
    
    /**
     * Check if insider access token is valid
     * @param itoken
     * @return 
     */
    public boolean isITokenValid(String iToken)
    {
        StringSerializer ser = StringSerializer.get();
        
        Cluster cluster = HFactory.getOrCreateCluster(Static.CLUSTER_NAME, Static.CLUSTER_ADDRT);
        Keyspace keyspace = HFactory.createKeyspace(Static.KEYSPACE_NAME, cluster);
        IndexedSlicesQuery<String,String,String> iquery = HFactory.createIndexedSlicesQuery(keyspace, ser, ser, ser);
        iquery.addEqualsExpression("iToken", iToken)
            .setColumnFamily(Static.COLUMN_USERS)
            .setColumnNames("iTokenExpires", "iToken");
        QueryResult<OrderedRows<String,String,String>> results = iquery.execute();
        
        if(results.get().getCount() > 0)
        {
            long expires = LongSerializer.get().fromByteBuffer(results.get().getList().get(0).getColumnSlice().getColumnByName("iTokenExpires").getValueBytes());
            if(expires > System.currentTimeMillis())                
                return true;
            else
                return false;
        }
        else
            return false;
        
    }
    
    public boolean rememberPin(String insiderId)
    {
        StringSerializer ser = StringSerializer.get();
        BooleanSerializer bser = BooleanSerializer.get();
        
        Cluster cluster = HFactory.getOrCreateCluster(Static.CLUSTER_NAME, Static.CLUSTER_ADDRT);
        Keyspace keyspace = HFactory.createKeyspace(Static.KEYSPACE_NAME, cluster);
        
        ColumnQuery<String,String,Boolean> query = HFactory.createColumnQuery(keyspace, ser, ser, bser);
        query.setColumnFamily(Static.COLUMN_USERS);
        query.setKey(insiderId);
        query.setName("rememberPin");
        QueryResult<HColumn<String,Boolean>> result = query.execute();
        
        if(result.get() == null)
            return false;
        
        return result.get().getValue();
    }
    
    public boolean isInsiderIdValid(String insiderId)
    {
        StringSerializer ser = StringSerializer.get();
        
        Cluster cluster = HFactory.getOrCreateCluster(Static.CLUSTER_NAME, Static.CLUSTER_ADDRT);
        Keyspace keyspace = HFactory.createKeyspace(Static.KEYSPACE_NAME, cluster);
        
        ColumnQuery<String,String,String> query = HFactory.createColumnQuery(keyspace, ser, ser, ser);
        query.setColumnFamily(Static.COLUMN_USERS);
        query.setKey(insiderId);
        query.setName("userName");
        QueryResult<HColumn<String,String>> result = query.execute();
        if(result != null)
            return true;
        else
            return false;
    }
    
    /**
     * 
     * Is username available or not
     * 
     * @param userName
     * @return {@code boolean} available
     */
    public boolean isUsernameAvailable(String userName)
    {
        StringSerializer ser = StringSerializer.get();
        
        Cluster cluster = HFactory.getOrCreateCluster(Static.CLUSTER_NAME, Static.CLUSTER_ADDRT);
        Keyspace keyspace = HFactory.createKeyspace(Static.KEYSPACE_NAME, cluster);
        IndexedSlicesQuery<String,String,String> iquery = HFactory.createIndexedSlicesQuery(keyspace, ser, ser, ser);
        iquery.addEqualsExpression("userName", userName.toLowerCase())
            .setColumnFamily(Static.COLUMN_USERS)
            .setReturnKeysOnly();
        QueryResult<OrderedRows<String,String,String>> results = iquery.execute();
        
        if(results.get().getCount() > 0)
            return false;
        else
            return true;
    }
    
    public boolean isUserSetup(String insiderId)
    {
        
        StringSerializer ser = StringSerializer.get();
        BooleanSerializer bser = BooleanSerializer.get();
        
        Cluster cluster = HFactory.getOrCreateCluster(Static.CLUSTER_NAME, Static.CLUSTER_ADDRT);
        Keyspace keyspace = HFactory.createKeyspace(Static.KEYSPACE_NAME, cluster);
        ColumnQuery<String,String,Boolean> query = HFactory.createColumnQuery(keyspace, ser, ser, bser);
        query.setColumnFamily(Static.COLUMN_USERS);
        query.setKey(insiderId);
        query.setName("isSetup");
        QueryResult<HColumn<String,Boolean>> result = query.execute();
        if(result.get() != null)
            return result.get().getValue();
        else
            return false;
    }
    
    
    
    /**
     * Get the Insider ID related to the token, returns {@code null} if invalid.
     * Use {@code isITokenValid} instead to validate user.
     * 
     * @param iToken
     * @return String - Insider ID
     */
    public String getIdFromToken(String iToken)
    {
        if(iToken == null)
            return null;
        
        StringSerializer ser = StringSerializer.get();
        
        Cluster cluster = HFactory.getOrCreateCluster(Static.CLUSTER_NAME, Static.CLUSTER_ADDRT);
        Keyspace keyspace = HFactory.createKeyspace(Static.KEYSPACE_NAME, cluster);
        IndexedSlicesQuery<String,String,String> iquery = HFactory.createIndexedSlicesQuery(keyspace, ser, ser, ser);
        iquery.addEqualsExpression("iToken", iToken)
            .setColumnFamily(Static.COLUMN_USERS)
            .setReturnKeysOnly();
        QueryResult<OrderedRows<String,String,String>> results = iquery.execute();
        
        if(results.get().getCount() > 0)
            return results.get().getList().get(0).getKey();
        else
            return null;
        
    }
    
    public String getIdFromFbId(String fbId)
    {
        StringSerializer ser = StringSerializer.get();
        
        Cluster cluster = HFactory.getOrCreateCluster(Static.CLUSTER_NAME, Static.CLUSTER_ADDRT);
        Keyspace keyspace = HFactory.createKeyspace(Static.KEYSPACE_NAME, cluster);
        IndexedSlicesQuery<String,String,String> iquery = HFactory.createIndexedSlicesQuery(keyspace, ser, ser, ser);
        iquery.addEqualsExpression("fbId", fbId)
            .setColumnFamily(Static.COLUMN_USERS)
            .setReturnKeysOnly();
        
        QueryResult<OrderedRows<String,String,String>> results = iquery.execute();
        
        if(results.get().getCount() > 0)
            return results.get().getList().get(0).getKey();
        else
            return null;
    }
    
    public String getIdFromUsername(String username)
    {
        StringSerializer ser = StringSerializer.get();
        
        Cluster cluster = HFactory.getOrCreateCluster(Static.CLUSTER_NAME, Static.CLUSTER_ADDRT);
        Keyspace keyspace = HFactory.createKeyspace(Static.KEYSPACE_NAME, cluster);
        IndexedSlicesQuery<String,String,String> iquery = HFactory.createIndexedSlicesQuery(keyspace, ser, ser, ser);
        iquery.addEqualsExpression("userName", username.toLowerCase())
            .setColumnFamily(Static.COLUMN_USERS)
            .setReturnKeysOnly();
        
        QueryResult<OrderedRows<String,String,String>> results = iquery.execute();
        
        if(results.get().getCount() > 0)
            return results.get().getList().get(0).getKey();
        else
            return null;
    }
    
    public boolean setPin(String insiderId, String pin)
    {
        if(insiderId == null)
            return false;
        
        StringSerializer ser = StringSerializer.get();
        BooleanSerializer bser = BooleanSerializer.get();
        
        Cluster cluster = HFactory.getOrCreateCluster(Static.CLUSTER_NAME, Static.CLUSTER_ADDRT);
        Keyspace keyspace = HFactory.createKeyspace(Static.KEYSPACE_NAME, cluster);
        
        Mutator mutator = HFactory.createMutator(keyspace, ser);
        mutator.addInsertion(insiderId, Static.COLUMN_USERS, HFactory.createStringColumn("pin", pin));
        mutator.addInsertion(insiderId, Static.COLUMN_USERS, HFactory.createColumn("usesPin", true, ser, bser));
        mutator.execute();
        JStatistics.incrementCounter(keyspace, Static.CR_COUNTERS_PINS);
        return true;
    }
    
    public void setUserAccountInfo(String insiderId, String firstName, String lastName, Integer countryCode, String phoneNumber,
                                            String eMail, String userName)
    {
        StringSerializer ser = StringSerializer.get();
        IntegerSerializer iser = IntegerSerializer.get();
        BooleanSerializer bser = BooleanSerializer.get();
        LongSerializer lser = LongSerializer.get();
        
        Cluster cluster = HFactory.getOrCreateCluster(Static.CLUSTER_NAME, Static.CLUSTER_ADDRT);
        Keyspace keyspace = HFactory.createKeyspace(Static.KEYSPACE_NAME, cluster);
        Mutator mutator = HFactory.createMutator(keyspace, ser);
        
        if(firstName != null)
            mutator.addInsertion(insiderId, Static.COLUMN_USERS, HFactory.createStringColumn("firstName", firstName) );
        if(lastName != null)
            mutator.addInsertion(insiderId, Static.COLUMN_USERS, HFactory.createStringColumn("lastName", lastName) );
        if(countryCode != null)
            mutator.addInsertion(insiderId, Static.COLUMN_USERS, HFactory.createColumn("countryCode", countryCode, ser, iser));
        if(phoneNumber != null)
            mutator.addInsertion(insiderId, Static.COLUMN_USERS, HFactory.createStringColumn("phoneNumber", phoneNumber));
        if(eMail != null)
            mutator.addInsertion(insiderId, Static.COLUMN_USERS, HFactory.createStringColumn("eMail", eMail));
        if(userName != null)
        {
            mutator.addInsertion(insiderId, Static.COLUMN_USERS, HFactory.createStringColumn("userName", userName.toLowerCase()));
            mutator.addInsertion(insiderId, Static.COLUMN_USERS, HFactory.createStringColumn("userNameFormat", userName));
        }
        
        mutator.addInsertion(insiderId, Static.COLUMN_USERS, HFactory.createColumn("isSetup", true, ser, bser));
        mutator.addInsertion(insiderId, Static.COLUMN_USERS, HFactory.createColumn("isLoggedIn", false, ser, bser));
        mutator.addInsertion(insiderId, Static.COLUMN_USERS, HFactory.createColumn("usesPin", false, ser, bser));
        mutator.addInsertion(insiderId, Static.COLUMN_USERS, HFactory.createColumn("updated_time", System.currentTimeMillis(), ser, lser));
        mutator.execute();
        
    }
    
    public boolean loginPin(String insiderId, String pin, boolean remember)
    {
        StringSerializer ser = StringSerializer.get();
        BooleanSerializer bser = BooleanSerializer.get();
        LongSerializer lser = LongSerializer.get();
        
        Cluster cluster = HFactory.getOrCreateCluster(Static.CLUSTER_NAME, Static.CLUSTER_ADDRT);
        Keyspace keyspace = HFactory.createKeyspace(Static.KEYSPACE_NAME, cluster);
        SliceQuery<String,String,String> query = HFactory.createSliceQuery(keyspace, ser, ser, ser);
        query.setColumnFamily(Static.COLUMN_USERS);
        query.setKey(insiderId);
        query.setColumnNames("isLoggedIn", "usesPin", "pin");
        
        QueryResult<ColumnSlice<String,String>> slice = query.execute();
        
        if(slice.get() == null)
        {
            Mutator mutator = HFactory.createMutator(keyspace, ser);
            mutator.addInsertion(insiderId, Static.COLUMN_USERS, HFactory.createColumn("isLoggedIn", false, ser, bser));
            mutator.addInsertion(insiderId, Static.COLUMN_USERS, HFactory.createColumn("rememberPin", false, ser, bser));
            mutator.execute();
            return false;
        }
        
        if(pin.compareTo(slice.get().getColumnByName("pin").getValue()) == 0)
        {
            Mutator mutator = HFactory.createMutator(keyspace, ser);
            mutator.addInsertion(insiderId, Static.COLUMN_USERS, HFactory.createColumn("isLoggedIn", true, ser, bser));
            mutator.addInsertion(insiderId, Static.COLUMN_USERS, HFactory.createColumn("usesPin", true, ser, bser));
            mutator.addInsertion(insiderId, Static.COLUMN_USERS, HFactory.createColumn("rememberPin", true, ser, bser));
            mutator.addInsertion(insiderId, Static.COLUMN_USERS, HFactory.createColumn("last_login", System.currentTimeMillis(), ser, lser));
            mutator.execute();
            
            return true;
        }
        else
        {
            Mutator mutator = HFactory.createMutator(keyspace, ser);
            mutator.addInsertion(insiderId, Static.COLUMN_USERS, HFactory.createColumn("isLoggedIn", false, ser, bser));
            mutator.execute();
            return false;
        }
        
    }
    
    public boolean usesPin(String insiderId)
    {
        StringSerializer ser = StringSerializer.get();
        BooleanSerializer bser = BooleanSerializer.get();
        
        Cluster cluster = HFactory.getOrCreateCluster(Static.CLUSTER_NAME, Static.CLUSTER_ADDRT);
        Keyspace keyspace = HFactory.createKeyspace(Static.KEYSPACE_NAME, cluster);
        ColumnQuery<String,String,Boolean> query = HFactory.createColumnQuery(keyspace, ser, ser, bser);
        query.setColumnFamily(Static.COLUMN_USERS);
        query.setKey(insiderId);
        query.setName("usesPin");
        QueryResult<HColumn<String,Boolean>> result = query.execute();
        
        if(result.get() == null)
            return false;
        
        return result.get().getValue();
    }
    
    public boolean isLoggedIn(String insiderId)
    {
        StringSerializer ser = StringSerializer.get();
        BooleanSerializer bser = BooleanSerializer.get();
        
        Cluster cluster = HFactory.getOrCreateCluster(Static.CLUSTER_NAME, Static.CLUSTER_ADDRT);
        Keyspace keyspace = HFactory.createKeyspace(Static.KEYSPACE_NAME, cluster);
        SliceQuery<String,String,Boolean> query = HFactory.createSliceQuery(keyspace, ser, ser, bser);
        query.setColumnFamily(Static.COLUMN_USERS);
        query.setKey(insiderId);
        query.setColumnNames("isLoggedIn", "usesPin");
        
        QueryResult<ColumnSlice<String,Boolean>> slice = query.execute();
        
        if(slice.get() == null)
            return false;
        
        boolean loggedIn = slice.get().getColumnByName("isLoggedIn").getValue();
        boolean usesPin = slice.get().getColumnByName("usesPin") == null ? false : slice.get().getColumnByName("usesPin").getValue();
        
        if(!usesPin)
            return true;
        else if(usesPin && loggedIn)
            return true;
        else
            return false;
          
        //return true;
    }
}
