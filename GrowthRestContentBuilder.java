package com.ihg.hcm.growth;

import java.sql.Timestamp;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.ihg.hcm.config.Configuration;
import com.ihg.hcm.content.ContentDataConstants;
import com.ihg.hcm.growth.schemas.AddressTranslation;
import com.ihg.hcm.growth.schemas.NameTranslation;

import org.apache.log4j.Logger;

import com.ihg.hcm.growth.schemas.Address;
import com.ihg.hcm.growth.schemas.HotelContactListType;
import com.ihg.hcm.growth.schemas.HotelContactsType;
import com.ihg.hcm.importer.status.GenericMapping;
import com.ihg.hcm.importer.status.MappingEntry;
import com.ihg.hcm.importer.util.DataImportUtil;
import com.ihg.hcm.model.CatalogService;
import com.ihg.hcm.model.Content;
import com.ihg.hcm.model.HotelImpl;
import com.ihg.hcm.model.HotelAttribute;
import com.ihg.hcm.model.HotelAttributeDAO;
import com.ihg.location.Location;
import com.ihg.location.LocationDAO;

/**
 * DOCUMENT ME!
 *
 * @author vuduths
 */
public class GrowthRestContentBuilder {

    static Logger logger = Logger.getLogger(GrowthRestContentBuilder.class.getName());

    /**
     * the date format object
     */
    private DateFormat dateFormat;
    private String US_LOCALE = "en_US";
    /**
     * the asset attribute object
     */
    private HotelAttributeDAO assetAttributeDAO;
    private LocationDAO locationDAO;
    private GenericMapping statusMapping;
    private CatalogService catalogService;
    private Configuration configuration;

    public void setCatalogService(CatalogService catalogService) {
        this.catalogService = catalogService;
    }

    public LocationDAO getLocationDAO() {
        return locationDAO;
    }

    public void setLocationDAO(LocationDAO locationDAO) {
        this.locationDAO = locationDAO;
    }

    public void setConfiguration(Configuration configuration) {
        this.configuration = configuration;
    }

    /**
     * DOCUMENT ME!
     *
     * @return the dateFormat
     */
    public DateFormat getDateFormat() {
        return dateFormat;
    }

    /**
     * DOCUMENT ME!
     *
     * @param dateFormat
     *            the dateFormat to set
     */
    public void setDateFormat(DateFormat dateFormat) {
        this.dateFormat = dateFormat;
    }

    /**
     * DOCUMENT ME!
     *
     * @return the assetAttributeDAO
     */
    public HotelAttributeDAO getHotelAttributeDAO() {
        return assetAttributeDAO;
    }

    /**
     * DOCUMENT ME!
     *
     * @param assetAttributeDAO
     *            the assetAttributeDAO to set
     */
    public void setHotelAttributeDAO(HotelAttributeDAO assetAttributeDAO) {
        this.assetAttributeDAO = assetAttributeDAO;
    }

    /**
     * Creates a Content object based on the response received from Growth
     * Service and also, based on the comparison of the values in the database.
     *
     * @param hotelNode
     * @param assetCode
     * @param asset
     * @param prevHotelStatusCode
     *
     * @return Content object
     *
     * @throws GrowthServiceException
     */
    public Content getContentObject(com.ihg.hcm.growth.schemas.Hotel hotelNode, HotelContactListType hotelContactList, String assetCode, HotelImpl asset, String prevHotelStatusCode)

