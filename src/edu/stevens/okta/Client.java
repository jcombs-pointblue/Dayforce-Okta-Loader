/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.stevens.okta;

import com.pointbluetech.oauth.OAuth2Details;
import com.pointbluetech.oauth.OAuthConstants;
import com.pointbluetech.oauth.OAuthUtils;
import static com.pointbluetech.oauth.OAuthUtils.getAuthorizationHeaderForAccessToken;
import com.pointbluetech.oktadriver.json.JSONObject;
import java.io.InputStream;
import java.util.Properties;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpEntityEnclosingRequestBase;
import org.apache.http.impl.conn.DefaultProxyRoutePlanner;
import java.util.HashMap;

import java.net.URI;

import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.ssl.SSLContextBuilder;
import org.apache.http.conn.ssl.TrustSelfSignedStrategy;
import org.apache.http.conn.ssl.TrustStrategy;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.HttpHost;
import org.apache.http.Header;
import org.apache.http.conn.routing.HttpRoutePlanner;
import org.apache.http.impl.client.HttpClientBuilder;

import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.client.HttpClient;
import java.io.IOException;
import org.apache.http.HttpResponse;
//import org.json.pointblue.parser.JSONParser;
//import org.json.simple.parser.ParseException;
import org.apache.http.entity.StringEntity;
import java.io.InputStreamReader;
import com.pointbluetech.oktadriver.json.JSONTokener;
import com.pointbluetech.oktadriver.json.JSONException;
import com.pointbluetech.oktadriver.json.JSONArray;
//import com.novell.nds.dirxml.driver.Trace;
//import com.pointbluetech.nds.dirxml.driver.okta.CommonImpl;
import java.util.Properties;
import edu.stevens.okta.Logger;
import java.util.Iterator;

/**
 *
 * @author jcombs
 */
public class Client {

    /**
     *
     */
    public String token;

    /**
     *
     */
    public Properties config;

    /**
     *
     */
    public CloseableHttpClient client;

    /**
     *
     */
    public boolean trustAll = true;

    /**
     *
     */
    public boolean useOAuth = false;

    /**
     *
     */
    public String apiToken = "";

    /**
     *
     */
    public String baseURL = "";

    /**
     *
     */
    public String pageLink;
    //public Trace trace;

    /**
     *
     */
    public boolean useProxy;
    //public String proxyHost;
    // public int proxyPort;
    Properties params;
    Logger trace;

    /**
     *
     * @param trace
     * @param params
     */
    public Client(Logger trace, Properties params) {
        this.trace = trace;
        this.params = params;
        this.apiToken = params.getProperty("token");
        this.baseURL = params.getProperty("oktaURL");
    }

    /**
     *
     * @param endpoint
     * @return
     * @throws IOException
     * @throws JSONException
     */
    public JSONObject get(String endpoint) throws IOException, JSONException {
        //trace.trace("GET: " + endpoint, 3);
        if (useOAuth) {
            loadToken(false);
        }
        if (client == null) {
            client = getHttpClient();
        }
        HttpGet get = new HttpGet(endpoint);
        if (useOAuth) {
            get.addHeader(OAuthConstants.AUTHORIZATION, getAuthorizationHeaderForAccessToken(token));
        } else {

            get.addHeader(OAuthConstants.AUTHORIZATION, "SSWS " + apiToken);
        }

        trace.trace("Auth header: " + get.getFirstHeader(OAuthConstants.AUTHORIZATION), 3);

        HttpResponse response = null;

        response = client.execute(get);
        int code = response.getStatusLine().getStatusCode();
        trace.trace("response Status: " + code, 3);

        if (code == 401 || code == 403) {
            if (useOAuth) {
                loadToken(true);
                get.removeHeaders(OAuthConstants.AUTHORIZATION);
                get.addHeader(OAuthConstants.AUTHORIZATION, getAuthorizationHeaderForAccessToken(token));
                response = client.execute(get);

            }
        }
        code = response.getStatusLine().getStatusCode();
        if (code == 200) {
            InputStream inputStream = response.getEntity().getContent();
            try {

                JSONObject jsonObject = new JSONObject(new JSONTokener(new InputStreamReader(inputStream, "UTF-8")));

//            JSONObject jsonObject = (JSONObject) jsonParser.parse(
//                    new InputStreamReader(inputStream, "UTF-8"));
                return jsonObject;
            } catch (Exception ex) {

                ex.printStackTrace();
                //throw new IOException("Response Failed to parse as JSON: rtnCode:" + code + " respBody:" + OAuthUtils.convertStreamToString(response.getEntity().getContent()));

                throw new IOException("Response Failed to parse as JSON: rtnCode:" + code + " URI: " + get.getURI().toString() + " auth: " + get.getFirstHeader(OAuthConstants.AUTHORIZATION));

            }
        } else {
            if (response.getEntity() != null) {
                // throw new IOException("Request Failed: rtnCode:" + code + " respBody:" + OAuthUtils.convertStreamToString(response.getEntity().getContent()));
                throw new IOException("Request Failed: rtnCode:" + code);

            } else {
                throw new IOException("Request Failed: rtnCode:" + code);

            }

        }
        //return null;
    }

