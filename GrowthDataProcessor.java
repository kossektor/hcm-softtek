package com.ihg.hcm.growth;

import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Map;

import com.ihg.hotel.Hotel;
import com.ihg.hotel.HotelProfile;
import com.ihg.hotel.RetrieveHotelProfileRequest;
import com.ihg.hotel.RetrieveHotelProfileResponse;
import com.ihg.hotel.contact.HotelContactList;
import com.ihg.hotel.contact.HotelContactService;
import com.ihg.hotel.contact.RetrieveHotelContacts;
import com.ihg.hotel.contact.RetrieveHotelContactsRequest;
import com.ihg.hotel.contact.RetrieveHotelContactsResponse;
import org.apache.log4j.Logger;
import org.apache.xmlbeans.XmlObject;

import com.ihg.hcm.content.ContentDataConstants;
import com.ihg.hcm.growth.client.GrowthRestClient;
import com.ihg.hcm.growth.schemas.HolidexCodeListType;
import com.ihg.hcm.growth.schemas.HotelContactListType;
import com.ihg.hcm.growth.schemas.HotelContactType;
import com.ihg.hcm.importer.util.DataImportUtil;
import com.ihg.hcm.model.AttributeQueryTerm;
import com.ihg.hcm.model.AttributeSpecNotFoundException;
import com.ihg.hcm.model.Content;
import com.ihg.hcm.model.HotelAttribute;
import com.ihg.hcm.model.HotelAttributeDAO;
import com.ihg.hcm.model.HotelDAO;
import com.ihg.hcm.model.HotelImpl;
import com.ihg.hcm.model.HotelService;
import com.ihg.hcm.model.RoleType;
import com.ihg.hcm.model.SaveProcessor;
import com.ihg.hcm.model.User;
import com.ihg.hcm.model.UserDAO;
import com.ihg.hcm.workflow.ActionItem;
import com.ihg.hcm.workflow.ActionItemService;

public class GrowthDataProcessor implements GrowthImport {

    static Logger logger = Logger.getLogger(GrowthDataProcessor.class.getName());

    private GrowthRestClient growthClient;
    private Hotel hotelProfileService;
    private HotelContactService hotelContactService;
    private GrowthContentBuilder contentBuilder;
    private GrowthRestContentBuilder growthRestContentBuilder;
    private SaveProcessor saveProcessor;
    private String growthRestContentServiceURL;
    private String growthRestContactServiceURL;
    private HotelService assetService;
    private String hostname;
    private int retryGrowthService = 3;
    private int retryGrowthSerivceInterval = 30;

    private ActionItemService actionItemService;

    // private String growthNHOPHolidexCodesServiceURL;

    private String growthHcmHolidexCodesServiceURL;

    private UserDAO userDAO;
    private HotelAttributeDAO assetAttributeDAO;
    private HotelDAO assetDAO;
    /** the constants object */
    private Map<String, String> hotelContactRoleMap;
    private static final String DEFAULT_PASSWORD = "hcm"; // You won't be able

    private boolean useGrowthHotelProfileService = false;

    // to log in with this
    // because it's not
    // MD5 hashed

    public void setGrowthClient(GrowthRestClient growthClient) {
        this.growthClient = growthClient;
    }

    public void setHotelProfileService(Hotel hotelProfileService) {
        this.hotelProfileService = hotelProfileService;
    }

    public void setHotelContactService(HotelContactService hotelContactService) {
        this.hotelContactService = hotelContactService;
    }

    public void setActionItemService(ActionItemService actionItemService) {
        this.actionItemService = actionItemService;
    }

    public ActionItemService getActionItemService() {
        return this.actionItemService;
    }

    public GrowthContentBuilder getContentBuilder() {
        return contentBuilder;
    }

    public void setContentBuilder(GrowthContentBuilder contentBuilder) {
        this.contentBuilder = contentBuilder;
    }