            throws GrowthServiceException {
        Content content = new Content();
        content.setHotelCode(assetCode);

        String brandCode = hotelNode.getBrandCode();
        String gdsChainCode = null;
        if (brandCode != null) {
            // The GDS Chain code is derived from the brand code based on the
            // map
            // supplied by business
            gdsChainCode = DataImportUtil.getGdsChainCodeMap().get(brandCode);
        }
        String brandName = hotelNode.getBrandName();
        String chainCode = hotelNode.getChainCode();
        String hotelName = hotelNode.getHotelName();
        Map<String, String> hotelNames = prepareNames(hotelName, hotelNode.getNameTranslationsList());
        String hotelShortName = hotelNode.getHotelShortName();
        String hotelLongName = hotelNode.getHotelLongName();
        String hotelGDSName = hotelNode.getHotelGDSName();

        Address address = hotelNode.getHotelAddress();
        // String operDivision =
        // getOperatingDivision(hotelNode.getOperatingDivision());
        String tempOperDivision = hotelNode.getTempOperatingDivision();

        String mgmtSubRegion = hotelNode.getManagementSubRegion();
        String mgmtArea = hotelNode.getManagementArea();
        String mgmtCluster = hotelNode.getManagementCluster();
        Integer mgmtType = null;
        if (hotelNode.getManagementType() > 0) {
            mgmtType = new Integer(hotelNode.getManagementType());
        }
        String mgmtTypeDescription = hotelNode.getManagementTypeDescription();
        Integer locationNumber = null;
        if (hotelNode.getLocationNumber() > 0) {
            locationNumber = new Integer(hotelNode.getLocationNumber());
        }

        Integer facilityNumber = null;
        if (hotelNode.getFacilityNumber() > 0) {
            facilityNumber = new Integer(hotelNode.getFacilityNumber());
        }

        String hotelStatus = hotelNode.getHotelStatus();
        String contractStatus = hotelNode.getContractStatus();
        // Mapping removed so to store the actual Growth HotelImpl Status
        // if (hotelStatus != null) {
        // hotelStatus = getHotelStatusMap().get(hotelStatus.trim());
        // }

        String hotelStatusCodeStr = hotelNode.getHotelStatusCode();
        Integer hotelStatusCode = hotelStatusCodeStr == null || "".equals(hotelStatusCodeStr) ? null : new Integer(hotelStatusCodeStr);

        String contractStatusCodeStr = hotelNode.getContractStatusCode();
        Integer contractStatusCode = contractStatusCodeStr == null || "".equals(contractStatusCodeStr) ? null : new Integer(contractStatusCodeStr);

        String dateOpened = getUtilDate(hotelNode.getDateOpened());
        String dateClosed = getUtilDate(hotelNode.getDateClosed());
        String preSellDate = getUtilDate(hotelNode.getPreSellDate());
        String ownerCompany = hotelNode.getOwnerCompany();
        String mgmtCompany = hotelNode.getManagementCompany();
        Integer nbrRooms = null;

        String genericHotelStatus = getGenericHotelStatus(preSellDate, hotelStatusCode);

        if (hotelNode.getNumberOfRooms() > 0) {
            nbrRooms = new Integer(hotelNode.getNumberOfRooms());
        }
        Timestamp yearBuilt = null;
        String yearBuiltStr = hotelNode.getYearBuilt();
        if (yearBuiltStr != null && !"".equals(yearBuiltStr)) {
            try {
                yearBuilt = new Timestamp(new SimpleDateFormat("yyyy").parse(yearBuiltStr).getTime());
            } catch (ParseException pe) {
                throw new GrowthServiceException(pe.getMessage(), pe);
            }
        }

        String trvlAgentCommStatus = hotelNode.getTravelAgentCommissionStatus();
        String currencyCode = hotelNode.getUsedCurrencyCode();

        Boolean trvlAgentCommStatusInHCM = getTravelAgentCommValue(trvlAgentCommStatus);

        String addressLine3 = null;
        String isoCountryCode = null;
        String isoStateCode = null;
        if (address != null) {
            addressLine3 = address.getLine3();
            isoCountryCode = address.getIsoCountryCode();
            isoStateCode = address.getIsoStateCode();
            if(isoStateCode != null) {
                address.setState(isoStateCode);
            }
        }

        Location country = locationDAO.retrieveCountry(isoCountryCode);
        Location subRegion = null;
        String region = null;
        if (country != null) {
            subRegion = country.getParent();
            region = subRegion.getParent().getName();
        }

        Boolean holidexListed = new Boolean(DataImportUtil.convertStringToBoolean(hotelNode.getHolidexListed()));

        List<HotelAttribute> results = content.getHotelAttributes();

        if ((brandCode != null) && !"".equals(brandCode)) {
            createHotelAttribute(results, ContentDataConstants.BRAND_CD, brandCode, asset, ContentDataConstants.TEXT_TYP);
        }
        if ((chainCode != null) && !"".equals(chainCode)) {
            createHotelAttribute(results, ContentDataConstants.CHAIN_CD, chainCode, asset, ContentDataConstants.TEXT_TYP);
        }
        if ((gdsChainCode != null) && !"".equals(gdsChainCode)) {
            createHotelAttribute(results, ContentDataConstants.GDS_CHAIN_CD, gdsChainCode, asset, ContentDataConstants.TEXT_TYP);
        }
        if (!hotelNames.isEmpty()) {
            createHotelAttribute(results, ContentDataConstants.HOTEL_NAME, hotelNames, asset, ContentDataConstants.MULTILINGUAL_TYPE);
        }
        if ((brandName != null) && !"".equals(brandName)) {
            createHotelAttribute(results, ContentDataConstants.BRAND_NAME, brandName, asset, ContentDataConstants.TEXT_TYP);
        }
        createHotelAttribute(results, ContentDataConstants.GDS_SHORT_NAME, hotelShortName, asset, ContentDataConstants.TEXT_TYP);

        createHotelAttribute(results, ContentDataConstants.GDS_LONG_NAME, hotelLongName, asset, ContentDataConstants.TEXT_TYP);

        // commenting as per JIRA# 1633. Uncomment when Renee confirms to enable
        // GDS Name import.
        // createHotelAttribute(results, ContentDataConstants.GDS_HOTEL_NAME,
        // hotelGDSName, asset,
        // ContentDataConstants.TEXT_TYP);

        createHotelAttribute(results, ContentDataConstants.HOTEL_ADDRESS, prepareAddresses(address, hotelNode.getAddressTranslationsList()), asset, ContentDataConstants.ADDRESS_TYP);

        createHotelAttribute(results, ContentDataConstants.HOTEL_ADDRESS_LINE3, addressLine3, asset, ContentDataConstants.MULTILINGUAL_TYPE);

        createHotelAttribute(results, ContentDataConstants.ISO_COUNTRY_CODE, isoCountryCode, asset, ContentDataConstants.TEXT_TYP);

        createHotelAttribute(results, ContentDataConstants.ISO_STATE_CODE, isoStateCode, asset, ContentDataConstants.TEXT_TYP);

        createHotelAttribute(results, ContentDataConstants.HOTEL_REGION, region, asset, ContentDataConstants.TEXT_TYP);

        createHotelAttribute(results, ContentDataConstants.TEMP_OPERATING_DIVISION, tempOperDivision, asset, ContentDataConstants.TEXT_TYP);

        createHotelAttribute(results, ContentDataConstants.MNGMT_SUB_REGION, mgmtSubRegion, asset, ContentDataConstants.TEXT_TYP);

        createHotelAttribute(results, ContentDataConstants.MNGMT_AREA, mgmtArea, asset, ContentDataConstants.TEXT_TYP);

        createHotelAttribute(results, ContentDataConstants.MNGMT_CLUSTER, mgmtCluster, asset, ContentDataConstants.TEXT_TYP);

        createHotelAttribute(results, ContentDataConstants.MGMT_TYP, mgmtType, asset, ContentDataConstants.NUMBER_TYP);

        createHotelAttribute(results, ContentDataConstants.MGMT_TYP_DESCRIPTION, mgmtTypeDescription, asset, ContentDataConstants.TEXT_TYP);

        createHotelAttribute(results, ContentDataConstants.LOCATION_NUMBER, locationNumber, asset, ContentDataConstants.NUMBER_TYP);

        createHotelAttribute(results, ContentDataConstants.FACILITY_NUMBER, facilityNumber, asset, ContentDataConstants.NUMBER_TYP);

        createHotelAttribute(results, ContentDataConstants.HOTEL_STATUS, hotelStatus, asset, ContentDataConstants.TEXT_TYP);

        createHotelAttribute(results, ContentDataConstants.GENERIC_HOTEL_STATUS, genericHotelStatus, asset, ContentDataConstants.TEXT_TYP);

        createHotelAttribute(results, ContentDataConstants.HOTEL_STATUS_CODE, hotelStatusCode, asset, ContentDataConstants.NUMBER_TYP);

        createHotelAttribute(results, ContentDataConstants.CONTRACT_STATUS, contractStatus, asset, ContentDataConstants.TEXT_TYP);

        createHotelAttribute(results, ContentDataConstants.CONTRACT_STATUS_CODE, contractStatusCode, asset, ContentDataConstants.TEXT_TYP);

        createHotelAttribute(results, ContentDataConstants.DATE_OPENED, dateOpened, asset, ContentDataConstants.TEXT_TYP);

        createHotelAttribute(results, ContentDataConstants.DATE_CLOSED, dateClosed, asset, ContentDataConstants.TEXT_TYP);

        createHotelAttribute(results, ContentDataConstants.PRE_SELL_DATE, preSellDate, asset, ContentDataConstants.TEXT_TYP);

        createHotelAttribute(results, ContentDataConstants.OWNER_COMPANY, ownerCompany, asset, ContentDataConstants.TEXT_TYP);

        createHotelAttribute(results, ContentDataConstants.MNGMT_COMPANY, mgmtCompany, asset, ContentDataConstants.TEXT_TYP);

        createHotelAttribute(results, ContentDataConstants.NBR_ROOMS, nbrRooms, asset, ContentDataConstants.NUMBER_TYP);

        createHotelAttribute(results, ContentDataConstants.YEAR_BUILT, yearBuilt, asset, ContentDataConstants.DATETIME_TYPE);

        createHotelAttribute(results, ContentDataConstants.TRVL_AGENT_COMM_STATUS, trvlAgentCommStatusInHCM, asset, ContentDataConstants.BOOLEAN_TYPE);

        createHotelAttribute(results, ContentDataConstants.CURRENCY_CODE, currencyCode, asset, ContentDataConstants.TEXT_TYP);

        createHotelAttribute(results, ContentDataConstants.LISTED_IN_HOLIDEX, holidexListed, asset, ContentDataConstants.BOOLEAN_TYPE);

        createHotelAttribute(results, ContentDataConstants.PROJECTED_OPEN_DATE, getUtilDate(hotelNode.getProjectedOpenDate()), asset, ContentDataConstants.TEXT_TYP);

        createHotelAttribute(results, ContentDataConstants.ORIGIN_TYPE, hotelNode.getOriginType(), asset, ContentDataConstants.TEXT_TYP);

        createHotelAttribute(results, ContentDataConstants.CONSUMER_FRIENDLY_URL, getHotelUrl(hotelNode.getBsaName(), hotelNode.getBrandCode()), asset, ContentDataConstants.TEXT_TYP);

        if (hotelNode.getMilitaryInstallation() != null) {
            createHotelAttribute(results, ContentDataConstants.MILITARY_INSTALLATION, hotelNode.getMilitaryInstallation(), asset, ContentDataConstants.TEXT_TYP);
        }

        if (isDataChanged(hotelStatusCode, prevHotelStatusCode)) {
            Integer prevhotelStatusCodeInt = prevHotelStatusCode == null || "".equals(prevHotelStatusCode) ? null : new Integer(prevHotelStatusCode);
            String hotelStatusDescription = getHotelStatusDescription(preSellDate, prevhotelStatusCodeInt);

            if (ContentDataConstants.TEMPORARILY_CLOSED.equals(hotelStatusDescription)) {
                createHotelAttribute(results, ContentDataConstants.TEMP_CLOSED_RFP_OVERRIDE, null, asset, ContentDataConstants.BOOLEAN_TYPE);
            }
        }

        populateContacts(results, getContactsMap(hotelContactList), asset);

        return content;
    }

