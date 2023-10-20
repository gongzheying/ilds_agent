package org.iata.ilds.agent.util;


import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import java.util.UUID;


public final class FileTrackingUtils {

    public static final String TRACKING_ID_PATTERN = "^((\\d{17})_[\\d|a-f]{32}_(I|O))(_\\d+)?$";
    public static final char INBOUND_DIRECTION_SYMBOL = 'I';
    public static final char OUTBOUND_DIRECTION_SYMBOL = 'O';

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS");
    private static final Pattern compiledTrackingIdPattern = Pattern.compile(TRACKING_ID_PATTERN);

    private FileTrackingUtils() {
    }

    public static String generateTrackingId(final boolean isInbound) {
        final char directionSymbol = isInbound ? INBOUND_DIRECTION_SYMBOL : OUTBOUND_DIRECTION_SYMBOL;
        return String.format("%s_%c", generateTrackingId(), directionSymbol);
    }

    public static String generateTrackingId() {
        return String.format("%s_%s", DATE_FORMAT.format(LocalDateTime.now()), generateUUID());
    }

    public static Boolean isInboundDirection(final String trackingId) {
        final Matcher matcher = compiledTrackingIdPattern.matcher(trackingId);

        if (!matcher.matches()) {
            throw new IllegalArgumentException(String.format("%s is an invalid tracking id.", trackingId));
        }

        return matcher.group(3).charAt(0) == INBOUND_DIRECTION_SYMBOL;
    }

    public static String generateSubTrackingId(final String trackingId, final long fileId) {
        final Matcher matcher = compiledTrackingIdPattern.matcher(trackingId);

        if (!matcher.matches() || matcher.group(4) != null) {
            throw new IllegalArgumentException(String.format("%s is an invalid file package tracking id.", trackingId));
        }

        return trackingId + "_" + fileId;
    }

    public static boolean isTransferPackageTrackingId(final String trackingId) {
        final Matcher matcher = compiledTrackingIdPattern.matcher(trackingId);
        return matcher.matches() && matcher.group(4) == null;
    }

    public static String extractPackageTrackingId(final String trackingId) {
        final Matcher matcher = compiledTrackingIdPattern.matcher(trackingId);

        if (!matcher.matches()) {
            throw new IllegalArgumentException(String.format("%s is an invalid tracking id.", trackingId));
        }

        return matcher.group(1);
    }

    private static String generateUUID() {
        return UUID.randomUUID().toString().replaceAll("-", "");
    }

    public static long extractFileId(final String subTrackingId) {
        final Matcher matcher = compiledTrackingIdPattern.matcher(subTrackingId);

        if (!matcher.matches()) {
            throw new IllegalArgumentException(String.format("%s is an invalid tracking id.", subTrackingId));
        }

        if (matcher.group(4) == null) {
            throw new IllegalArgumentException(String.format("%s has not file id.", subTrackingId));
        }

        return Long.parseLong(matcher.group(4).substring(1));
    }
}
