package org.jfrog.bamboo.builder;

import com.atlassian.bamboo.configuration.AdministrationConfiguration;
import com.atlassian.bamboo.plan.PlanManager;
import com.atlassian.bamboo.utils.EscapeChars;
import com.atlassian.bamboo.variable.VariableDefinition;
import com.atlassian.bamboo.variable.VariableDefinitionManager;
import com.google.common.collect.Maps;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.io.IOUtils;
import org.codehaus.jackson.JsonFactory;
import org.codehaus.jackson.JsonParser;
import org.codehaus.jackson.map.ObjectMapper;
import org.jfrog.bamboo.util.ConstantValues;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.jfrog.bamboo.util.ConstantValues.ADMIN_CONFIG_SERVLET_CONTEXT_NAME;

/**
 * This class contains utilities and APIs.
 */
public class BambooUtilsHelper {
    private PlanManager planManager;
    private VariableDefinitionManager variableDefinitionManager;
    private AdministrationConfiguration administrationConfiguration;
    private HttpClient httpClient = new HttpClient();

    public Map<String, String> getAllVariables(String planKey) {
        HashMap<String, String> params = Maps.newHashMap();
        params.put(ConstantValues.PLAN_KEY_PARAM, planKey);
        String requestUrl = prepareRequestUrl(ADMIN_CONFIG_SERVLET_CONTEXT_NAME, params);
        GetMethod getMethod = new GetMethod(requestUrl);
        InputStream responseStream = null;
        try {
            executeMethod(requestUrl, getMethod);

            JsonFactory jsonFactory = new JsonFactory();
            ObjectMapper mapper = new ObjectMapper();
            jsonFactory.setCodec(mapper);

            responseStream = getMethod.getResponseBodyAsStream();
            if (responseStream == null) {
                return Maps.newHashMap();
            }

            JsonParser parser = jsonFactory.createJsonParser(responseStream);
            return parser.readValueAs(Map.class);
        } catch (IOException ioe) {
            return Maps.newHashMap();
        } finally {
            getMethod.releaseConnection();
            IOUtils.closeQuietly(responseStream);
        }
    }

    private String prepareRequestUrl(String servletName, Map<String, String> params) {
        String bambooBaseUrl = administrationConfiguration.getBaseUrl();
        StringBuilder builder = new StringBuilder(bambooBaseUrl);
        if (!bambooBaseUrl.endsWith("/")) {
            builder.append("/");
        }
        StringBuilder requestUrlBuilder = builder.append("plugins/servlet/").append(servletName);
        if (params.size() != 0) {
            requestUrlBuilder.append("?");

            for (Map.Entry<String, String> param : params.entrySet()) {
                if (!requestUrlBuilder.toString().endsWith("?")) {
                    requestUrlBuilder.append("&");
                }
                requestUrlBuilder.append(param.getKey()).append("=").append(EscapeChars.forURL(param.getValue()));
            }
        }

        return requestUrlBuilder.toString();
    }

    /**
     * Executes the given HTTP method
     *
     * @param requestUrl Full request URL
     * @param getMethod  HTTP GET method
     */
    private void executeMethod(String requestUrl, GetMethod getMethod) throws IOException {
        int responseCode = httpClient.executeMethod(getMethod);
        if (responseCode == HttpStatus.SC_NOT_FOUND) {
            throw new IOException("Unable to find requested resource: " + requestUrl);
        } else if (responseCode != HttpStatus.SC_OK) {
            throw new IOException("Failed to retrieve requested resource: " + requestUrl + ". Response code: " +
                    responseCode + ", Message: " + getMethod.getStatusText());
        }
    }

    private void appendVariableDefs(Map<String, String> globalVariableMap, List<VariableDefinition> globalVariables) {
        for (VariableDefinition variableDefinition : globalVariables) {
            globalVariableMap.put(variableDefinition.getKey(), variableDefinition.getValue());
        }
    }

    public void setPlanManager(PlanManager planManager) {
        this.planManager = planManager;
    }

    public void setVariableDefinitionManager(VariableDefinitionManager variableDefinitionManager) {
        this.variableDefinitionManager = variableDefinitionManager;
    }

    public void setAdministrationConfiguration(AdministrationConfiguration administrationConfiguration) {
        this.administrationConfiguration = administrationConfiguration;
    }
}