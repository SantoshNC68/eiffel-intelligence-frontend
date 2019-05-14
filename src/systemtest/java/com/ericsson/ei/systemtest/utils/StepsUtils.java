package com.ericsson.ei.systemtest.utils;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Iterator;

import org.apache.http.client.ClientProtocolException;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ericsson.eiffelcommons.JenkinsManager;
import com.ericsson.eiffelcommons.subscriptionobject.RestPostSubscriptionObject;
import com.ericsson.eiffelcommons.utils.HttpRequest;
import com.ericsson.eiffelcommons.utils.HttpRequest.HttpMethod;
import com.ericsson.eiffelcommons.utils.ResponseEntity;
import com.ericsson.eiffelcommons.utils.Utils;

public class StepsUtils {
    private static final Logger LOGGER = LoggerFactory.getLogger(StepsUtils.class);

    private static JenkinsManager jenkinsManager;
    private static JSONObject subscriptions = new JSONObject();
    /**
    *
    * This function creates a job in jenkins with an attached script
    *
    * @param jenkinsJobName - Name of the jenkins job
    * @param scriptFileName - FileName of the script which is to be executed when the job is triggered
    * @param jenkinsBaseUrl - Base url to jenkins e.g http://localhost:8070
    * @param jenkinsUserName - Username to the jenkins machine
    * @param jenkinsPassword - Password to the jenkins machine
    * @param jenkinsToken - Token to the jenkins job.
    *
    * @return boolean - If the creation was a success or not
     * @throws Exception
    */
    public static boolean createJenkinsJob(String jenkinsJobName, String scriptFileName, String jenkinsBaseUrl, String jenkinsUsername, String jenkinsPassword, String jenkinsToken, String jenkinsJobXml, String remremBaseUrl) throws Exception {
        jenkinsManager = new JenkinsManager(jenkinsBaseUrl, jenkinsUsername, jenkinsPassword);

        if(!jenkinsManager.pluginExists("Groovy")) {
            jenkinsManager.installPlugin("Groovy", "2.1");
            jenkinsManager.restartJenkins();
        }
        String script = new String(Files.readAllBytes(Paths.get(scriptFileName)));
        script = script.replace("REMREM_BASE_URL_TO_BE_REPLACED", remremBaseUrl);

        String xmlJobData = Utils.getResourceFileAsString(jenkinsJobXml);
        xmlJobData = xmlJobData.replace("SCRIPT_TO_BE_REPLACED", script);
        return jenkinsManager.forceCreateJob(jenkinsJobName, xmlJobData);
    }

    /**
     *
     * This is a function that removes the jenkins jobs after the testing is done.
     * @param jenkinsJobNames - A list with the jobs to remove from jenkins
     *
     * @return
     * @throws Exception
     */
    public static void deleteJenkinsJobs(ArrayList<String> jenkinsJobNames) throws Exception {
        for (int i = 0; i < jenkinsJobNames.size(); i++) {
            String jenkinsJobName = jenkinsJobNames.get(i);
            boolean success = jenkinsManager.deleteJob(jenkinsJobName);

            if (!success) {
                LOGGER.error("Failed to remove job: \"" + jenkinsJobName+ "\" from jenkins");
            }
        }
    }

    /**
     * Creates a subscription that later can be used to send to Eiffel intelligence.
     *
     * @param subscriptionName
     * @param nameOfJobToBeTriggered
     * @param jenkinsUserame
     * @param jenkinsPassword
     * @param jenkinsBaseUrl
     * @throws IOException
     * @throws JSONException
     */
    public static void createSubscription(String subscriptionName, String nameOfJobToBeTriggered, String jenkinsUserame, String jenkinsPassword, String jenkinsBaseUrl) throws IOException, JSONException {
        RestPostSubscriptionObject subscription = new RestPostSubscriptionObject(subscriptionName);
        subscription.setRestPostBodyMediaType("application/x-www-form-urlencoded");
        subscription.setBasicAuth(jenkinsUserame, jenkinsPassword);
        subscription.setNotificationMeta(jenkinsBaseUrl + "/job/" + nameOfJobToBeTriggered + "/buildWithParameters");
        subscriptions.put(subscriptionName, subscription);
    }