    /**
     *
     * @param endpoint
     * @return
     * @throws IOException
     * @throws JSONException
     */
    public JSONArray getArray(String endpoint) throws IOException, JSONException {

        System.out.println("GET: " + endpoint);
        if (useOAuth) {
            loadToken(false);
        }
        //if (client == null) {
        client = getHttpClient();
        //}
        HttpGet get = new HttpGet(endpoint);
        if (useOAuth) {
            get.addHeader(OAuthConstants.AUTHORIZATION, getAuthorizationHeaderForAccessToken(token));
        } else {
            get.addHeader(OAuthConstants.AUTHORIZATION, "SSWS " + apiToken);
        }
        HttpResponse response = null;
        //System.out.println("before req");

        response = client.execute(get);
        //System.out.println("after req");
        int code = response.getStatusLine().getStatusCode();
        if (code == 401 || code == 403) {
            if (useOAuth) {
                loadToken(true);
                get.removeHeaders(OAuthConstants.AUTHORIZATION);
                get.addHeader(OAuthConstants.AUTHORIZATION, getAuthorizationHeaderForAccessToken(token));
                response = client.execute(get);

            }
        }
        code = response.getStatusLine().getStatusCode();
        if (code == 200) {
            InputStream inputStream = response.getEntity().getContent();
            try {

                JSONArray jsonArray = new JSONArray(new JSONTokener(new InputStreamReader(inputStream, "UTF-8")));

                pageLink = null;
                Header[] links = response.getHeaders("link");
                if (links.length == 2) {
                    String rawHeader = links[1].getValue();
                    pageLink = rawHeader.substring(rawHeader.indexOf("<") + 1, rawHeader.indexOf(">"));
                } else //                for (int i = 0; i < links.length; i++) 
                //                {
                //                    if (links[i].getValue().contains("?after=")) 
                //                    {
                //                        
                //                        String rawHeader = links[i].getValue();
                //                        pageLink = rawHeader.substring(rawHeader.indexOf("<")+1, rawHeader.indexOf(">"));
                //                    }
                //
                //                }
                {
                    //System.out.println(pageLink);
                }

//            JSONObject jsonObject = (JSONObject) jsonParser.parse(
//                    new InputStreamReader(inputStream, "UTF-8"));
                return jsonArray;
            } catch (Exception ex) {
                ex.printStackTrace();
                //throw new IOException("Response Failed to parse as JSON: rtnCode:" + code + " respBody:" + OAuthUtils.convertStreamToString(response.getEntity().getContent()));
                throw new IOException("Response Failed to parse as JSON: rtnCode:" + code);

            }
        } else {
            if (response.getEntity() != null) {
                System.out.println("Code: " + code);
                throw new IOException("Request Failed: rtnCode:" + code + " respBody:" + OAuthUtils.convertStreamToString(response.getEntity().getContent()));

            } else {
                throw new IOException("Request Failed: rtnCode:" + code);

            }

        }
        //return null;
    }