    /**
     * @param results
     * @param asset
     * @throws GrowthServiceException
     */
    private Map<String, String> getContactsMap(HotelContactListType hotelContactList) throws GrowthServiceException {
        String roleCode = null;
        String firstName = null;
        String lastName = null;
        Map<String, String> contactsMap = new HashMap<String, String>();
        List<String> roleCodeList = DataImportUtil.getRoleCodeList();
        if (hotelContactList != null) {
            List<HotelContactsType> contactList = hotelContactList.getHotelContactsList();
            // iterate over the hotel contacts and create a map of Names keyed
            // by role code
            for (HotelContactsType contact : contactList) {
                roleCode = contact.getRoleCode();
                if (roleCode != null && roleCodeList.contains(roleCode)) {
                    firstName = contact.getFirstName();
                    lastName = contact.getLastName();
                    contactsMap.put(roleCode, DataImportUtil.getFullName(firstName, lastName));
                }
            }
        }

        return contactsMap;
    }

    /**
     * @param results
     * @param contactsMap
     * @param asset
     * @throws GrowthServiceException
     */
    private void populateContacts(List<HotelAttribute> results, Map<String, String> contactsMap, HotelImpl asset) throws GrowthServiceException {
        String contactName = contactsMap.get(DataImportUtil.AREA_MANAGER_NAME);
        createHotelAttribute(results, ContentDataConstants.AREA_MANAGER_NAME, contactName, asset, ContentDataConstants.TEXT_TYP);

        contactName = contactsMap.get(DataImportUtil.IHG_REGIONAL_DIRECTOR_NAME);
        createHotelAttribute(results, ContentDataConstants.IHG_REGIONAL_DIRECTOR_NAME, contactName, asset, ContentDataConstants.TEXT_TYP);

        contactName = contactsMap.get(DataImportUtil.IHG_REGIONAL_REVENUE_MANAGER_NAME);
        createHotelAttribute(results, ContentDataConstants.IHG_REGIONAL_REVENUE_MANAGER_NAME, contactName, asset, ContentDataConstants.TEXT_TYP);

        contactName = contactsMap.get(DataImportUtil.IHG_REGIONAL_MANAGER_FOR_HIRE_NAME);
        createHotelAttribute(results, ContentDataConstants.IHG_REGIONAL_MANAGER_FOR_HIRE_NAME, contactName, asset, ContentDataConstants.TEXT_TYP);

        contactName = contactsMap.get(DataImportUtil.IHG_HOTEL_OPERATIONS_SUPPORT_MANAGER_NAME);
        createHotelAttribute(results, ContentDataConstants.IHG_HOTEL_OPERATIONS_SUPPORT_MANAGER_NAME, contactName, asset, ContentDataConstants.TEXT_TYP);

        contactName = contactsMap.get(DataImportUtil.IHG_QUALITY_CONSULTANT_NAME);
        createHotelAttribute(results, ContentDataConstants.IHG_QUALITY_CONSULTANT_NAME, contactName, asset, ContentDataConstants.TEXT_TYP);

        String brand = asset.getBrand();
        // The Food and Beverage field applies only to Indigo Brand
        if (brand != null && "INDG".equals(brand)) {
            contactName = contactsMap.get(DataImportUtil.IHG_FOOD_AND_BEVERAGE_CONSULTANT_NAME);
            createHotelAttribute(results, ContentDataConstants.IHG_FOOD_AND_BEVERAGE_CONSULTANT_NAME, contactName, asset, ContentDataConstants.TEXT_TYP);
        }

        contactName = contactsMap.get(DataImportUtil.IHG_BRAND_SERVICE_MANAGER_NAME);
        createHotelAttribute(results, ContentDataConstants.IHG_BRAND_SERVICE_MANAGER_NAME, contactName, asset, ContentDataConstants.TEXT_TYP);

        contactName = contactsMap.get(DataImportUtil.IHG_BRAND_SERVICE_CONSULTANT_NAME);
        createHotelAttribute(results, ContentDataConstants.IHG_BRAND_SERVICE_CONSULTANT_NAME, contactName, asset, ContentDataConstants.TEXT_TYP);

        // contactName =
        // contactsMap.get(DataImportUtil.IHG_PIP_CONSULTANT_NAME);
        // createHotelAttribute(results,
        // ContentDataConstants.IHG_PIP_CONSULTANT_NAME, contactName, asset,
        // ContentDataConstants.TEXT_TYP);
        //
        // contactName =
        // contactsMap.get(DataImportUtil.IHG_PLAN_REVIEW_CONSULTANT_NAME);
        // createHotelAttribute(results,
        // ContentDataConstants.IHG_PLAN_REVIEW_CONSULTANT_NAME, contactName,
        // asset,
        // ContentDataConstants.TEXT_TYP);

    }