    /**
     * Adds a notification to the subscription
     *
     * @param key
     * @param value
     * @param subscriptionName
     * @throws JSONException
     */
    public static void addNotificationToSubscription(String key, String value, String subscriptionName) throws JSONException {
        RestPostSubscriptionObject subscription = (RestPostSubscriptionObject) subscriptions.get(subscriptionName);
        subscription.addNotificationMessageKeyValue(key, value);
    }

    /**
     * Adds a condition to the default requirement in the subscription.
     *
     * @param jmesPath
     * @param subscriptionName
     * @throws JSONException
     */
    public static void addConditionToRequirement(String jmesPath, String subscriptionName) throws JSONException {
        RestPostSubscriptionObject subscription = (RestPostSubscriptionObject) subscriptions.get(subscriptionName);

        JSONObject condition = new JSONObject();
        condition.put("jmespath", jmesPath);
        subscription.addConditionToRequirement(0, condition);
    }

    /**
     * Adds a subscription in eiffel intelligence, if it is any subscription with the same subscriptionName in EI it will first be removed.
     *
     * @param subscriptionName
     * @param frontendBaseUrl
     * @param backendBaseUrl
     * @return
     * @throws JSONException
     * @throws ClientProtocolException
     * @throws URISyntaxException
     * @throws IOException
     */
    public static ResponseEntity sendSubscriptionToEiffelIntelligence(String subscriptionName, String frontendBaseUrl, String backendBaseUrl) throws JSONException, ClientProtocolException, URISyntaxException, IOException {
        deleteSubscription(subscriptionName, frontendBaseUrl, backendBaseUrl);

        RestPostSubscriptionObject subscription = (RestPostSubscriptionObject) subscriptions.get(subscriptionName);
        ResponseEntity response = new HttpRequest(HttpMethod.POST)
            .setBaseUrl(frontendBaseUrl)
            .setEndpoint("/subscriptions")
            .addHeader("Content-Type", "application/json")
            .addParam("backendurl", backendBaseUrl)
            .setBody(subscription.getAsSubscriptions().toString())
            .performRequest();

        return response;
    }

    /**
     * Deletes all created subscriptions from ei
     *
     * @param frontendBaseUrl
     * @param backendBaseUrl
     * @throws ClientProtocolException
     * @throws URISyntaxException
     * @throws IOException
     */
    public static void deleteSubscriptions(String frontendBaseUrl, String backendBaseUrl) throws ClientProtocolException, URISyntaxException, IOException {
        Iterator<String> keys = subscriptions.keys();

        while(keys.hasNext()) {
            String subscriptionName = keys.next();
            ResponseEntity response = deleteSubscription(subscriptionName, frontendBaseUrl, backendBaseUrl);

            if(response.getStatusCode() != 200) {
                LOGGER.error("Failed to remove subscription: \"" + subscriptionName+ "\" from EI. Response: " + response.getBody());
            }
        }
    }

    /**
     * Deletes a subscription from eiffel intelligence
     *
     * @param subscriptionName
     * @param frontendBaseUrl
     * @param backendBaseUrl
     * @throws ClientProtocolException
     * @throws URISyntaxException
     * @throws IOException
     */
    private static ResponseEntity deleteSubscription(String subscriptionName, String frontendBaseUrl, String backendBaseUrl) throws ClientProtocolException, URISyntaxException, IOException {
        ResponseEntity response = new HttpRequest(HttpMethod.DELETE)
                .setBaseUrl(frontendBaseUrl)
                .setEndpoint("/subscriptions/" + subscriptionName)
                .addParam("backendurl", backendBaseUrl)
                .performRequest();

        return response;
    }
}