    /*  public String post(String endpoint, String body)
    {
        HttpPost post = new HttpPost(oauthDetails.getAuthenticationServerUrl());

    }
     */

    /**
     *
     * @param endpoint
     * @param body
     * @return
     * @throws IOException
     * @throws JSONException
     */

    public JSONObject post(String endpoint, JSONObject body) throws IOException, JSONException {
        if (useOAuth) {
            loadToken(false);

        }
        if (client == null) {
            client = getHttpClient();
        }
        HttpPost post = new HttpPost(endpoint);
        if (useOAuth) {
            post.addHeader(OAuthConstants.AUTHORIZATION, getAuthorizationHeaderForAccessToken(token));
        } else {
            post.addHeader(OAuthConstants.AUTHORIZATION, "SSWS " + apiToken);
        }
        post.addHeader("content-type", "application/json");
        StringEntity params = new StringEntity(body.toString());

        post.setEntity(params);
        HttpResponse response = null;

        response = client.execute(post);
        int code = response.getStatusLine().getStatusCode();
        if (code == 401 || code == 403) {
            if (useOAuth) {
                loadToken(true);
                post.removeHeaders(OAuthConstants.AUTHORIZATION);
                post.addHeader(OAuthConstants.AUTHORIZATION, getAuthorizationHeaderForAccessToken(token));
                response = client.execute(post);
            }
        }

        code = response.getStatusLine().getStatusCode();
        System.out.println("Code: " + code);
        if (code == 200 || code == 201) {
            InputStream inputStream = response.getEntity().getContent();
            JSONObject jsonObject = new JSONObject(new JSONTokener(new InputStreamReader(inputStream, "UTF-8")));

            return jsonObject;
        } else {
            System.out.println("in else for exception");
            //throw new IOException("Request Failed: rtnCode:" + code + " respBody:" + OAuthUtils.convertStreamToString(response.getEntity().getContent()));
            if (code == 400 || code==403) {
                InputStream inputStream = response.getEntity().getContent();
                JSONObject jsonObject = new JSONObject(new JSONTokener(new InputStreamReader(inputStream, "UTF-8")));
                throw new IOException("Request Failed: rtnCode:" + code + " respBody: " + jsonObject.toString());
            }
            throw new IOException("Request Failed: rtnCode:" + code + " respBody:"+OAuthUtils.convertStreamToString(response.getEntity().getContent()));

        }
        //return null;
    }

    /**
     *
     * @param endpoint
     * @param body
     * @return
     * @throws IOException
     * @throws JSONException
     */
    public JSONObject put(String endpoint, JSONObject body) throws IOException, JSONException {
        if (useOAuth) {
            loadToken(false);
        }

        if (client == null) {
            client = getHttpClient();
        }
        HttpPut put = new HttpPut(endpoint);
        if (useOAuth) {
            put.addHeader(OAuthConstants.AUTHORIZATION, getAuthorizationHeaderForAccessToken(token));
        } else {
            put.addHeader(OAuthConstants.AUTHORIZATION, "SSWS " + apiToken);
        }
        put.addHeader("content-type", "application/json");
        StringEntity params = new StringEntity(body.toString());

        put.setEntity(params);
        HttpResponse response = null;

        response = client.execute(put);
        int code = response.getStatusLine().getStatusCode();
        if (code == 401 || code == 403) {
            if (useOAuth) {
                loadToken(true);
                put.removeHeaders(OAuthConstants.AUTHORIZATION);
                put.addHeader(OAuthConstants.AUTHORIZATION, getAuthorizationHeaderForAccessToken(token));
                response = client.execute(put);
            }
        }

        code = response.getStatusLine().getStatusCode();
        if (code == 200 || code == 201) {
            InputStream inputStream = response.getEntity().getContent();
            JSONObject jsonObject = new JSONObject(new JSONTokener(new InputStreamReader(inputStream, "UTF-8")));

            return jsonObject;
        } else {
            throw new IOException("Request Failed: rtnCode:" + code + " respBody:" + OAuthUtils.convertStreamToString(response.getEntity().getContent()));

        }
    }

