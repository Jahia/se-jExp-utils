package org.jahia.se.modules.utils.initializers.jExperience;

import com.ning.http.client.AsyncCompletionHandler;
import com.ning.http.client.AsyncHttpClient;
import com.ning.http.client.ListenableFuture;
import com.ning.http.client.Response;
import org.apache.unomi.api.Metadata;
import org.apache.unomi.api.PropertyType;
import org.jahia.modules.jexperience.admin.ContextServerService;
import org.jahia.services.content.decorator.JCRSiteNode;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.osgi.service.component.annotations.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class Utils {
    private static final Logger logger = LoggerFactory.getLogger(Utils.class);
    private ContextServerService contextServerService;
    private JCRSiteNode site;

    public Utils(JCRSiteNode site,ContextServerService contextServerService){
        this.site=site;
        this.contextServerService = contextServerService;
    }

    public HashMap<String,String> getPropertyNames(String cardName) {
        PropertyType[] profileProperties = getProfileProperties();
        return getPropertyNames(profileProperties,cardName);
    };

    public TreeSet<String> getCardNames() {
        PropertyType[] profileProperties = getProfileProperties();
        return getCardNames(profileProperties);
    };

    private PropertyType[] getProfileProperties() {
        try {
            PropertyType[] profileProperties = contextServerService.executeGetRequest(
                site.getSiteKey(),
                "/cxs/profiles/properties/targets/profiles",
                null,
                null,
                PropertyType[].class
            );

            return profileProperties;

        }catch (IOException e) {
            logger.error("Error happened", e);
        }
        return null;
    };

    private HashMap<String,String> getPropertyNames(PropertyType[] profileProperties, String cardName) {
        HashMap<String,String> propertyNames= new HashMap<String,String>();
        Arrays.stream(profileProperties).forEach(props -> {
            boolean cardNameMatch = cardName.equals(getCardName(props));
            boolean isMultivalued = props.isMultivalued() != null ? props.isMultivalued() : false;
            if(cardNameMatch){
                String propsType = props.getValueTypeId();
                String propsId = props.getMetadata().getId();
                String propsName = props.getMetadata().getName();

                propertyNames.put(propsId,propsName+" <"+propsType+">"+(isMultivalued?"*":""));
            }
        });

        return propertyNames;
    };

    private TreeSet<String> getCardNames(PropertyType[] profileProperties) {
        TreeSet<String> cardNames= new TreeSet<>(new Comparator<String>() {
            @Override
            public int compare(String s1, String s2) {
                return s1.trim().toUpperCase().compareTo(s2.trim().toUpperCase());
            }
        });

        Arrays.stream(profileProperties).forEach(props -> {
            String cardName = getCardName(props);
            if(!cardName.isEmpty())
                cardNames.add(cardName);
        });

        return cardNames;
    };

    private String getCardName(PropertyType props) {
        String cardName = "";
        String tags = props.getMetadata().getSystemTags().toString();
        Pattern pattern = Pattern.compile("cardDataTag/[^/]+/[^/]+/([^,\\]]+)");
        Matcher matcher = pattern.matcher(props.getMetadata().getSystemTags().toString());
        if (matcher.find())
            cardName = matcher.group(1);

        return cardName;
    };
}
