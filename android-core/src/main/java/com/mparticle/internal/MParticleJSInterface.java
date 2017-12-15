package com.mparticle.internal;


import android.webkit.JavascriptInterface;

import com.mparticle.MParticle;
import com.mparticle.MParticle.EventType;
import com.mparticle.commerce.CommerceEvent;
import com.mparticle.commerce.Impression;
import com.mparticle.commerce.Product;
import com.mparticle.commerce.Promotion;
import com.mparticle.commerce.TransactionAttributes;
import com.mparticle.identity.IdentityApiRequest;
import com.mparticle.identity.IdentityStateListener;
import com.mparticle.identity.MParticleUser;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Javascript interface to be used for {@code Webview} analytics.
 *
 * This class knows how to parse JSON that has been generated by the mParticle Javascript SDK, basically
 * creating a bridge between the two SDKs.
 *
 */
public class MParticleJSInterface {
    public static final String INTERFACE_NAME = "mParticleAndroid";

    //the following keys are sent from the JS library as a part of each event
    private static final String JS_KEY_EVENT_NAME = "EventName";
    private static final String JS_KEY_EVENT_CATEGORY = "EventCategory";
    private static final String JS_KEY_EVENT_ATTRIBUTES = "EventAttributes";
    private static final String JS_KEY_EVENT_DATATYPE = "EventDataType";
    private static final String JS_KEY_OPTOUT = "OptOut";

    private static final int JS_MSG_TYPE_SS = 1;
    private static final  int JS_MSG_TYPE_SE = 2;
    private static final int JS_MSG_TYPE_PV = 3;
    private static final int JS_MSG_TYPE_PE = 4;
    private static final int JS_MSG_TYPE_CR = 5;
    private static final int JS_MSG_TYPE_OO = 6;
    private static final int JS_MSG_TYPE_COMMERCE = 16;

    private static final String errorMsg = "Error processing JSON data from Webview: %s";

    private static final String EVENT_NAME = "EventName";
    private static final String CURRENCY_CODE = "CurrencyCode";
    private static final String CHECKOUT_STEP = "CheckoutStep";
    private static final String CHECKOUT_OPTIONS = "CheckoutOptions";
    private static final String PRODUCT_ACTION = "ProductAction";
    private static final String PRODUCT_LIST = "ProductList";
    private static final String PRODUCT_ACTION_TYPE = "ProductActionType";
    private static final String PROMOTION_ACTION = "PromotionAction";
    private static final String PROMOTION_LIST = "PromotionList";
    private static final String PROMOTION_ACTION_TYPE = "PromotionActionType";
    private static final String PRODUCT_IMPRESSIONS = "ProductImpressions";

    private static final String AFFILIATION = "Affiliation";
    private static final String TOTAL_AMOUNT = "TotalAmount";
    private static final String SHIPPING_AMOUNT = "ShippingAmount";
    private static final String TAX_AMOUNT = "TaxAmount";
    private static final String TRANSACTION_ID = "TransactionId";

    private static final String NAME = "Name";
    private static final String SKU = "Sku";
    private static final String PRICE = "Price";
    private static final String QUANTITY = "Quantity";
    private static final String BRAND = "Brand";
    private static final String VARIANT = "Variant";
    private static final String CATEGORY = "Category";
    private static final String POSITION = "Position";
    private static final String COUPON_CODE = "CouponCode";
    private static final String ATTRIBUTES = "Attributes";

    private static final String PROMOTION_ID = "Id";
    private static final String PROMOTION_NAME = "Name";
    private static final String PROMOTION_CREATIVE = "Creative";
    private static final String PROMOTION_POSITION = "Position";

    private static final String PRODUCT_IMPRESSION_NAME = "ProductImpressionList";

    private static final String USER_IDENTITIES = "UserIdentities";
    private static final String USER_IDENTITY = "UserIdentity";
    private static final String IDENTITY = "Identity";
    private static final String TYPE = "Type";

