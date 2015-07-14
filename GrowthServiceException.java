package com.ihg.hcm.growth;

import org.apache.log4j.Logger;

/**
 * Exception related to Growth Rest Service operations. If this exception wraps
 * another, the getMessage method will automatically append the message of the
 * cause exception to this objects message.
 * 
 * @author vuduths
 */
public class GrowthServiceException extends Exception {

    static Logger logger = Logger.getLogger(GrowthServiceException.class.getName());

    /**
     * the serial uid.
     */
    private static final long serialVersionUID = -658823081523811063L;

    /**
     * Constructor setting the message and the cause.
     * 
     * @param message
     *            the message
     * @param cause
     *            the cause
     */
    public GrowthServiceException(final String message, final Throwable cause) {
        super(message, cause);
    }

    /**
     * Constructor setting the message.
     * 
     * @param message
     *            the message
     */
    public GrowthServiceException(final String message) {
        super(message);
    }

    /**
     * Constructor setting the throwable object.
     * 
     * @param e
     */
    public GrowthServiceException(final Throwable e) {
        super(e);
    }

    /**
     * Gets the message. If there is a cause associated, the getMessage of the
     * cause is automatically appended.
     * 
     * @return the message.
     */
    @Override
    public String getMessage() {
        StringBuffer buf = new StringBuffer();
        buf.append(super.getMessage());

        if (getCause() != null) {
            buf.append(".  Cause: " + getCause().getMessage());
        }

        return buf.toString();
    }
}