    /**
     * Sets the HotelAttribute properties if one exists in the database or
     * creates a new one.
     *
     * @param results
     * @param strAttrNumber
     * @param obj
     * @param asset
     * @param fieldType
     *
     * @throws GrowthServiceException
     */
    private void createHotelAttribute(List<HotelAttribute> results, String strAttrNumber, Object obj, HotelImpl asset, int fieldType) throws GrowthServiceException {
        Integer attributeNumber = new Integer(strAttrNumber);

        try {
            catalogService.getEffectiveCatalog(asset).getAttributeSpec(attributeNumber);

            boolean dataChanged = false;
            HotelAttribute aa = assetAttributeDAO.findByNumber(asset, attributeNumber);

            // if no HotelAttribute exists then create a new one.
            if (aa == null) {
                aa = new HotelAttribute();
                aa.setAttributeNumber(attributeNumber);
            }

            aa.getSubattributes().clear();
            aa.getInstances().clear();

            try {
                if (fieldType != ContentDataConstants.ADDRESS_TYP) {
                    dataChanged = setPropertyValue(aa, obj, fieldType);
                } else {
                    dataChanged = setMultiLanguageAddress(aa, (Map<String, Address>) obj);
                }
            } catch (Exception e) {
                throw new GrowthServiceException(e);
            }

            if (dataChanged) {
                // aa.setUnsubmitted(true);
                aa.setChanged(true);
                results.add(aa);
            }
        } catch (Exception ex) {
            logger.warn("Attribute " + strAttrNumber + " for hotel " + asset.getCode() + " not found: " + ex.getMessage());
        }
    }