    public void setGrowthRestContentBuilder(GrowthRestContentBuilder growthRestContentBuilder) {
        this.growthRestContentBuilder = growthRestContentBuilder;
    }

    public SaveProcessor getSaveProcessor() {
        return saveProcessor;
    }

    public void setSaveProcessor(SaveProcessor saveProcessor) {
        this.saveProcessor = saveProcessor;
    }

    public HotelService getHotelService() {
        return assetService;
    }

    public void setHotelService(HotelService assetService) {
        this.assetService = assetService;
    }

    public String getHostname() {
        return hostname;
    }

    public void setHostname(String hostname) {
        this.hostname = hostname;
    }

    public void setGrowthRestContentServiceURL(String growthRestContentServiceURL) {
        this.growthRestContentServiceURL = growthRestContentServiceURL;
    }

    public void setGrowthRestContactServiceURL(String growthRestContactServiceURL) {
        this.growthRestContactServiceURL = growthRestContactServiceURL;
    }

    /**
     * @param growthNHOPHolidexCodesServiceURL
     *            the growthNHOPHolidexCodesServiceURL to set
     */
    // public void setGrowthNHOPHolidexCodesServiceURL(
    // String growthNHOPHolidexCodesServiceURL) {
    // this.growthNHOPHolidexCodesServiceURL = growthNHOPHolidexCodesServiceURL;
    // }

    public void setGrowthHcmHolidexCodesServiceURL(String growthHcmHolidexCodesServiceURL) {
        this.growthHcmHolidexCodesServiceURL = growthHcmHolidexCodesServiceURL;
    }

    public void setUserDAO(UserDAO userDAO) {
        this.userDAO = userDAO;
    }

    public void setHotelAttributeDAO(HotelAttributeDAO assetAttributeDAO) {
        this.assetAttributeDAO = assetAttributeDAO;
    }

    public void setHotelDAO(HotelDAO assetDAO) {
        this.assetDAO = assetDAO;
    }

    public void setUseGrowthHotelProfileService(boolean useGrowthHotelProfileService) {
        this.useGrowthHotelProfileService = useGrowthHotelProfileService;
    }

    protected void init() throws GrowthServiceException {
        // validate we have all required members
        if (growthClient == null) {
            throw new GrowthServiceException("GrowthRestClient not set on GrowthDataProcessor");
        }
        if (contentBuilder == null) {
            throw new GrowthServiceException("GrowthContentBuilder not set on GrowthDataProcessor");
        }
        if (saveProcessor == null) {
            throw new GrowthServiceException("SaveProcessor not set on GrowthDataProcessor");
        }
        if (assetService == null) {
            throw new GrowthServiceException("HotelService not set on GrowthDataProcessor");
        }
        if (userDAO == null) {
            throw new GrowthServiceException("UserDAO not set on GrowthDataProcessor");
        }
        hotelContactRoleMap = DataImportUtil.getHotelContactRoleMap();
    }

