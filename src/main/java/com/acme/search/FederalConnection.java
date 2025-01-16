package com.acme.search;

import java.sql.PreparedStatement;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

@Service
public class FederalConnection {

    @Autowired
    private FederalConnectionLoader fedConnectionLoader;

    // connect to the federal database and search the forecasts table for entries with the given query
    String doSearch(final String searchTerm) throws SQLException {
        // connect to the federal database
        Connection conn = fedConnectionLoader.getConnection();
        // search the forecasts table for entries with the given query
        String query = "SELECT * FROM forecasts WHERE entry_desc LIKE ?";
        PreparedStatement stmt = conn.prepareStatement(query);
        stmt.setString(1, "%" + searchTerm + "%");
        ResultSet rs = stmt.execute();
        List<String> ids = new ArrayList<>();
        while(rs.next()) {
            String id = rs.getString("entry_id");
            ids.add(id);
        }
        rs.close();
        stmt.close();
        conn.close();
        return String.join(",", ids);
    }
}