    /**
     * Sets the value of an HotelAttibute if it is changed.
     *
     * @param aa
     * @param fieldName
     * @param newValue
     *
     * @return true/false based on the value changed or not.
     */
    private boolean setIfChanged(HotelAttribute aa, String fieldName, Object newValue) {
        boolean valueChanged = false;
        Object currValue = aa.getValue(fieldName);

        if (isDataChanged(currValue, newValue)) {
            aa.setValue(fieldName, newValue);
            valueChanged = true;
        }

        return valueChanged;
    }

    private boolean setIfChanged(HotelAttribute aa, String fieldName, Object newValue, String locale) {
        boolean valueChanged = false;
        Object currValue = aa.getValue(fieldName, locale);

        if (isDataChanged(currValue, newValue)) {
            aa.setValue(fieldName, (String) newValue, locale);
            valueChanged = true;
        }

        return valueChanged;
    }

    private boolean setMultiLanguageValue(HotelAttribute aa, String fieldName, Object newValue, String defaultLocale) {
        if (newValue instanceof Map) {
            boolean valueChanged = false;
            Map<String, String> values = (Map<String, String>) newValue;
            for (String locale : values.keySet()) {
                valueChanged = valueChanged || setIfChanged(aa, fieldName, values.get(locale), locale);
            }
            return valueChanged;
        } else {
            return setIfChanged(aa, fieldName, newValue, defaultLocale);
        }
    }