    /**
     * Processes importing the hotel data from Growth and saving the data to the
     * HCM database.
     * 
     * @param assetCodeList
     * 
     * @return DOCUMENT ME!
     * 
     * @throws GrowthServiceException
     */
    public boolean importHotel(String assetCode) throws GrowthServiceException {

        com.ihg.hcm.growth.schemas.Hotel hotel = null;
        HotelContactListType hotelContactList = null;
        HotelProfile hotelProfile = null;
        HotelContactList hotelProfileContacts = null;
        int locationNumber = 0;
        HotelImpl asset = null;
        Content content = null;
        boolean assetModified = false;
        boolean assetCreated = false;


        if (useGrowthHotelProfileService) {
            hotelProfile = loadHotelProfile(assetCode);
            hotelProfileContacts = loadHotelProfileContacts(hotelProfile);
            locationNumber = hotelProfile == null ? 0 : hotelProfile.getLocationNumber();
        } else {
            hotel = (com.ihg.hcm.growth.schemas.Hotel) growthClient.downloadHotelInfo(growthRestContentServiceURL, assetCode);
            hotelContactList = (HotelContactListType) growthClient.downloadHotelInfo(growthRestContactServiceURL, assetCode);
            locationNumber = hotel.getLocationNumber();
        }

        asset = assetService.retrieve(assetCode);

        try {
            logger.debug("::importHotel: assetCode = " + assetCode);

            if ((hotel != null || hotelProfile !=null) && locationNumber > 0) {
                if (asset == null) {

                    logger.debug("::importHotel: asset was null, creating new HotelImpl and setting asset code");

                    asset = new HotelImpl();
                    assetCreated = true;
                    asset.setCode(assetCode);
                }

                // Save growth status before hotel update
                String prevHotelStatusCode = asset.getGrowthStatus();

                logger.debug("::importHotel: invoking contentBuilder.updateHotel, use HPS=" + useGrowthHotelProfileService);
                if (useGrowthHotelProfileService) {
                    assetModified = contentBuilder.updateHotel(hotelProfile, asset);
                } else {
                    assetModified = growthRestContentBuilder.updateHotel(hotel, asset);
                }

                if (assetModified) {

                    logger.debug("::importHotel: asset was modified, invoking store on asset through DAO");
                    assetService.store(asset);
                }

                logger.debug("::importHotel: invoking contentBuilder.getContentObject");

                if (useGrowthHotelProfileService) {
                    content = contentBuilder.getContentObject(hotelProfile, hotelProfileContacts, assetCode, asset, prevHotelStatusCode);
                } else {
                    content = growthRestContentBuilder.getContentObject(hotel, hotelContactList, assetCode, asset, prevHotelStatusCode);
                }

                if (content.getHotelAttributes().size() > 0) {
                    try {
                        // [HCM-50] modified call to save to include the
                        // autopublish attribute logic
                        logger.debug("::importHotel: invoking save on saveProcessor");
                        saveProcessor.save(content, "GROWTH");
                        assetModified = true;
                    } catch (AttributeSpecNotFoundException nfe) {
                        // This will happen if an AA for a subattribute that has
                        // been removed still exists.
                        // We just ignore it now.
                        logger.debug("::importHotel: unhandled meta asset attribute not found exception, nfe = " + nfe.toString());
                    }
                }
                if ("NEW".equals(asset.getStatus()) && assetCreated) {
                    createNhopActionItems(asset);
                }
            } else {
                throw new GrowthServiceException("Invalid HotelImpl Code: " + assetCode);
            }
        } catch (Exception e) {
            if (e instanceof GrowthServiceException) {
                throw (GrowthServiceException) e;
            } else {
                logger.debug("::importHotel: unknown exception, e = " + e.toString());
                throw new GrowthServiceException("importHotel exception", e);
            }
        } catch (Throwable t) {
            logger.debug("::importHotel: unknown exception, e = " + t.toString());
            throw new GrowthServiceException("importHotel exception", t);
        }
        return assetModified;
    }

    private HotelProfile loadHotelProfile(String code) {
        HotelProfile hotelProfile = null;
        try {
            RetrieveHotelProfileRequest retrieveHotelProfileRequest = new RetrieveHotelProfileRequest();
            retrieveHotelProfileRequest.getHolidexCode().add(code);
            RetrieveHotelProfileResponse hotelProfileResponse = hotelProfileService.retrieveHotelProfile(retrieveHotelProfileRequest);
            hotelProfile = hotelProfileResponse.getHotel().isEmpty() ? null : hotelProfileResponse.getHotel().get(0);
        } catch (Exception e) {
            logger.error("Error loading hotel profile for code " + code, e);
        }
        return hotelProfile;
    }