    public MParticleJSInterface() {
        Product.setEqualityComparator(new Product.EqualityComparator() {
            @Override
            public boolean equals(Product product1, Product product2) {
                if (product1.getSku() == null) {
                    return product2.getSku() == null;
                } else {
                    return product1.getSku().equals(product2.getSku());
                }
            }
        });
    }

    @JavascriptInterface
    public String getCurrentMpId() {
        MParticleUser user = MParticle.getInstance().Identity().getCurrentUser();
        if (user != null) {
            return String.valueOf(user.getId());
        } else {
            return String.valueOf(0);
        }
    }

    @JavascriptInterface
    public void login() {
        MParticle.getInstance().Identity().login();
    }

    @JavascriptInterface
    public void login(String json) {
        IdentityApiRequest request = null;
        try {
            JSONObject jsonObject = new JSONObject(json);
            request = getIdentityApiRequest(jsonObject);
        }
        catch (JSONException jse) {
            Logger.warning(String.format(errorMsg, jse.getMessage()));
        }
        MParticle.getInstance().Identity().login(request);
    }

    @JavascriptInterface
    public void logout() {
        MParticle.getInstance().Identity().logout();
    }


    @JavascriptInterface
    public void logout(String json) {
        IdentityApiRequest request = null;
        try {
            JSONObject jsonObject = new JSONObject(json);
            request = getIdentityApiRequest(jsonObject);
        }
        catch (JSONException jse) {
            Logger.warning(String.format(errorMsg, jse.getMessage()));
        }
        MParticle.getInstance().Identity().logout(request);
    }

    @JavascriptInterface
    public void modify(String json) {
        IdentityApiRequest request = null;
        try {
            JSONObject jsonObject = new JSONObject(json);
            request = getIdentityApiRequest(jsonObject);
        }
        catch (JSONException jse) {
            Logger.warning(String.format(errorMsg, jse.getMessage()));
        }
        MParticle.getInstance().Identity().modify(request);
    }

    @JavascriptInterface
    public void logEvent(String json) {
        try {
            JSONObject event = new JSONObject(json);

            String name = event.getString(JS_KEY_EVENT_NAME);
            EventType eventType = convertEventType(event.getInt(JS_KEY_EVENT_CATEGORY));
            Map<String, String> eventAttributes = convertToMap(event.optJSONObject(JS_KEY_EVENT_ATTRIBUTES));

            int messageType = event.getInt(JS_KEY_EVENT_DATATYPE);
            switch (messageType){
                case JS_MSG_TYPE_PE:
                    MParticle.getInstance().logEvent(name,
                            eventType,
                            eventAttributes);
                    break;
                case JS_MSG_TYPE_PV:
                    MParticle.getInstance().logScreen(name,
                            eventAttributes);
                    break;
                case JS_MSG_TYPE_OO:
                    MParticle.getInstance().setOptOut(event.optBoolean(JS_KEY_OPTOUT));
                    break;
                case JS_MSG_TYPE_CR:
                    MParticle.getInstance().logError(name, eventAttributes);
                    break;
                case JS_MSG_TYPE_COMMERCE:
                    CommerceEvent commerceEvent = toCommerceEvent(event);
                    if (commerceEvent == null) {
                        Logger.warning("CommerceEvent empty, or unparseable");
                        break;
                    }
                    MParticle.getInstance().logEvent(commerceEvent);
                    break;
                case JS_MSG_TYPE_SE:
                case JS_MSG_TYPE_SS:
                    //swallow session start and end events, the native SDK will handle those.
                default:

            }

        } catch (JSONException jse) {
            Logger.warning(String.format(errorMsg, jse.getMessage()));
        }
    }