    /**
     * Sets the HotelAttribute property type and value.
     *
     * @param aa
     * @param newValue
     * @param fieldType
     *
     * @return true/false based on the value changed or not.
     */
    private boolean setPropertyValue(HotelAttribute aa, Object newValue, int fieldType) {
        boolean valueChanged = false;

        if (fieldType == ContentDataConstants.TEXT_TYP) {
            valueChanged = setIfChanged(aa, "hcm:basicText-TextValue", newValue);
        } else if (fieldType == ContentDataConstants.NUMBER_TYP) {
            valueChanged = setIfChanged(aa, "hcm:integer-IntegerValue", newValue);
        } else if (fieldType == ContentDataConstants.TERNARY_TYPE) {
            valueChanged = setIfChanged(aa, "hcm:ternary-TextValue", newValue);
        } else if (fieldType == ContentDataConstants.BOOLEAN_TYPE) {
            valueChanged = setIfChanged(aa, "hcm:boolean-BooleanValue", newValue);
        } else if (fieldType == ContentDataConstants.DATETIME_TYPE) {
            valueChanged = setIfChanged(aa, "hcm:datetime-DateValue", newValue);
        } else if (fieldType == ContentDataConstants.MULTILINGUAL_TYPE) {
            valueChanged = setMultiLanguageValue(aa, "hcm:mlText-TextValue", newValue, US_LOCALE);
        }

        return valueChanged;
    }

    /**
     * Compares two objects for equality.
     *
     * @param obj1
     * @param obj2
     *
     * @return true/false
     */
    private boolean isDataChanged(Object obj1, Object obj2) {
        String dbValue = null;
        String clientValue = null;

        if (obj1 != null) {
            dbValue = obj1.toString();
        }

        if (obj2 != null) {
            clientValue = obj2.toString();
        }

        if ((dbValue == null) && (clientValue == null)) {
            return false;
        } else if ((dbValue != null) && (clientValue != null)) {
            // notice the !. if the values are not same then return true,
            // indicating the data change.
            return !dbValue.equals(clientValue);
        } else {
            // one of the values is null and other is not
            return true;
        }
    }

    private boolean setMultiLanguageAddress(HotelAttribute aa, Map<String, Address> addresses) {
        boolean valueChanged = false;
        for (String locale : addresses.keySet()) {
            valueChanged = setAddressProperties(aa, addresses.get(locale), locale);
        }
        return valueChanged;
    }

    /**
     * Sets the address properties on the HotelAttribute.
     *
     * @param aa
     * @param newValue
     * @param fieldType
     *
     * @return true/false based on the value changed or not.
     */
    private boolean setAddressProperties(HotelAttribute aa, Object newValue, String locale) {

        // NOTE: The intent here is to store the countryCode in the space where
        // the country name was formerly stored.
        // The idea behind that change is that the country name display can be
        // gleaned from the ISO country code (2-letter)
        // using the existing Java API Locale object capabilities.
        // Created a new field for Iso Country Code. Storing the country name
        // from Growth in the Coutry field for Address.

        boolean valueChanged = false;
        int nbrFieldsChanged = 0;
        Address addr = null;
        String line1 = null;
        String line2 = null;
        String line3 = null;
        String city = null;
        String state = null;
        // String isoStateCode = null;
        String postalCode = null;
        String countryName = null;

        if (newValue != null && newValue instanceof Address) {
            addr = (Address) newValue;
            line1 = addr.getLine1();
            line2 = addr.getLine2();
            line3 = addr.getLine3();
            city = addr.getCity();
            state = addr.getState();
            // isoStateCode = addr.getIsoStateCode();
            // if (isoStateCode != null && !"".equals(isoStateCode)) {
            // state = isoStateCode;
            // }
            postalCode = addr.getPostalCode();
            countryName = addr.getCountry();
        }

        valueChanged = setIfChanged(aa, "hcm:mlAddress-Line1", line1, locale);

        if (valueChanged) {
            nbrFieldsChanged++;
        }

        valueChanged = setIfChanged(aa, "hcm:mlAddress-Line2", line2, locale);

        if (valueChanged) {
            nbrFieldsChanged++;
        }

        valueChanged = setIfChanged(aa, "hcm:mlAddress-Line3", line3, locale);

        if (valueChanged) {
            nbrFieldsChanged++;
        }

        valueChanged = setIfChanged(aa, "hcm:mlAddress-City", city, locale);

        if (valueChanged) {
            nbrFieldsChanged++;
        }

        valueChanged = setIfChanged(aa, "hcm:mlAddress-State", state, locale);

        if (valueChanged) {
            nbrFieldsChanged++;
        }

        valueChanged = setIfChanged(aa, "hcm:mlAddress-PostalCode", postalCode, locale);

        if (valueChanged) {
            nbrFieldsChanged++;
        }

        valueChanged = setIfChanged(aa, "hcm:mlAddress-Country", countryName, locale);

        if (valueChanged) {
            nbrFieldsChanged++;
        }

        if (nbrFieldsChanged > 0) {
            valueChanged = true;
        }

        return valueChanged;
    }

