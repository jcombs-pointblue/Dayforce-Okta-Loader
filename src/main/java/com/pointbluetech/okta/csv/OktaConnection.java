package com.pointbluetech.okta.csv;

import com.pointbluetech.oktadriver.json.JSONArray;
import com.pointbluetech.oktadriver.json.JSONException;
import com.pointbluetech.oktadriver.json.JSONObject;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;

/**
 * Validates connectivity to an Okta tenant and lists users.
 *
 * <p>Reuses {@link Client} for HTTP and SSWS auth. Reads {@code oktaURL}
 * and {@code token} (encrypted) from the supplied {@link Properties}.
 */
public class OktaConnection {

    private final Client client;
    private final String baseURL;

    public OktaConnection(Properties config, Logger trace) {
        this.client = new Client(trace, config);
        this.baseURL = config.getProperty("oktaURL");
        if (this.baseURL == null || this.baseURL.isEmpty()) {
            throw new IllegalArgumentException("oktaURL is required");
        }
    }

    /**
     * Confirms the configured URL is reachable and the SSWS token is accepted.
     * Throws {@link IOException} on any non-2xx response.
     *
     * @return the Okta organization id
     */
    public String validate() throws IOException, JSONException {
        JSONObject org = client.get(baseURL + "/api/v1/org");
        return org.getString("id");
    }

    /**
     * Pages through {@code /api/v1/users} and returns every user the token
     * can see (Okta's default scope: active users).
     */
    public List<JSONObject> listUsers() throws IOException, JSONException {
        List<JSONObject> users = new ArrayList<JSONObject>();
        client.pageLink = baseURL + "/api/v1/users";
        while (client.pageLink != null) {
            JSONArray page = client.getArray(client.pageLink);
            Iterator iter = page.iterator();
            while (iter.hasNext()) {
                users.add((JSONObject) iter.next());
            }
            try {
                Thread.sleep(300L);
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
                break;
            }
        }
        return users;
    }

    /**
     * CLI smoke test: validate the connection, then print one row per user.
     *
     * <pre>
     *   java -cp target/DayforceOktaLoader.jar \
     *     com.pointbluetech.okta.csv.OktaConnection
     * </pre>
     *
     * Reads {@code csvSync.properties} from the current working directory.
     */
    public static void main(String[] args) throws Exception {
        Properties props = new Properties();
        try (InputStream in = new FileInputStream("csvSync.properties")) {
            props.load(in);
        }
        String logPath = props.getProperty("logPath", "./");
        Logger trace = new Logger(Logger.NORMAL, logPath + "okta-connection-check.log");

        OktaConnection conn = new OktaConnection(props, trace);
        String orgId = conn.validate();
        System.out.println("Connection OK. Okta org id: " + orgId);

        List<JSONObject> users = conn.listUsers();
        System.out.println("User count: " + users.size());
        for (JSONObject u : users) {
            System.out.println(u.toString(2));
            System.out.println();
        }
    }
}