    /**
     *
     * @param endpoint
     * @param body
     * @return
     * @throws IOException
     * @throws JSONException
     */
    public JSONObject deleteWithBody(String endpoint, JSONObject body) throws IOException, JSONException {
        if (useOAuth) {
            loadToken(false);
        }
        if (client == null) {
            client = getHttpClient();
        }
        HttpDeleteWithBody delete = new HttpDeleteWithBody(endpoint);
        if (useOAuth) {
            delete.addHeader(OAuthConstants.AUTHORIZATION, getAuthorizationHeaderForAccessToken(token));
        }
        delete.addHeader("content-type", "application/json");
        StringEntity params = new StringEntity(body.toString());
        delete.setEntity(params);
        HttpResponse response = null;

        response = client.execute(delete);
        int code = response.getStatusLine().getStatusCode();
        if (code == 401 || code == 403) {
            if (useOAuth) {
                loadToken(true);
                delete.removeHeaders(OAuthConstants.AUTHORIZATION);
                delete.addHeader(OAuthConstants.AUTHORIZATION, getAuthorizationHeaderForAccessToken(token));
                response = client.execute(delete);
            }
        }

        code = response.getStatusLine().getStatusCode();
        if (code == 200 || code == 201) {
            InputStream inputStream = response.getEntity().getContent();
            JSONObject jsonObject = new JSONObject(new JSONTokener(new InputStreamReader(inputStream, "UTF-8")));

            return jsonObject;
        } else {
            throw new IOException("Request Failed: rtnCode:" + code + " respBody:" + OAuthUtils.convertStreamToString(response.getEntity().getContent()));

        }
    }

    /*
    public String put(String endpoint, String body)
    {

    }

    public String delete(String endpoint, JSONObject body)
    {

    }

    public String delete(String endpoint, String body)
    {

    }
     */

    /**
     *
     * @param forceNew
     */

    public void loadToken(boolean forceNew) {
        if (forceNew || (token == null)) {
            //Load the properties file
            if (config == null) {
                config = OAuthUtils.getClientConfigProps(OAuthConstants.CONFIG_FILE_PATH);

            }

            //Generate the OAuthDetails bean from the config properties file
            OAuth2Details oauthDetails = OAuthUtils.createOAuthDetails(config);

            //Validate Input
            if (!OAuthUtils.isValidInput(oauthDetails)) {
                System.out.println("Please provide valid config properties to continue.");
                System.exit(0);
            }

            //Determine operation
            if (oauthDetails.isAccessTokenRequest()) {
                //Generate new Access token
                String accessToken = OAuthUtils.getAccessToken(oauthDetails);
                if (OAuthUtils.isValid(accessToken)) {
                    token = accessToken;
                    System.out.println("Successfully generated Access token for " + oauthDetails.getGrantType() + " grant_type: " + accessToken);
                } else {
                    System.out.println("Could not generate Access token for " + oauthDetails.getGrantType() + " grant_type");
                    System.exit(0);

                }
            }
        }
    }

    /**
     *
     * @throws IOException
     */
    public void loadUsers() throws IOException {

        //does not include deactivated
        pageLink = baseURL + "/api/v1/users";

        while (pageLink != null) {

            //System.out.println("pageLink not null");
            try {
                Thread.sleep(300l);

            } catch (Exception ex) {

            }
            JSONArray users = getArray(pageLink);

            Iterator userIter = users.iterator();

            while (userIter.hasNext()) {
                JSONObject user = (JSONObject) userIter.next();
                
                //https://siot-admin.okta.com/admin/user/demasteruser/00uy84yd1EubJ7cBY696?reset=no
                

                JSONObject profile = (JSONObject) user.get("profile");
                String cwid = null;
                try {
                    cwid = profile.getString("universalID");

                } catch (JSONException jse) {
                    //System.out.println("No CWID Found: " + profile.getString("login"));
                    continue;
                }
                

                StevensStudentSync.currentOktaUsers.put(cwid, user);
                
                                //https://siot-admin.okta.com/admin/user/demasteruser/00uy84yd1EubJ7cBY696?reset=no
                                // "type": {
 //       "id": "otycd8jni7E2gvlhN695"
 //   }
           

                //System.out.println(login);
            }
        }

        // StevensStudentSync.currentOktaUsers.put("456", new JSONObject());
    }