    @JavascriptInterface
    public void setUserTag(String json) {
        try {
            final JSONObject attribute = new JSONObject(json);
            final String key = attribute.getString("key");
            if (MParticle.getInstance().Identity().getCurrentUser() != null) {
                MParticle.getInstance().Identity().getCurrentUser().setUserTag(key);
            } else {
                MParticle.getInstance().Identity().addIdentityStateListener(new SingleUserIdentificationCallback() {
                    @Override
                    public void onUserFound(MParticleUser user) {
                        user.setUserTag(key);
                    }
                });
            }
        } catch (JSONException jse) {
            Logger.warning(String.format(errorMsg, jse.getMessage()));
        }
    }

    @JavascriptInterface
    public void removeUserTag(String json){
        try{
            JSONObject attribute = new JSONObject(json);
            final String key = attribute.getString("key");
            if (MParticle.getInstance().Identity().getCurrentUser() != null) {
                MParticle.getInstance().Identity().getCurrentUser().removeUserAttribute(key);
            } else {
                MParticle.getInstance().Identity().addIdentityStateListener(new SingleUserIdentificationCallback() {
                    @Override
                    public void onUserFound(MParticleUser user) {
                        user.removeUserAttribute(key);
                    }
                });
            }
        }catch (JSONException jse){
            Logger.warning(String.format(errorMsg, jse.getMessage()));
        }
    }

    @JavascriptInterface
    public void setUserAttribute(String json){
        try {
            JSONObject attribute = new JSONObject(json);
            final String key = attribute.getString("key");
            final Object value = attribute.get("value");
            if (MParticle.getInstance().Identity().getCurrentUser() != null) {
                MParticle.getInstance().Identity().getCurrentUser().setUserAttribute(key, String.valueOf(value));
            } else {
                MParticle.getInstance().Identity().addIdentityStateListener(new SingleUserIdentificationCallback() {
                    @Override
                    public void onUserFound(MParticleUser user) {
                        user.setUserAttribute(key, String.valueOf(value));
                    }
                });
            }
        } catch (JSONException jse) {
            Logger.warning(String.format(errorMsg, jse.getMessage()));
        }
    }

    @JavascriptInterface
    public void removeUserAttribute(String json){
        try{
            JSONObject attribute = new JSONObject(json);
            final String key = attribute.getString("key");
            if (MParticle.getInstance().Identity().getCurrentUser() != null) {
                MParticle.getInstance().Identity().getCurrentUser().removeUserAttribute(key);
            } else {
                MParticle.getInstance().Identity().addIdentityStateListener(new SingleUserIdentificationCallback() {
                    @Override
                    public void onUserFound(MParticleUser user) {
                        user.removeUserAttribute(key);
                    }
                });
            }
        }catch (JSONException jse){
            Logger.warning(String.format(errorMsg, jse.getMessage()));
        }
    }

    @JavascriptInterface
    public void setSessionAttribute(String json){
        try {
            JSONObject attribute = new JSONObject(json);
            MParticle.getInstance().setSessionAttribute(attribute.getString("key"), attribute.getString("value"));
        } catch (JSONException jse) {
            Logger.warning(String.format(errorMsg, jse.getMessage()));
        }
    }

    @JavascriptInterface
    public void setUserIdentity(String json){
        //do nothing
    }

    @JavascriptInterface
    public void removeUserIdentity(String json){
       // do nothing
    }

    @JavascriptInterface
    public void addToProductBag(String productBagName, String json){
        try {
            JSONObject attribute = new JSONObject(json);
            Product product = toProduct(attribute);
            if (product != null) {
                MParticle.getInstance().ProductBags().addProduct(productBagName, product);
            } else {
                Logger.warning(String.format(errorMsg, "unable to convert String to Product: " + json));
            }
        } catch (JSONException jse) {
            Logger.warning(String.format(errorMsg, jse.getMessage()));
        }
    }

    @JavascriptInterface
    public boolean removeFromProductBag(String productBagName, String json) {
        try {
            JSONObject attribute = new JSONObject(json);
            Product product = toProduct(attribute);
            if (product != null) {
                return MParticle.getInstance().ProductBags().removeProduct(productBagName, product);
            } else {
                Logger.warning(String.format(errorMsg, "unable to convert String to Product: " + json));
            }
        } catch (JSONException jse) {
            Logger.warning(String.format(errorMsg, jse.getMessage()));
        }
        return false;
    }