    private HotelContactList loadHotelProfileContacts(HotelProfile profile) {
        HotelContactList hotelContactList = null;
        if (profile == null) {
            return null;
        }

        try {
            RetrieveHotelContacts retrieveHotelContacts = new RetrieveHotelContacts();
            RetrieveHotelContactsRequest retrieveHotelContactsRequest = new RetrieveHotelContactsRequest();
            retrieveHotelContacts.setRetrieveHotelContactsRequest(retrieveHotelContactsRequest);
            retrieveHotelContactsRequest.getHotelId().add("" + profile.getHotelID());
            retrieveHotelContactsRequest.getHotelRoleCode().addAll(DataImportUtil.getRoleCodeList());
            RetrieveHotelContactsResponse retrieveHotelContactsResponse = hotelContactService.retrieveHotelContacts(retrieveHotelContacts);
            hotelContactList = retrieveHotelContactsResponse.getHotelContactList();
        } catch (Exception e) {
            logger.error("Error loading hotel profile for id " + profile.getHotelID(), e);
        }
        return hotelContactList;
    }

    private String getUserRole(String role) {
        String assignedRole = hotelContactRoleMap.get(role);
        // RoleType definedRole = constants.getRoleType(assignedRole);
        return assignedRole;
    }

    /**
     * 
     * @param itemList
     * @param asset
     * @throws GrowthServiceException
     */
    private void updateNHOPContact(List<HotelContactType> itemList, HotelImpl asset) throws GrowthServiceException {
        String firstName = null;
        String lastName = null;
        String networkId = null;
        String role = null;
        boolean isAAChanged = false;
        String hotelStatus = asset.getStatus();
        if (hotelStatus != null && "NEW".equals(hotelStatus)) {
            HotelAttribute mcParent = new HotelAttribute();
            HotelAttribute currentMcParent = null;
            currentMcParent = assetAttributeDAO.findByNumber(asset, new Integer(ContentDataConstants.NHOP_MANAGER_COMPOUND));
            mcParent.setAttributeNumber(new Integer(ContentDataConstants.NHOP_MANAGER_COMPOUND));
            for (HotelContactType contact : itemList) {
                firstName = contact.getFirstName();
                lastName = contact.getLastName();
                networkId = contact.getNetworkId();
                role = contact.getRoleCode();
                if (getUserRole(role) != null && getUserRole(role).equals(RoleType.ROLE_NHOP_MANAGER)) {
                    logger.debug("Adding NHOP Contact to content");
                    User user = userDAO.retrieve(networkId);
                    HotelAttribute compound = new HotelAttribute();
                    compound.setAttributeNumber(new Integer(ContentDataConstants.NHOP_MANAGER_COMPOUND));
                    HotelAttribute name = new HotelAttribute();
                    name.setAttributeNumber(new Integer(ContentDataConstants.NHOP_MANAGER_NAME));
                    name.setValue("hcm:basicText-TextValue", firstName + " " + lastName);
                    compound.addSubattribute(name);
                    HotelAttribute email = new HotelAttribute();
                    email.setAttributeNumber(new Integer(ContentDataConstants.NHOP_MANAGER_EMAIL));
                    if (user != null) {
                        email.setValue("hcm:basicText-TextValue", user.getEmailAddress());
                    }
                    compound.addSubattribute(email);
                    logger.debug("Compound now has " + compound.getSubattributes().size() + " subattributes");
                    mcParent.addInstance(compound);
                }
            }
            mcParent.setHotel(asset);
            try {
                if (currentMcParent != null) {
                    mcParent.markChanges(currentMcParent);
                    isAAChanged = mcParent.isChanged();
                }
                if (isAAChanged) {
                    logger.debug("Saving #" + mcParent.getAttributeNumber() + " with " + mcParent.getInstances().size() + " instances");
                    if (logger.isDebugEnabled()) {
                        logger.debug("Full attribute:\n" + mcParent);
                    }
                    saveProcessor.save(mcParent, "GROWTH Contact Import");
                }
            } catch (AttributeSpecNotFoundException maanfe) {
                logger.debug("Cannot find the MAA for asset: " + asset.getCode(), maanfe);
            } catch (Exception e) {
                logger.debug("Error saving NHOP contact for Hotel: " + asset.getCode(), e);
                throw new GrowthServiceException(e);
            }
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ihg.hcm.growth.GrowthImport#getNewHotels()
     */
    // public List<String> getHotelCodesFromGrowth() throws
    // GrowthServiceException {
    // List<String> hotelCodes = new ArrayList<String>();
    // HolidexCodeListType hldxCodeList = null;
    // XmlObject object =
    // growthClient.downloadHotelInfo(growthNHOPHolidexCodesServiceURL, null);
    // if (object != null && object instanceof HolidexCodeListType) {
    // hldxCodeList = (HolidexCodeListType) object;
    // List<String> itemList = hldxCodeList.getHolidexCodeList();
    // for (String holidexCode: itemList) {
    // if (holidexCode != null) {
    // hotelCodes.add(holidexCode);
    // }
    // }
    // } else {
    // logger.debug("HotelStatusCodeListType is not the root");
    // }
    // return hotelCodes;
    // }

    public List<String> getUpdatedGrowthHotels(String fromDate) throws UnknownHostException, GrowthServiceException {
        List<String> hotelCodes = new ArrayList<String>();
        HolidexCodeListType hldxCodeList = null;
        XmlObject object = growthClient.downloadHotelInfo(growthHcmHolidexCodesServiceURL, fromDate);
        if (object != null && object instanceof HolidexCodeListType) {
            hldxCodeList = (HolidexCodeListType) object;
            List<String> itemList = hldxCodeList.getHolidexCodeList();
            for (String holidexCode : itemList) {
                if (holidexCode != null) {
                    hotelCodes.add(holidexCode);
                }
            }
        } else {
            logger.debug("HotelStatusCodeListType is not the root");
        }
        return hotelCodes;
    }

    /**
     * A utility method which gets all the hotels from HCM and updated hotels in
     * the last one month from Growth and merges both the lists to a unique set.
     * 
     * @return
     * @throws GrowthServiceException
     */
    public List<String> getUpdatedHotelCodeList() throws GrowthServiceException {
        List<String> hcmHotelCodes = assetDAO.findHotelCodes();

        int retryTimes = 0;
        List<String> updatedGrowthHotels = new ArrayList<String>();
        while (retryTimes < this.retryGrowthService) {
            retryTimes++;
            try {
                updatedGrowthHotels = getUpdatedGrowthHotels(DataImportUtil.getOneMonthPriorDate());
                break;
            } catch (UnknownHostException uhe) {
                logger.warn(uhe.getMessage());
                break;
            } catch (Exception ex) {
                if (retryTimes == this.retryGrowthService) {
                    throw new GrowthServiceException(ex);
                } else {
                    try {
                        Thread.sleep(this.retryGrowthSerivceInterval * 60 * 1000);
                    } catch (Exception ex2) {
                        throw new GrowthServiceException(ex2);
                    }
                }
            }
        }
        for (String hotelCode : updatedGrowthHotels) {
            if (!hcmHotelCodes.contains(hotelCode)) {
                hcmHotelCodes.add(hotelCode);
            }
        }
        return hcmHotelCodes;
    }

    /**
     * A utility method that checks the difference between current date and the
     * date hotel is opened. If the difference is more than 91 days it return
     * true.
     * 
     * @param hotel
     * @return
     */
    private boolean check91DayNHOPRule(com.ihg.hcm.growth.schemas.Hotel hotel) {
        Calendar dateOpened = hotel.getDateOpened();
        Calendar now = Calendar.getInstance();
        if (dateOpened != null) {
            long timeOpened = dateOpened.getTimeInMillis();
            long timeNow = now.getTimeInMillis();
            long daysBetween = (timeNow - timeOpened) / (1000 * 60 * 60 * 24);
            if (daysBetween > 91) {
                return true;
            }
        }
        return false;
    }

    private void createNhopActionItems(HotelImpl asset) throws Exception {
        if ("AMER".equals(asset.getLegacyRegion())) {
            createActionItem(
                    asset,
                    "Laying the Groundwork - Creating the HotelImpl Database",
                    "The pre-work you will complete prior to the Business Preparation Visit will be used <br>to build your hotel in applicable reservation channels, with the option to<br>pre-sell reservations.",
                    AMER_PREVISIT);
            createActionItem(
                    asset,
                    "Business Preparation Visit – Creating Inventory and Room Rates",
                    "The information in this stage is critical to your hotel’s successful pre-sales <br>and ramp up revenues. Your HotelImpl Opening Manager will facilitate your completion of this section during your on-site Business Preparation Visit<br>using a variety of tools and exercises.  Together you will also complete a few forms necessary to prepare for pre-sales.",
                    AMER_ON_SITE);
        } else if ("EMEA".equals(asset.getLegacyRegion())) {
            createActionItem(asset, "Action Item Group 1", "Action Item Group 1", EMEA_NHOP_1);
            createActionItem(asset, "Action Item Group 2", "Action Item Group 2", EMEA_NHOP_2);
        } else if ("APAC".equals(asset.getLegacyRegion())) {
            createActionItem(asset, "Action Item Group 1", "Action Item Group 1", APAC_NHOP_1);
            createActionItem(asset, "Action Item Group 2", "Action Item Group 2", APAC_NHOP_2);
        }
    }

    private void createActionItem(HotelImpl asset, String name, String desc, int[] attributes) throws Exception {
        Calendar cal = Calendar.getInstance();
        ActionItem item = new ActionItem(asset);
        item.setHotel(asset);
        item.setName(name);
        item.setDescription(desc);
        item.setPriority(ActionItem.PRIORITY_NORMAL);
        item.setIssueDate(cal.getTime());
        cal.add(Calendar.DAY_OF_YEAR, 90);
        item.setDueDate(cal.getTime());
        for (int i = 0; i < attributes.length; i++) {
            item.addSearchTerm(new AttributeQueryTerm(AttributeQueryTerm.TYPE_ID, "" + attributes[i]));
        }
        actionItemService.createActionItem(item);
    }

    // / Action item defs
    public static final int[] AMER_PREVISIT = new int[] { 578, 337, 1266, 251, 1172, 874, 1239, 305, 847, 1243, 257, 1454, 577, 256, 36, 406, 1140, 1412, 1565, 120, 229, 1246, 1455, 1261, 907, 1695,
            41, 295, 457, 1560, 1180, 3, 1189, 1309, 26, 1238, 1526, 807, 1456, 1235, 299, 1693, 1689, 233, 747, 137, 231, 1095, 33, 556, 291, 292, 1528, 572, 108, 1240, 1152, 34, 1288, 1085, 275,
            804, 1453, 1692, 666, 1263, 302, 794, 781, 810, 1558, 1417, 569, 1248, 597, 285, 805, 833, 742, 639, 668, 451, 1516, 39, 1553, 162, 1484, 326, 331, 811, 254, 228, 820, 364, 4, 1566, 570,
            316, 230, 1265, 49, 7, 1515, 1527, 782, 1256, 1460, 866, 593, 1206, 9, 806, 672, 843, 812, 809, 235, 463, 1103, 1262, 768, 27, 294, 413, 1531, 1252, 1268, 1066, 886, 1251, 558, 130, 1226,
            1250, 1163, 670, 783, 54, 267, 1529, 825, 579, 1294, 1232, 808, 232, 1198, 1276, 1406, 814, 1269, 1285, 427, 403, 1333, 448, 1245, 225, 124, 879, 1281, 813, 588, 253, 15, 1304, 234, 103,
            1533, 1328, 255, 1378, 419, 1253, 466, 671, 1255, 1306, 674, 1501, 227, 65, 1229, 1691, 557, 584, 1244, 410, 1291, 1267, 306, 307, 1249, 6, 1297, 1530, 667, 1459, 673, 25, 226, 262, 575,
            1307, 1264, 1485, 1308, 859, 380, 35, 669, 1122, 1305, 71, 1694, 1131, 252, 1534, 5, 1457, 296, 665, 434, 1555, 1254, 1483, 1148, 392, 293, 718 };

    public static final int[] AMER_ON_SITE = new int[] { 165, 1371, 158, 1401, 1760, 170, 681, 163, 156, 159, 171, 688, 157, 164, 153, 161, 1508, 694, 167, 1748, 166, 168, 1358, 1368, 154, 705, 675,
            1361, 1514, 700, 169, 676, 1364 };

    public static final int[] EMEA_NHOP_1 = new int[] { 558, 814, 833, 843, 820, 825, 847, 593, 597, 768, 747, 1558, 742, 1555, 718, 688, 705, 1514, 694, 1276, 1565, 71, 65, 25, 39, 26, 5112, 27, 33,
            34, 35, 36, 41, 49, 54, 3, 4, 5, 1560, 7, 9, 178, 120, 108, 130, 137, 1066, 1085, 1566, 1103, 1095, 1122, 1131, 1140, 1148, 1163, 1172, 1328, 1333, 1180, 1072, 1198, 1396, 1206, 235, 557,
            558, 575, 569, 570, 572 };

    public static final int[] EMEA_NHOP_2 = new int[] { 694, 153, 676, 1514, 156, 700, 167, 1371, 688, 157, 1358, 170, 1364, 161, 168, 159, 1401, 1760, 171, 1748, 169, 1361, 166, 164, 158, 681, 675,
            154, 705, 163, 1508, 165, 1368 };

    public static final int[] NHOP_DEMO_ACTION_ITEM = new int[] { 569, 556, 570, 71, 557, 558, 575, 572 };

    public static final int[] APAC_NHOP_1 = new int[] { 1244, 1253, 225, 1533, 1266, 1251, 1555, 1262, 25, 1694, 162, 235, 1527, 1243, 1693, 1235, 227, 1206, 9, 1232, 7, 230, 302, 233, 1691, 1689,
            331, 1103, 267, 1189, 1180, 231, 1264, 392, 364, 666, 1406, 71, 1095, 326, 1246, 228, 1248, 742, 1528, 768, 1256, 1484, 1485, 39, 15, 292, 296, 556, 229, 4, 307, 291, 1255, 718, 1252,
            570, 35, 1066, 305, 667, 316, 1328, 1267, 1249, 747, 671, 3, 41, 232, 380, 36, 27, 275, 1692, 33, 457, 6, 306, 1483, 1378, 1254, 234, 575, 1250, 1198, 1261, 557, 26, 1245, 1240, 1553,
            1265, 665, 1238, 65, 1558, 1526, 1515, 1565, 1531, 1534, 226, 54, 34, 569, 668, 295, 294, 285, 578, 1269, 1417, 669, 337, 670, 1529, 49, 673, 1268, 1239, 572, 1263, 299, 1226, 1276, 1560,
            1229, 558, 577 };

    public static final int[] APAC_NHOP_2 = new int[] { 153, 154, 156, 157, 158 };

    public int getRetryGrowthService() {
        return retryGrowthService;
    }

    public void setRetryGrowthService(int retryGrowthService) {
        this.retryGrowthService = retryGrowthService;
    }

    public int getRetryGrowthSerivceInterval() {
        return retryGrowthSerivceInterval;
    }

    public void setRetryGrowthSerivceInterval(int retryGrowthSerivceInterval) {
        this.retryGrowthSerivceInterval = retryGrowthSerivceInterval;
    }

}
