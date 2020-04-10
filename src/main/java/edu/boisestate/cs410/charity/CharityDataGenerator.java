package edu.boisestate.cs410.charity;

import com.github.javafaker.Faker;
import com.google.common.base.Preconditions;
import org.apache.commons.math3.distribution.EnumeratedIntegerDistribution;
import org.apache.commons.math3.distribution.IntegerDistribution;
import org.apache.commons.math3.distribution.PoissonDistribution;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.Instant;
import java.util.*;

/**
 * Class to generate example data for the Charity database.
 */
public class CharityDataGenerator {
    private static String[] FUNDS = {
            "General",
            "Veterinary Assistance",
            "Lobbying",
            "Cat Sheltering",
            "Dog Sheltering"
    };
    private static double[] fund_weights = {
            10, 5, 1, 2, 2
    };
    private static Instant FIRST_DATE = Instant.parse("2000-01-01T00:00:00.00Z");
    private static Instant LAST_DATE = Instant.parse("2020-01-01T00:00:00.00Z");

    private final int donorCount;
    private final double giftsPerDonor;

    private Faker faker = new Faker();
    private PoissonDistribution giftCountDist;
    private PoissonDistribution fundsPerGift;

    private Connection db;

    private List<Integer> donors;
    private List<Integer> funds;

    public CharityDataGenerator(Connection cxn, int dc, double gpd) {
        db = cxn;
        donorCount = dc;
        giftsPerDonor = gpd;
        giftCountDist = new PoissonDistribution(gpd);
        fundsPerGift = new PoissonDistribution(1);
    }

    public void generate() throws SQLException {
        insertFunds();
        generateDonors();
        generateGifts();
    }

    public void insertFunds() throws SQLException {
        String q = "INSERT INTO fund (fund_id, fund_name) VALUES (?, ?) ON CONFLICT DO NOTHING";
        try (var ps = db.prepareStatement(q)) {
            for (int i = 0; i < FUNDS.length; i++) {
                ps.setInt(1, i + 1);
                ps.setString(2, FUNDS[i]);
                int n = ps.executeUpdate();
                if (n > 0) {
                    System.out.format("inserted fund %s%n", FUNDS[i]);
                }
            }
        }
    }

    public void generateDonors() throws SQLException {
        Preconditions.checkState(donors == null, "donors already generated");
        String q = "INSERT INTO donor (donor_name, donor_email, donor_address, donor_city, donor_state, donor_zip) " +
                "VALUES (?, ?, ?, ?, ?, ?) RETURNING donor_id";
        donors = new ArrayList<>();
        try (var ps = db.prepareStatement(q)) {
            for (int i = 0; i < donorCount; i++) {
                String name = faker.name().name();
                String email = faker.internet().emailAddress();
                String address = faker.address().streetAddress();
                String city = faker.address().city();
                String state = faker.address().stateAbbr();
                String zip = faker.address().zipCodeByState(state);
                ps.setString(1, name);
                ps.setString(2, email);
                ps.setString(3, address);
                ps.setString(4, city);
                ps.setString(5, state);
                ps.setString(6, zip);
                try (var rs = ps.executeQuery()) {
                    rs.next();
                    int id = rs.getInt(1);
                    System.out.format("donor %d: %s%n", id, name);
                    donors.add(id);
                }
            }
        }
    }

    private void generateGifts() throws SQLException {
        for (int donor: donors) {
            int ngifts = giftCountDist.sample();
            generateGiftsForDonor(donor, ngifts);
        }
    }

    private void generateGiftsForDonor(int donor, int ngifts) throws SQLException {
        String gq = "INSERT INTO gift (gift_date, donor_id) VALUES (?, ?) RETURNING gift_id";
        String fq = "INSERT INTO gift_fund_allocation (gift_id, fund_id, amount) VALUES (?, ?, ?)";
        try (var igs = db.prepareStatement(gq); var ias = db.prepareStatement(fq)) {
            igs.setInt(2, donor);
            for (int i = 0; i < ngifts; i++) {
                var rawDate = faker.date().between(java.util.Date.from(FIRST_DATE), java.util.Date.from(LAST_DATE));
                var date = new java.sql.Date(rawDate.toInstant().toEpochMilli());
                igs.setDate(1, date);
                int giftId;
                try (var rs = igs.executeQuery()) {
                    rs.next();
                    giftId = rs.getInt(1);
                }

                int nallocs = sampleFundCount();

                ias.setInt(1, giftId);
                var funds = selectFunds(nallocs);
                for (int f: funds) {
                    int pennies = faker.number().numberBetween(100, 100*10000);
                    var dollars = new BigDecimal(pennies).divide(new BigDecimal(100));
                    ias.setInt(2, f + 1);
                    ias.setBigDecimal(3, dollars);
                    ias.executeUpdate();
                }
            }
        }
    }

    private int sampleFundCount() {
        int nallocs = Integer.MAX_VALUE;
        while (nallocs > FUNDS.length) {
            nallocs = fundsPerGift.sample();
        }
        if (nallocs == 0) nallocs = 1;  // 0s become 1, so 1 is vastly more frequent
        return nallocs;
    }

    private List<Integer> selectFunds(int n) {
        List<Integer> funds = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            var dist = makeFundDist(funds);
            funds.add(dist.sample());
        }

        return funds;
    }

    private IntegerDistribution makeFundDist(Collection<Integer> exclude) {
        int n = fund_weights.length - exclude.size();
        int[] funds = new int[n];
        double[] weights = new double[n];
        int j = 0;
        for (int i = 0; i < FUNDS.length; i++) {
            if (!exclude.contains(i)) {
                funds[j] = i;
                weights[j] = fund_weights[i];
                j++;
            }
        }
        assert j == n;
        return new EnumeratedIntegerDistribution(funds, weights);
    }
}
