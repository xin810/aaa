/*
 * Copyright (c) 2014 Hewlett-Packard Development Company, L.P. and others.
 * All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.aaa.idm.persistence;

/**
 *
 * @author peter.mellquist@hp.com
 *
 */

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import org.opendaylight.aaa.idm.IdmLightApplication;
import org.opendaylight.aaa.idm.model.User;
import org.opendaylight.aaa.idm.model.Users;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sqlite.JDBC;

public class UserStore {
   private static Logger logger = LoggerFactory.getLogger(UserStore.class);
   protected Connection  dbConnection = null;
   protected final static String SQL_ID             = "userid";
   protected final static String SQL_NAME           = "name";
   protected final static String SQL_EMAIL          = "email";
   protected final static String SQL_PASSWORD       = "password";
   protected final static String SQL_DESCR          = "description";
   protected final static String SQL_ENABLED        = "enabled";
   public final static int       MAX_FIELD_LEN      = 128;

   protected Connection getDBConnect() throws StoreException {
      if ( dbConnection==null ) {
         try {
            JDBC jdbc = new JDBC();
	    dbConnection = DriverManager.getConnection (IdmLightApplication.config.dbPath);
            return dbConnection;
         }
         catch (Exception e) {
            throw new StoreException("Cannot connect to database server "+ e);
         }
      }
      else {
         try {
            if ( dbConnection.isClosed()) {
               try {
                  JDBC jdbc = new JDBC();
		  dbConnection = DriverManager.getConnection (IdmLightApplication.config.dbPath);
		  return dbConnection;
               }
               catch (Exception e) {
                  throw new StoreException("Cannot connect to database server "+ e);
               }
            }
            else
               return dbConnection;
         }
	 catch (SQLException sqe) {
            throw new StoreException("Cannot connect to database server "+ sqe);
         }
      }
   }

   protected Connection dbConnect() throws StoreException {
      Connection conn;
      try {
         conn = getDBConnect();
      }
      catch (StoreException se) {
         throw se;
      }
      try {
         DatabaseMetaData dbm = conn.getMetaData();
         ResultSet rs = dbm.getTables(null, null, "users", null);
         if (rs.next()) {
            debug("users Table already exists");
         }
         else
         {
            logger.info("users Table does not exist, creating table");
            Statement stmt = null;
            stmt = conn.createStatement();
            String sql = "CREATE TABLE users " +
                         "(userid    INTEGER PRIMARY KEY AUTOINCREMENT," +
                         "name       VARCHAR(128)      NOT NULL, " +
                         "email      VARCHAR(128)      NOT NULL, " +
                         "password   VARCHAR(128)      NOT NULL, " +
                         "description VARCHAR(128)     NOT NULL, " +
                         "enabled     INTEGER          NOT NULL)" ;
           stmt.executeUpdate(sql);
           stmt.close();
         }
      }
      catch (SQLException sqe) {
         throw new StoreException("Cannot connect to database server "+ sqe);
      }
      return conn;
   }


   protected void dbClose() {
      if (dbConnection != null)
      {
         try {
            dbConnection.close ();
          }
          catch (Exception e) {
            logger.error("Cannot close Database Connection " + e);
          }
       }
   }

   @Override
protected void finalize ()  {
      dbClose();
   }

   protected User rsToUser(ResultSet rs) throws SQLException {
      User user = new User();
      try {
         user.setUserid(rs.getInt(SQL_ID));
         user.setName(rs.getString(SQL_NAME));
         user.setEmail(rs.getString(SQL_EMAIL));
         user.setPassword(rs.getString(SQL_PASSWORD));
         user.setDescription(rs.getString(SQL_DESCR));
         user.setEnabled(rs.getInt(SQL_ENABLED)==1?true:false);
      }
      catch (SQLException sqle) {
         logger.error( "SQL Exception : " + sqle);
            throw sqle;
      }
      return user;
   }

   public Users getUsers() throws StoreException {
      Users users = new Users();
      List<User> userList = new ArrayList<User>();
      Connection conn = dbConnect();
      Statement stmt=null;
      String query = "SELECT * FROM users";
      try {
         stmt=conn.createStatement();
         ResultSet rs=stmt.executeQuery(query);
         while (rs.next()) {
            User user = rsToUser(rs);
            userList.add(user);
         }
         rs.close();
         stmt.close();
         dbClose();
      }
      catch (SQLException s) {
         dbClose();
         throw new StoreException("SQL Exception : " + s);
      }
      users.setUsers(userList);
      return users;
   }