    /**
     * Converts java Calendar object to java util Data object.
     *
     * @param cal
     *
     * @return date string
     */
    private String getUtilDate(Calendar cal) {
        if (cal != null) {
            return dateFormat.format(cal.getTime());
        } else {
            return null;
        }
    }

    private String getGenericHotelStatus(String preSellDate, Integer hotelStatus) {
        return statusMapping.getGenericStatus(preSellDate, hotelStatus);
    }

    private String getHotelStatusDescription(String preSellDate, Integer hotelStatus) {
        MappingEntry entry =  statusMapping.getMappingEntry(preSellDate, hotelStatus);

        if (entry != null) {
            return entry.getDescription();
        }

        return "";
    }

    /**
     * Maps the Growth travel agent status value to HCM database value.
     *
     * @param trvlAgentCommStatus
     *
     * @return result string.
     */
    private Boolean getTravelAgentCommValue(String trvlAgentCommStatus) {
        Map<String, Boolean> travelAgentCommMap = ContentDataConstants.getTravelAgentCommStatusMap();
        Boolean result = null;

        if (trvlAgentCommStatus != null) {
            result = travelAgentCommMap.get(trvlAgentCommStatus.trim());
        }

        return result;
    }

    /**
     * Updates the HotelImpl object if any of the values are modified.
     *
     * @param hotelNode
     * @param asset
     *
     * @return true/false based on if a value has been modified or not.
     */
    public boolean updateHotel(com.ihg.hcm.growth.schemas.Hotel hotelNode, HotelImpl asset) {

        // NOTE: This is invoked separately before building the asset
        // attributes.
        // The asset record is updated and saved up in the caller,
        // GrowthDataProcessor.importHotel
        // The result is that the asset object is saved/updated before building
        // the address asset attribute object above.

        boolean dataChanged = false;
        String brandCode = hotelNode.getBrandCode();
        String chainCode = hotelNode.getChainCode();
        String hotelName = hotelNode.getHotelName();
        Address address = hotelNode.getHotelAddress();
        String line1 = null;
        String line2 = null;
        String city = null;
        String state = null;
        String isoStateCode = null;
        String postalCode = null;
        String countryCode = null;
        String hotelStatus = null;
        String hotelStatusCode = null;
        String currencyCode = hotelNode.getUsedCurrencyCode();
        String holidexListed = hotelNode.getHolidexListed();
        Boolean holidexListedFlag = new Boolean(false);

        if (holidexListed != null && "Y".equals(holidexListed)) {
            holidexListedFlag = new Boolean(true);
        }

        if (address != null) {
            line1 = address.getLine1();
            line2 = address.getLine2();
            city = address.getCity();
            state = address.getState();
            isoStateCode = address.getIsoStateCode();
            if (isoStateCode != null && !"".equals(isoStateCode)) {
                state = isoStateCode;
            }
            postalCode = address.getPostalCode();
            countryCode = address.getIsoCountryCode();
        }

        String operDivision = getOperatingDivision(hotelNode.getOperatingDivision());
        String growthHotelStatus = hotelNode.getHotelStatus();
        hotelStatusCode = hotelNode.getHotelStatusCode();

        if (growthHotelStatus != null) {
            hotelStatus = getHotelStatusMap().get(growthHotelStatus.trim());
        }

        if (brandCode != null) {
            if (isDataChanged(brandCode, asset.getBrand())) {
                asset.setBrand(brandCode);
                dataChanged = true;
            }
        }

        if (chainCode != null) {
            if (isDataChanged(chainCode, asset.getChain())) {
                asset.setChain(chainCode);
                dataChanged = true;
            }
        }

        if (hotelName != null) {
            if (isDataChanged(hotelName, asset.getDisplayName())) {
                asset.setDisplayName(hotelName);
                dataChanged = true;
            }
        }

        if (isDataChanged(line1, asset.getAddressLine1())) {
            asset.setAddressLine1(line1);
            dataChanged = true;
        }

        if (isDataChanged(line2, asset.getAddressLine2())) {
            asset.setAddressLine2(line2);
            dataChanged = true;
        }

        if (isDataChanged(city, asset.getCity())) {
            asset.setCity(city);
            dataChanged = true;
        }

        if (isDataChanged(state, asset.getState())) {
            asset.setState(state);
            dataChanged = true;
        }

        if (isDataChanged(postalCode, asset.getZipcode())) {
            asset.setZipcode(postalCode);
            dataChanged = true;
        }

        if (isDataChanged(countryCode, asset.getCountryCode())) {
            asset.setCountry(locationDAO.retrieveCountry(countryCode));
            dataChanged = true;
        }
        if (isDataChanged(operDivision, asset.getLegacyRegion())) {
            asset.setLegacyRegion(operDivision);
            dataChanged = true;
        }

        if (isDataChanged(currencyCode, asset.getCurrency())) {
            asset.setCurrency(currencyCode);
            dataChanged = true;
        }

        if (growthHotelStatus != null) {
            if (isDataChanged(hotelStatus, asset.getStatus())) {
                asset.setStatus(hotelStatus);
                dataChanged = true;
            }
        }

        if (isDataChanged(hotelStatusCode, asset.getGrowthStatus())) {
            asset.setGrowthStatus(hotelStatusCode);
            dataChanged = true;
        }

        if (isDataChanged(holidexListedFlag, asset.getHolidexListed())) {
            asset.setHolidexListed(holidexListedFlag);
            dataChanged = true;
        }

        return dataChanged;
    }