    @JavascriptInterface
    public void clearProductBag(String productBagName) {
        MParticle.getInstance().ProductBags().clearProductBag(productBagName);
    }

    @JavascriptInterface
    public void addToCart(String json) {
        try {
            JSONObject attribute = new JSONObject(json);
            Product product = toProduct(attribute);
            if (product != null) {
                MParticleUser user = MParticle.getInstance().Identity().getCurrentUser();
                if (user != null) {
                    user.getCart().add(product);
                } else {
                    Logger.warning("Unable to add product to cart - no mParticle user identified.");
                }
            } else {
                Logger.warning(String.format(errorMsg, "unable to convert String to Product: " + json));
            }
        } catch (JSONException jse) {
            Logger.warning(String.format(errorMsg, jse.getMessage()));
        }
    }

    @JavascriptInterface
    public void removeFromCart(String json) {
        try {
            JSONObject attribute = new JSONObject(json);
            Product product = toProduct(attribute);
            if (product != null) {
                MParticleUser user = MParticle.getInstance().Identity().getCurrentUser();
                if (user != null) {
                    user.getCart().remove(product);
                } else {
                    Logger.warning("Unable to remove product from cart - no mParticle user identified.");
                }
            } else {
                Logger.warning(String.format(errorMsg, "unable to convert String to Product: " + json));
            }
        } catch (JSONException jse) {
            Logger.warning(String.format(errorMsg, jse.getMessage()));
        }
    }

    @JavascriptInterface
    public void clearCart() {
        MParticleUser user = MParticle.getInstance().Identity().getCurrentUser();
        if (user != null) {
            user.getCart().clear();
        } else {
            Logger.warning("Unable to clear cart - no mParticle user identified.");
        }
    }

    @JavascriptInterface
    public void setUserAttributeList(String json) {
        try {
            JSONObject jsonObject = new JSONObject(json);
            final String key = jsonObject.getString("key");
            JSONArray value = jsonObject.getJSONArray("value");
            final List<String> attributes = new ArrayList<String>();
            for (int i = 0; i < value.length(); i++) {
                attributes.add(String.valueOf(value.get(i)));
            }
            if (MParticle.getInstance().Identity().getCurrentUser() != null) {
                MParticle.getInstance().Identity().getCurrentUser().setUserAttributeList(key, attributes);
            } else {
                MParticle.getInstance().Identity().addIdentityStateListener(new SingleUserIdentificationCallback() {
                    @Override
                    public void onUserFound(MParticleUser user) {
                        user.setUserAttributeList(key, attributes);
                    }
                });
            }
        } catch (JSONException jse) {
            Logger.warning(String.format(errorMsg, jse.getMessage()));
        }
    }

    @JavascriptInterface
    public void removeAllUserAttributes() {
        if (MParticle.getInstance().Identity().getCurrentUser() != null) {
            MParticleUser user = MParticle.getInstance().Identity().getCurrentUser();
            for (final String userAttribute : user.getUserAttributes().keySet()) {
                user.removeUserAttribute(userAttribute);
            }
        } else {
            MParticle.getInstance().Identity().addIdentityStateListener(new SingleUserIdentificationCallback() {
                @Override
                public void onUserFound(MParticleUser user) {
                    for (final String userAttribute : user.getUserAttributes().keySet()) {
                        user.removeUserAttribute(userAttribute);
                    }
                }
            });
        }
    }