   public Users getUsers(String username) throws StoreException {
      Users users = new Users();
      List<User> userList = new ArrayList<User>();
      Connection conn = dbConnect();
      Statement stmt=null;
      String query = "SELECT * FROM users WHERE name='" + username +"'";
      try {
         stmt=conn.createStatement();
         ResultSet rs=stmt.executeQuery(query);
         while (rs.next()) {
            User user = rsToUser(rs);
            userList.add(user);
         }
         rs.close();
         stmt.close();
         dbClose();
      }
      catch (SQLException s) {
         dbClose();
         throw new StoreException("SQL Exception : " + s);
      }
      users.setUsers(userList);
      return users;
   }


   public User getUser(long id) throws StoreException {
      Connection conn = dbConnect();
      Statement stmt=null;
      String query = "SELECT * FROM users WHERE userid=" + id;
      try {
         stmt=conn.createStatement();
         ResultSet rs=stmt.executeQuery(query);
         if (rs.next()) {
            User user = rsToUser(rs);
            rs.close();
            stmt.close();
            dbClose();
            return user;
         }
         else {
            rs.close();
            stmt.close();
            dbClose();
            return null;
         }
      }
      catch (SQLException s) {
         dbClose();
         throw new StoreException("SQL Exception : " + s);
      }
   }

   public User createUser(User user) throws StoreException {
       int key=0;
       Connection conn = dbConnect();
       try {
          String query = "insert into users (name,email,password,description,enabled) values(?,?,?,?,?)";
          PreparedStatement statement = conn.prepareStatement(query, Statement.RETURN_GENERATED_KEYS);
          statement.setString(1,user.getName());
          statement.setString(2,user.getEmail());
          statement.setString(3,user.getPassword());
          statement.setString(4,user.getDescription());
          statement.setInt(5,user.getEnabled()?1:0);
          int affectedRows = statement.executeUpdate();
          if (affectedRows == 0)
             throw new StoreException("Creating user failed, no rows affected.");
          ResultSet generatedKeys = statement.getGeneratedKeys();
          if (generatedKeys.next())
             key = generatedKeys.getInt(1);
          else
             throw new StoreException("Creating user failed, no generated key obtained.");
          user.setUserid(key);
          dbClose();
          return user;
       }
       catch (SQLException s) {
          dbClose();
          throw new StoreException("SQL Exception : " + s);
       }
   }

   public User putUser(User user) throws StoreException {

      User savedUser = this.getUser(user.getUserid());
      if (savedUser==null)
         return null;

      if (user.getDescription()!=null)
         savedUser.setDescription(user.getDescription());
      if (user.getName()!=null)
         savedUser.setName(user.getName());
      if (user.getEnabled()!=null)
         savedUser.setEnabled(user.getEnabled());
      if (user.getEmail()!=null)
         savedUser.setEmail(user.getEmail());
      if (user.getPassword()!=null)
         savedUser.setPassword(user.getPassword());

      Connection conn = dbConnect();
      try {
         String query = "UPDATE users SET name = ?, email = ?, password = ?, description = ?, enabled = ? WHERE userid = ?";
         PreparedStatement statement = conn.prepareStatement(query);
         statement.setString(1, savedUser.getName());
         statement.setString(2, savedUser.getEmail());
         statement.setString(3, savedUser.getPassword());
         statement.setString(4, savedUser.getDescription());
         statement.setInt(5, savedUser.getEnabled()?1:0);
         statement.setInt(6,savedUser.getUserid());
         statement.executeUpdate();
         statement.close();
         dbClose();
      }
      catch (SQLException s) {
         dbClose();
         throw new StoreException("SQL Exception : " + s);
      }

      return savedUser;
   }

   public User deleteUser(User user) throws StoreException {
      User savedUser = this.getUser(user.getUserid());
      if (savedUser==null)
         return null;

      Connection conn = dbConnect();
      Statement stmt=null;
      String query = "DELETE FROM users WHERE userid=" + user.getUserid();
      try {
         stmt=conn.createStatement();
         int deleteCount = stmt.executeUpdate(query);
         debug("deleted " + deleteCount + " records");
         stmt.close();
         dbClose();
         return savedUser;
      }
      catch (SQLException s) {
         dbClose();
         throw new StoreException("SQL Exception : " + s);
      }
   }

   private static final void debug(String msg) {
       if (logger.isDebugEnabled())
           logger.debug(msg);
   }
}