    /**
     * Returns a HCM string for the hotel region of operation.
     *
     * @param operDivision
     *
     * @return
     */
    private static String getOperatingDivision(String operDivision) {
        if (operDivision != null) {
            if (operDivision.equals(ContentDataConstants.GROWTH_MEXICO_REGION)) {
                operDivision = ContentDataConstants.HCM_AMER_REGION;
            } else if (operDivision.equals(ContentDataConstants.GROWTH_AMER_REGION)) {
                operDivision = ContentDataConstants.HCM_AMER_REGION;
            } else if (operDivision.equals(ContentDataConstants.GROWTH_ASIAPAC_REGION)) {
                operDivision = ContentDataConstants.HCM_APAC_REGION;
            }
        }

        return operDivision;
    }

    public void setStatusMapping(GenericMapping statusMapping) {
        this.statusMapping = statusMapping;
    }

    /**
     * Returns a map of HCM equivalent status message corresponding to the
     * Growth one.
     *
     * @return a map
     */
    public static Map<String, String> getHotelStatusMap() {
        return ContentDataConstants.getHotelStatusMap();
    }

    private String getHotelUrl(String bsaName, String brandCode) {
        if (bsaName != null && !bsaName.isEmpty() && brandCode != null) {
            String domainName = configuration.getConstants().getBrandDomainName(brandCode);
            if (domainName != null && !domainName.isEmpty()) {
                bsaName = bsaName.replaceAll(" ", "").replaceAll(",", "").replaceAll(",", "").toLowerCase();
                if (domainName.endsWith("/")) {
                    return domainName + bsaName;
                } else {
                    return domainName + "/" + bsaName;
                }
            } else {
                logger.info("No domain name for brand " + brandCode);
            }
        }

        return null;
    }

    private Map<String, String> prepareNames(String name, List<NameTranslation> translations) {
        Map<String, String> names = new HashMap<String, String>();

        if (name != null) {
            names.put(US_LOCALE, name);
        }

        if (translations != null) {
            for (NameTranslation translation : translations) {
                if (translation.getName() != null && translation.getLocale() != null) {
                    names.put(fixLocale(translation.getLocale()), translation.getName());
                }
            }
        }
        return names;
    }

    private Map<String, Address> prepareAddresses(Address address, List<AddressTranslation> translations) {
        Map<String, Address> addresses = new HashMap<String, Address>();

        if (address != null) {
            addresses.put(US_LOCALE, address);
        }

        if (translations != null) {
            for (AddressTranslation translation : translations) {
                if (translation != null && translation.getLocale() != null) {
                    addresses.put(fixLocale(translation.getLocale()), translation);
                }
            }
        }
        return addresses;
    }


    private String fixLocale(String growthLocale) {
        if (growthLocale != null) {
            return growthLocale.replaceAll("-","_");
        } else {
            return null;
        }
    }

}