    /**
     *
     * @return
     */
    public CloseableHttpClient getHttpClient() {
        //CloseableHttpClient httpClient = null;

        CloseableHttpClient httpClient = HttpClients.createDefault();

        String proxyHost = params.getProperty("proxyHost");
        int proxyPort = Integer.parseInt(params.getProperty("proxyPort", "0"));

        if (proxyHost != null) {
            if (proxyPort == 0) {
                throw new RuntimeException("A proxy port is required if a proxy host is specified");
            }
            //Creating an HttpHost object for proxy
            HttpHost proxyhost = new HttpHost(proxyHost, proxyPort);
            //Creating an HttpHost object for target

            //creating a RoutePlanner object
            HttpRoutePlanner routePlanner = new DefaultProxyRoutePlanner(proxyhost);

            //Setting the route planner to the HttpClientBuilder object
            HttpClientBuilder clientBuilder = HttpClients.custom();
            clientBuilder = clientBuilder.setRoutePlanner(routePlanner);

            //Building a CloseableHttpClient
            httpClient = clientBuilder.build();
        } else {
            httpClient = HttpClients.createDefault();

        }

        //CloseableHttpClient httpClient = null;
        //TODO: need to properly set http vs Https
        // httpClient = HttpClients.createDefault();
        /*  try
        {
            httpClient = HttpClients.custom().
                    setSSLHostnameVerifier(NoopHostnameVerifier.INSTANCE).
                    setSSLContext(new SSLContextBuilder().loadTrustMaterial(null, new TrustStrategy()
                    {
                        public boolean isTrusted(X509Certificate[] arg0, String arg1) throws CertificateException
                        {
                            return true;
                        }
                    }).build()).build();
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
         */
        return httpClient;

        /*      try
        {
            SSLContextBuilder builder = new SSLContextBuilder();

            //This will trusy all certs
            TrustStrategy ts = new TrustStrategy()
            {
                @Override
                public boolean isTrusted(X509Certificate[] x509Certificates, String s) throws CertificateException
                {
                    return true; // trust all certs
                }

            };

            if (trustAll)
            {
                builder.loadTrustMaterial(null, ts);

            }
            else
            {
                //This one verifies the domain name is correct but will trust self-signed
                builder.loadTrustMaterial(null, new TrustSelfSignedStrategy());
            }

            SSLConnectionSocketFactory sslsf = new SSLConnectionSocketFactory(
                    builder.build());
            //CloseableHttpClient theClient = HttpClients.custom().setSSLSocketFactory(sslsf).build();
             CloseableHttpClient theClient = HttpClients.custom().setSSLSocketFactory(sslsf).setSSLHostnameVerifier( NoopHostnameVerifier.INSTANCE).build();
            return theClient;
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
        return null;

         */
    }

    /*
       This is used becaus the standard HttpDelete does not support sending a body in the request
     */
    class HttpDeleteWithBody extends HttpEntityEnclosingRequestBase {

        public static final String METHOD_NAME = "DELETE";

        public String getMethod() {
            return METHOD_NAME;
        }

        public HttpDeleteWithBody(final String uri) {
            super();
            setURI(URI.create(uri));
        }

        public HttpDeleteWithBody(final URI uri) {
            super();
            setURI(uri);
        }

        public HttpDeleteWithBody() {
            super();
        }
    }
}
