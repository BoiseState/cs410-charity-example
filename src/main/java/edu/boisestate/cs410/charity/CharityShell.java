package edu.boisestate.cs410.charity;

import com.budhash.cliche.Command;
import com.budhash.cliche.ShellFactory;

import java.io.IOException;
import java.sql.*;

public class CharityShell {
    private final Connection db;

    public CharityShell(Connection cxn) {
        db = cxn;
    }

    @Command
    public void funds() throws SQLException {
        try (Statement stmt = db.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT fund_id, fund_name FROM fund")) {
            System.out.format("Funds:%n");
            while (rs.next()) {
                System.out.format("%d: %s%n",
                                  rs.getInt("fund_id"),
                                  rs.getString("fund_name"));
            }
        }
    }

    @Command
    public void donor(int id) throws SQLException {
        String query = "SELECT donor_name, donor_address, donor_city, donor_state, donor_zip FROM donor WHERE donor_id = ?";
        try (PreparedStatement stmt = db.prepareStatement(query)) {
            stmt.setInt(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                if (!rs.next()) {
                    System.err.format("%d: donor does not exist%n", id);
                    return;
                }
                System.out.format("%s%n", rs.getString("donor_name"));
                System.out.format("%s%n", rs.getString("donor_address"));
                System.out.format("%s, %s %s%n",
                                  rs.getString("donor_city"),
                                  rs.getString("donor_state"),
                                  rs.getString("donor_zip"));
            }
        }
    }

    @Command
    public void echo(String... args) {
        for (int i = 0; i < args.length; i++) {
            if (i > 0) {
                System.out.print(' ');
            }
            System.out.print(args[i]);
        }
        System.out.println();
    }

    public static void main(String[] args) throws IOException, SQLException {
        String dbUrl = args[0];
        try (Connection cxn = DriverManager.getConnection("jdbc:" + dbUrl)) {
            CharityShell shell = new CharityShell(cxn);
            ShellFactory.createConsoleShell("charity", "", shell)
                        .commandLoop();
        }
    }
}