    @JavascriptInterface
    public String getUserAttributesLists() {
        final Map<String, List> attributeMap = new HashMap<String, List>();
        //TODO
        //we need to implement an Asynchronous version of this method once we get a callback scheme in
        //place across platforms
        MParticleUser user = MParticle.getInstance().Identity().getCurrentUser();
        if (user == null) {
            return new JSONObject().toString();
        }
        for (Map.Entry<String, Object> entry: user.getUserAttributes().entrySet()) {
            if (entry.getValue() instanceof List) {
                attributeMap.put(entry.getKey(), (List)entry.getValue());
            }
        }
        JSONArray jsonArray = new JSONArray();
        for (Map.Entry<String, List> entry: attributeMap.entrySet()) {
            try {
                JSONObject jsonObject = new JSONObject();
                jsonObject.put("key", entry.getKey());
                JSONArray jsonArray1 = new JSONArray();
                for (Object attribute: entry.getValue()) {
                    jsonArray1.put(attribute);
                }
                jsonObject.put("value", jsonArray1.toString());
                jsonArray.put(jsonObject);
            } catch (JSONException jse) {
                Logger.warning(jse.getMessage());
            }
        }
        return jsonArray.toString();
    }

    @JavascriptInterface
    public String getAllUserAttributes() {
        //TODO
        //we need to implement an Asynchronous version of this method once we get a callback scheme in
        //place across platforms
        Map<String, Object> attributeMap = MParticle.getInstance().Identity().getCurrentUser().getUserAttributes();
        JSONArray jsonArray = new JSONArray();
        for (Map.Entry<String, Object> entry: attributeMap.entrySet()) {
            try {
                JSONObject jsonObject = new JSONObject();
                jsonObject.put("key", entry.getKey());
                jsonObject.put("value", entry.getValue().toString());
                jsonArray.put(jsonObject);
            } catch (JSONException jse) {
                Logger.warning(jse.getMessage());
            }
        }
        return jsonArray.toString();
    }

    private static Map<String, String> convertToMap(JSONObject attributes) {
        if (null != attributes) {
            Iterator keys = attributes.keys();

            Map<String, String> parsedAttributes = new HashMap<String, String>();

            while (keys.hasNext()) {
                String key = (String) keys.next();
                try {
                    parsedAttributes.put(key, attributes.getString(key));
                } catch (JSONException e) {
                    Logger.warning("Could not parse event attribute value");
                }
            }

            return parsedAttributes;
        }

        return null;
    }

    private EventType convertEventType(int eventType) {
        switch (eventType) {
            case 1:
                return EventType.Navigation;
            case 2:
                return EventType.Location;
            case 3:
                return EventType.Search;
            case 4:
                return EventType.Transaction;
            case 5:
                return EventType.UserContent;
            case 6:
                return EventType.UserPreference;
            case 7:
                return EventType.Social;
            default:
                return EventType.Other;
        }
    }

