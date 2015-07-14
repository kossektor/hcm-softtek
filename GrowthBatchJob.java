/**
 * 
 */
package com.ihg.hcm.growth;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.List;

import org.apache.log4j.Logger;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.SchedulerContext;
import org.quartz.SchedulerException;
import org.springframework.context.ApplicationContext;

import com.ihg.hcm.command.GrowthImportCommand;
import com.ihg.hcm.command.HotelCommandBatchJob;
import com.ihg.hcm.command.HotelCommandRecord;
import com.ihg.hcm.model.HotelCommandService;

/**
 * DOCUMENT ME!
 * 
 * @author vuduths
 */
public class GrowthBatchJob implements Job {

    private static final Logger LOGGER = Logger.getLogger(GrowthBatchJob.class.getName());
    private static final String ADMIN_USERNAME = "Administrator";

    private GrowthImport growthDataProcessor;
    private HotelCommandService hotelCommandService;

    public GrowthBatchJob() {
    }

    /**
     * DOCUMENT ME!
     * 
     * @param context
     *            DOCUMENT ME!
     * 
     * @throws JobExecutionException
     *             DOCUMENT ME!
     */
    public void execute(JobExecutionContext context) throws JobExecutionException {
        long startTime = System.currentTimeMillis();
        try {
            InetAddress addr = InetAddress.getLocalHost();
            // Get hostname
            String actualHostname = addr.getHostName();

            List<String> assetCodes = null;
            String hostname = null;
            SchedulerContext schedulerCtx = context.getScheduler().getContext();
            ApplicationContext applicationContext = (ApplicationContext) schedulerCtx.get("applicationContext");
            growthDataProcessor = (GrowthImport) applicationContext.getBean("growthDataProcessor");
            hotelCommandService = (HotelCommandService) applicationContext.getBean("hotelCommandService");

            if (growthDataProcessor != null && hotelCommandService != null) {
                hostname = growthDataProcessor.getHostname();
                if (actualHostname != null && actualHostname.equals(hostname)) {
                    LOGGER.info("***** EXECUTING GROWTH BATCH JOB on [" + actualHostname + "] ******");
                    try {
                        // get the new hotels from Growth
                        assetCodes = growthDataProcessor.getUpdatedHotelCodeList();
                    } catch (Exception e) {
                        LOGGER.info("Error retrieving the new hotels from Growth");
                    }
                    if (assetCodes != null) {
                        processGrowthImport(assetCodes);
                    }

                    long endTime = System.currentTimeMillis();
                    long duration = endTime - startTime;
                    LOGGER.info("Total time taken for Growth Batch Job: [" + duration + "ms]");
                    LOGGER.info("*************************** FINISHED GROWTH BATCH JOB  **********************************");
                } else {
                    LOGGER.info("Skipping Growth Job, Actual Hostname: " + actualHostname + "; Supplied Hostname: " + hostname);
                }
            } else {
                LOGGER.info("Required spring bean is null, skipping Growth Batch Job");
            }
        } catch (SchedulerException e) {
            throw new JobExecutionException(e);
        } catch (UnknownHostException uhe) {
            throw new JobExecutionException(uhe);
        }
    }

    private void processGrowthImport(List<String> assetCodes) {
        hotelCommandService.updateRecentJobsStatus(HotelCommandBatchJob.GROWTH_IMPORT);
        List<HotelCommandBatchJob> recent = hotelCommandService.findRecentJobs(HotelCommandBatchJob.GROWTH_IMPORT);

        if (!recent.isEmpty()) {
            LOGGER.info("Skipping Growth Job, Growth Import Command job already running or enqueued");
            return;
        }

        LOGGER.info("Enqueue Growth Import Command job for Hotels: " + assetCodes);
        HotelCommandRecord prototype = GrowthImportCommand.createRecord(null);
        hotelCommandService.enqueueJob(assetCodes, HotelCommandBatchJob.GROWTH_IMPORT, ADMIN_USERNAME, prototype);
    }

}
