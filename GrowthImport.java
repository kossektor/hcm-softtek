/**
 * 
 */
package com.ihg.hcm.growth;

import java.util.List;

/**
 * @author vuduths
 * 
 */
public interface GrowthImport {

    public boolean importHotel(String assetCode) throws GrowthServiceException;

    // public List<String> getHotelCodesFromGrowth() throws
    // GrowthServiceException;
    public List<String> getUpdatedHotelCodeList() throws GrowthServiceException;

    public String getHostname();
}