    protected CommerceEvent toCommerceEvent(JSONObject jsonObject) {
        CommerceEvent.Builder builder = null;

        //try to instantiate CommerceEvent with a Product and add Products
        JSONObject productActionObj = jsonObject.optJSONObject(PRODUCT_ACTION);
        if (productActionObj != null) {
            String productAction = productActionObj.optString(PRODUCT_ACTION_TYPE);
            JSONArray productArray = productActionObj.optJSONArray(PRODUCT_LIST);
            if (productAction != null && productArray != null) {
                for (int i = 0; i < productArray.length(); i++) {
                    Product product = toProduct(productArray.optJSONObject(i));
                        if (builder == null) {
                            builder = new CommerceEvent.Builder(productAction, product);
                        } else {
                            builder.addProduct(product);
                        }
                }
                if (builder == null) {
                    builder = new CommerceEvent.Builder(productAction, (Product)null);
                }
                TransactionAttributes transactionAttributes = getTransactionAttributes(productActionObj);
                if (transactionAttributes != null) {
                    builder.transactionAttributes(transactionAttributes);
                }
                builder.checkoutStep(productActionObj.optInt(CHECKOUT_STEP));
                builder.checkoutOptions(productActionObj.optString(CHECKOUT_OPTIONS));
            }
        }

        //try and instantiate CommerceEvent with a Promotion or add Promotions
        JSONObject promotionActionObj = jsonObject.optJSONObject(PROMOTION_ACTION);
        if (promotionActionObj != null) {
            String promotionAction = promotionActionObj.optString(PROMOTION_ACTION_TYPE);
            JSONArray promotionArray = promotionActionObj.optJSONArray(PROMOTION_LIST);
            if (promotionAction != null && promotionArray != null) {
                for (int i = 0; i < promotionArray.length(); i++) {
                    Promotion promotion = toPromotion(promotionArray.optJSONObject(i));
                        if (builder == null) {
                            builder = new CommerceEvent.Builder(promotionAction, promotion);
                        } else {
                            builder.addPromotion(promotion);
                        }
                }
                if (builder == null) {
                    builder = new CommerceEvent.Builder(promotionAction, (Promotion)null);
                }
            }
        }

        //try and instantiate CommerceEvent with an Impression or add Impressions
        JSONArray impressionList = jsonObject.optJSONArray(PRODUCT_IMPRESSIONS);
        if (impressionList != null) {
            for (int i = 0; i < impressionList.length(); i++) {
                Impression impression = toImpression(impressionList.optJSONObject(i));
                if (impression != null) {
                    if (builder == null) {
                        builder = new CommerceEvent.Builder(impression);
                    } else {
                        builder.addImpression(impression);
                    }
                }
            }
        }
        if (builder == null) {
            return null;
        }
        Map<String, String> customAttributes = getCustomAttributes(jsonObject);
        if (customAttributes != null) {
            builder.customAttributes(customAttributes);
        }
        builder.currency(jsonObject.optString(CURRENCY_CODE, null));
        builder.internalEventName(jsonObject.optString(EVENT_NAME));
        return builder.build();
    }
    
    private TransactionAttributes getTransactionAttributes(JSONObject jsonObject) {
        TransactionAttributes attributes = null;
        if (jsonObject != null &&
                (jsonObject.has(TRANSACTION_ID) ||
                jsonObject.has(AFFILIATION) ||
                jsonObject.has(COUPON_CODE) ||
                jsonObject.has(TOTAL_AMOUNT) ||
                jsonObject.has(TAX_AMOUNT) ||
                jsonObject.has(SHIPPING_AMOUNT))) {
            attributes = new TransactionAttributes();
            attributes
                    .setId(jsonObject.optString(TRANSACTION_ID, attributes.getId()))
                    .setAffiliation(jsonObject.optString(AFFILIATION, attributes.getAffiliation()))
                    .setCouponCode(jsonObject.optString(COUPON_CODE, attributes.getCouponCode()))
                    .setRevenue(jsonObject.optDouble(TOTAL_AMOUNT))
                    .setTax(jsonObject.optDouble(TAX_AMOUNT))
                    .setShipping(jsonObject.optDouble(SHIPPING_AMOUNT));
        }
        return attributes;
    }

    Product toProduct(JSONObject jsonObject) {
        if (jsonObject == null) { return null; }
        try {
            Product.Builder builder = new Product.Builder(jsonObject.getString(NAME), jsonObject.optString(SKU, null), jsonObject.optDouble(PRICE, 0));
            builder.category(jsonObject.optString(CATEGORY, null));
            builder.couponCode(jsonObject.optString(COUPON_CODE, null));
            if (jsonObject.has(POSITION)) {
                builder.position(jsonObject.optInt(POSITION, 0));
            }
            if (jsonObject.has(QUANTITY)) {
                builder.quantity(jsonObject.optDouble(QUANTITY, 1));
            }
            builder.brand(jsonObject.optString(BRAND, null));
            builder.variant(jsonObject.optString(VARIANT, null));
            Map<String, String> customAttributes = getCustomAttributes(jsonObject);
            if (customAttributes != null) {
                builder.customAttributes(customAttributes);
            }
            return builder.build();
        }
        catch (JSONException ignore) {
            return null;
        }
    }

