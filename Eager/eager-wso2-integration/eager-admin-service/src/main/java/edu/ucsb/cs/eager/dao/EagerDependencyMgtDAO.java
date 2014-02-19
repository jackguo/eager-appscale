/*
 *  Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *   * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */

package edu.ucsb.cs.eager.dao;

import edu.ucsb.cs.eager.models.APIInfo;
import edu.ucsb.cs.eager.models.ApplicationInfo;
import edu.ucsb.cs.eager.models.DependencyInfo;
import edu.ucsb.cs.eager.models.EagerException;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.apimgt.impl.utils.APIMgtDBUtil;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class EagerDependencyMgtDAO {

    private static final Log log = LogFactory.getLog(EagerDependencyMgtDAO.class);

    public DependencyInfo[] getDependents(APIInfo api) throws EagerException {
        String selectQuery = "SELECT" +
                " DEP.EAGER_DEPENDENCY_NAME AS DEPENDENCY_NAME," +
                " DEP.EAGER_DEPENDENCY_VERSION AS DEPENDENCY_VERSION," +
                " DEP.EAGER_DEPENDENT_NAME AS DEPENDENT_NAME," +
                " DEP.EAGER_DEPENDENT_VERSION AS DEPENDENT_VERSION," +
                " DEP.EAGER_DEPENDENCY_OPERATIONS AS OPERATIONS " +
                "FROM" +
                " EAGER_API_DEPENDENCY DEP " +
                "WHERE" +
                " DEP.EAGER_DEPENDENCY_NAME=?" +
                " AND DEP.EAGER_DEPENDENCY_VERSION=?";
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            conn = APIMgtDBUtil.getConnection();
            ps = conn.prepareStatement(selectQuery);
            ps.setString(1, api.getName());
            ps.setString(2, api.getVersion());
            rs = ps.executeQuery();
            List<DependencyInfo> dependencies = new ArrayList<DependencyInfo>();
            while (rs.next()) {
                DependencyInfo dependency = new DependencyInfo();
                dependency.setName(rs.getString("DEPENDENT_NAME"));
                dependency.setVersion(rs.getString("DEPENDENT_VERSION"));
                dependency.setOperations(getOperationsListFromString(rs.getString("OPERATIONS")));
                dependencies.add(dependency);
            }

            return dependencies.toArray(new DependencyInfo[dependencies.size()]);
        } catch (SQLException e) {
            handleException("Error while obtaining API dependency information", e);
            return null;
        } finally {
            APIMgtDBUtil.closeAllConnections(ps, conn, rs);
        }
    }

    public DependencyInfo[] getDependencies(String name, String version) throws EagerException {
        String selectQuery = "SELECT" +
                " DEP.EAGER_DEPENDENCY_NAME AS DEPENDENCY_NAME," +
                " DEP.EAGER_DEPENDENCY_VERSION AS DEPENDENCY_VERSION," +
                " DEP.EAGER_DEPENDENT_NAME AS DEPENDENT_NAME," +
                " DEP.EAGER_DEPENDENT_VERSION AS DEPENDENT_VERSION," +
                " DEP.EAGER_DEPENDENCY_OPERATIONS AS OPERATIONS " +
                "FROM" +
                " EAGER_API_DEPENDENCY DEP " +
                "WHERE" +
                " DEP.EAGER_DEPENDENT_NAME=?" +
                " AND DEP.EAGER_DEPENDENT_VERSION=?";
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            conn = APIMgtDBUtil.getConnection();
            ps = conn.prepareStatement(selectQuery);
            ps.setString(1, name);
            ps.setString(2, version);
            rs = ps.executeQuery();
            List<DependencyInfo> dependencies = new ArrayList<DependencyInfo>();
            while (rs.next()) {
                DependencyInfo dependency = new DependencyInfo();
                dependency.setName(rs.getString("DEPENDENCY_NAME"));
                dependency.setVersion(rs.getString("DEPENDENCY_VERSION"));
                dependency.setOperations(getOperationsListFromString(rs.getString("OPERATIONS")));
                dependencies.add(dependency);
            }

            return dependencies.toArray(new DependencyInfo[dependencies.size()]);
        } catch (SQLException e) {
            handleException("Error while obtaining API dependency information", e);
            return null;
        } finally {
            APIMgtDBUtil.closeAllConnections(ps, conn, rs);
        }
    }

    public boolean recordDependencies(ApplicationInfo app) throws EagerException {
        Connection conn = null;
        try {
            conn = APIMgtDBUtil.getConnection();
            deleteExistingDependencies(conn, app);
            saveDependencies(conn, app);
            conn.commit();
            return true;
        } catch (SQLException e) {
            handleException("Error while recording API dependency", e);
            return false;
        } finally {
            APIMgtDBUtil.closeAllConnections(null, conn, null);
        }
    }

    private void deleteExistingDependencies(Connection conn, ApplicationInfo app) throws SQLException {
        String deleteQuery = "DELETE FROM EAGER_API_DEPENDENCY WHERE " +
                "(EAGER_DEPENDENT_NAME=? AND EAGER_DEPENDENT_VERSION=?) OR " +
                "(EAGER_DEPENDENCY_NAME=? AND EAGER_DEPENDENCY_VERSION=?)";
        PreparedStatement psDelete = null;
        try {
            psDelete = conn.prepareStatement(deleteQuery);
            psDelete.setString(1, app.getName());
            psDelete.setString(2, app.getVersion());
            psDelete.setString(3, app.getName());
            psDelete.setString(4, app.getVersion());
            psDelete.executeUpdate();
        } finally {
            APIMgtDBUtil.closeAllConnections(psDelete, null, null);
        }
    }

    private void saveDependencies(Connection conn, ApplicationInfo app) throws SQLException {
        if (app.getDependencies() == null && app.getEnclosedAPIs() == null) {
            return;
        }
        String insertQuery = "INSERT INTO EAGER_API_DEPENDENCY (EAGER_DEPENDENCY_NAME, " +
                "EAGER_DEPENDENCY_VERSION, EAGER_DEPENDENT_NAME, EAGER_DEPENDENT_VERSION, " +
                "EAGER_DEPENDENCY_OPERATIONS) VALUES (?,?,?,?,?)";
        PreparedStatement psInsert = null;
        try {
            psInsert = conn.prepareStatement(insertQuery);
            if (app.getDependencies() != null) {
                for (DependencyInfo dependency : app.getDependencies()) {
                    psInsert.setString(1, dependency.getName());
                    psInsert.setString(2, dependency.getVersion());
                    psInsert.setString(3, app.getName());
                    psInsert.setString(4, app.getVersion());
                    psInsert.setString(5, getOperationsListAsString(dependency));
                    psInsert.addBatch();
                }
            }
            if (app.getEnclosedAPIs() != null) {
                for (APIInfo enclosedAPI : app.getEnclosedAPIs()) {
                    psInsert.setString(1, app.getName());
                    psInsert.setString(2, app.getVersion());
                    psInsert.setString(3, enclosedAPI.getName());
                    psInsert.setString(4, enclosedAPI.getVersion());
                    psInsert.setString(5, "");
                    psInsert.addBatch();
                }
            }
            psInsert.executeBatch();
            psInsert.clearBatch();
        } finally {
            APIMgtDBUtil.closeAllConnections(psInsert, null, null);
        }
    }

    private String[] getOperationsListFromString(String operations) {
        if (operations != null) {
            operations = operations.trim();
            if (!"".equals(operations)) {
                return operations.split(",");
            }
        }
        return new String[] { };
    }

    private String getOperationsListAsString(DependencyInfo dependency) {
        String ops = "";
        String[] operations = dependency.getOperations();
        if (operations != null) {
            for (int i = 0; i < operations.length; i++) {
                if (i > 0) {
                    ops += ",";
                }
                ops += operations[i];
            }
        }
        return ops;
    }

    private void handleException(String msg, Exception ex) throws EagerException {
        log.error(msg, ex);
        throw new EagerException(msg, ex);
    }
}