    private Map<String, String> getCustomAttributes(JSONObject jsonObject) {
        JSONObject attributesJson = jsonObject.optJSONObject(ATTRIBUTES);
        if (attributesJson != null) {
            if (attributesJson.length() > 0) {
                Map<String, String> customAttributes = new HashMap<String, String>();
                Iterator<String> keys = attributesJson.keys();

                while (keys.hasNext()) {
                    String key = keys.next();
                    customAttributes.put(key, attributesJson.optString(key));
                }
                return customAttributes;
            }
        }
        return null;
    }

    private Impression toImpression(JSONObject jsonObject) {
        Impression impression = null;
        if (jsonObject == null) { return impression; }
        if (jsonObject.has(PRODUCT_IMPRESSION_NAME)) {
            JSONArray jsonArray = jsonObject.optJSONArray(PRODUCT_LIST);
            if (jsonArray != null) {
                for (int i = 0; i < jsonArray.length(); i++) {
                    Product product = toProduct(jsonArray.optJSONObject(i));
                    String impressionName = jsonObject.optString(PRODUCT_IMPRESSION_NAME, null);
                    if (product != null) {
                        if (impression == null) {
                            if (!MPUtility.isEmpty(impressionName))
                                impression = new Impression(impressionName, product);
                        } else {
                            impression.addProduct(product);
                        }
                    }
                }
            }
        }
        return impression;
    }
    
    private Promotion toPromotion(JSONObject jsonObject) {
        if (jsonObject == null) {
            return null;
        }
        return new Promotion()
                .setName(jsonObject.optString(PROMOTION_NAME))
                .setCreative(jsonObject.optString(PROMOTION_CREATIVE))
                .setId(jsonObject.optString(PROMOTION_ID))
                .setPosition(jsonObject.optString(PROMOTION_POSITION));
    }

    private IdentityApiRequest getIdentityApiRequest(JSONObject jsonObject) {
        JSONArray identitiesArray = jsonObject.optJSONArray(USER_IDENTITIES);

        Map<MParticle.IdentityType, String> identities = new HashMap<MParticle.IdentityType, String>();
        if (identitiesArray != null) {
            for (int i = 0; i < identitiesArray.length(); i++) {
                try {
                    JSONObject object = identitiesArray.getJSONObject(i);
                    identities.put(getIdentityType(object), object.getString(IDENTITY));
                }
                catch (JSONException jse){
                    Logger.warning(String.format(errorMsg, jse.getMessage()));
                }
            }
        }
        IdentityApiRequest.Builder builder = IdentityApiRequest.withEmptyUser()
                .userIdentities(identities);

        if (jsonObject.has(TYPE) && jsonObject.has(IDENTITY)) {
            MParticle.IdentityType type = getIdentityType(jsonObject);
            String value = jsonObject.optString(IDENTITY);
            if (type != null && !MPUtility.isEmpty(value)) {
                builder.userIdentity(type, value);
            }
        }
        return builder.build();
    }

    private MParticle.IdentityType getIdentityType(JSONObject object) {
        MParticle.IdentityType identityType = null;
        String previousErrorMessage = null;

        try {
            identityType = MParticle.IdentityType.parseInt(object.getInt(TYPE));
        }
        catch (JSONException jse) {
            previousErrorMessage = jse.getMessage();
        }
        if (identityType != null) {
            return identityType;
        }
        try {
            identityType = MParticle.IdentityType.valueOf(object.getString(TYPE));
        }
        catch (JSONException jse) {
            Logger.warning(String.format(errorMsg, (jse.getMessage() + (!MPUtility.isEmpty(previousErrorMessage) ? "\n" + previousErrorMessage : ""))));
        }
        return identityType;
    }

    abstract class SingleUserIdentificationCallback implements IdentityStateListener {

        @Override
        public void onUserIdentified(MParticleUser user) {
            MParticle.getInstance().Identity().removeIdentityStateListener(this);
            onUserFound(user);
        }

        abstract void onUserFound(MParticleUser user);

    }
}
