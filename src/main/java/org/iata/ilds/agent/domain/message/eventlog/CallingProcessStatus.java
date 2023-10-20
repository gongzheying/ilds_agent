package org.iata.ilds.agent.domain.message.eventlog;


import org.iata.ilds.agent.domain.message.eventlog.AbstractEventLogMessage.CallingProcess;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import static org.iata.ilds.agent.domain.message.eventlog.AbstractEventLogMessage.CallingProcess.*;
import static org.iata.ilds.agent.domain.message.eventlog.LogType.*;

public final class CallingProcessStatus {

    private static final Map<CallingProcessStatus, Integer> processToIdMap = new HashMap<>();

    private final LogType status;
    private final CallingProcess callingProcess;

    public static final int PROBLEM_RESOLUTION_BACKUP_STARTED_ID = 101;
    public static final int PROBLEM_RESOLUTION_QUARANTINE_STARTED_ID = 102;
    public static final int PROBLEM_RESOLUTION_BACKUP_FAILED_ID = 103;
    public static final int PROBLEM_RESOLUTION_QUARANTINE_FAILED_ID = 104;

    public static final int INBOUND_RECEIPT_TRACKINGIDCREATED_ID = 111;
    public static final int INBOUND_RECEIPT_COMPLETED_ID = 112;
    public static final int INBOUND_RECEIPT_FAILED_ID = 113;
    public static final int INBOUND_RECEIPT_SECURE_DELETION_FAILED_ID = 114;

    public static final int INBOUND_BACKUP_STARTED_ID = 121;
    public static final int INBOUND_BACKUP_COMPLETED_ID = 122;
    public static final int INBOUND_BACKUP_FAILED_ID = 123;
    public static final int INBOUND_BACKUP_SECURE_DELETION_FAILED_ID = 124;

    public static final int INBOUND_VERIFICATION_STARTED_ID = 131;
    public static final int INBOUND_VERIFICATION_COMPLETED_ID = 132;
    public static final int INBOUND_VERIFICATION_FAILED_ID = 133;
    public static final int INBOUND_VERIFICATION_SECURE_DELETION_FAILED_ID = 134;

    public static final int INBOUND_SUBSTITUTION_STARTED_ID = 141;
    public static final int INBOUND_SUBSTITUTION_COMPLETED_ID = 142;
    public static final int INBOUND_SUBSTITUTION_FAILED_ID = 143;
    public static final int INBOUND_SUBSTITUTION_SECURE_DELETION_FAILED_ID = 144;

    public static final int INBOUND_DISPATCH_PREPARATION_STARTED_ID = 151;
    public static final int INBOUND_DISPATCH_PREPARATION_COMPLETED_ID = 152;
    public static final int INBOUND_DISPATCH_PREPARATION_FAILED_ID = 153;
    public static final int INBOUND_DISPATCH_PREPARATION_SECURE_DELETION_FAILED_ID = 154;

    public static final int INBOUND_DISPATCH_STARTED_ID = 161;
    public static final int INBOUND_DISPATCH_COMPLETED_ID = 162;
    public static final int INBOUND_DISPATCH_FAILED_ID = 163;
    public static final int INBOUND_DISPATCH_SECURE_DELETION_FAILED_ID = 164;

    public static final int OUTBOUND_RECEIPT_TRACKINGIDCREATED_ID = 211;
    public static final int OUTBOUND_RECEIPT_COMPLETED_ID = 212;
    public static final int OUTBOUND_RECEIPT_FAILED_ID = 213;
    public static final int OUTBOUND_RECEIPT_SECURE_DELETION_FAILED_ID = 214;

    public static final int OUTBOUND_VERIFICATION_STARTED_ID = 221;
    public static final int OUTBOUND_VERIFICATION_COMPLETED_ID = 222;
    public static final int OUTBOUND_VERIFICATION_FAILED_ID = 223;
    public static final int OUTBOUND_VERIFICATION_SECURE_DELETION_FAILED_ID = 224;

    public static final int OUTBOUND_RESTORATION_STARTED_ID = 231;
    public static final int OUTBOUND_RESTORATION_COMPLETED_ID = 232;
    public static final int OUTBOUND_RESTORATION_FAILED_ID = 233;
    public static final int OUTBOUND_RESTORATION_SECURE_DELETION_FAILED_ID = 234;

    public static final int OUTBOUND_DISPATCH_PREPARATION_STARTED_ID = 241;
    public static final int OUTBOUND_DISPATCH_PREPARATION_COMPLETED_ID = 242;
    public static final int OUTBOUND_DISPATCH_PREPARATION_FAILED_ID = 243;
    public static final int OUTBOUND_DISPATCH_PREPARATION_SECURE_DELETION_FAILED_ID = 244;

    public static final int OUTBOUND_DISPATCH_STARTED_ID = 251;
    public static final int OUTBOUND_DISPATCH_COMPLETED_ID = 252;
    public static final int OUTBOUND_DISPATCH_FAILED_ID = 253;
    public static final int OUTBOUND_DISPATCH_SECURE_DELETION_FAILED_ID = 254;

    public static final int ONLINE_FILE_SUBSTITUTION_STARTED_ID = 311;
    public static final int ONLINE_FILE_SUBSTITUTION_COMPLETED_ID = 312;
    public static final int ONLINE_FILE_SUBSTITUTION_FAILED_ID = 313;
    public static final int ONLINE_FILE_SUBSTITUTION_SECURE_DELETION_FAILED_ID = 314;

    public static final int ONLINE_FILE_RESTORATION_STARTED_ID = 411;
    public static final int ONLINE_FILE_RESTORATION_COMPLETED_ID = 412;
    public static final int ONLINE_FILE_RESTORATION_FAILED_ID = 413;
    public static final int ONLINE_FILE_RESTORATION_SECURE_DELETION_FAILED_ID = 414;

    public static final int ONLINE_TRANSACTION_STARTED_ID = 511;
    public static final int ONLINE_TRANSACTION_COMPLETED_ID = 512;
    public static final int ONLINE_TRANSACTION_FAILED_ID = 513;

    public static final int OFFLINE_SCANNING_STARTED_ID = 601;
    public static final int OFFLINE_SCANNING_COMPLETED_ID = 602;
    public static final int OFFLINE_SCANNING_FAILED_ID = 603;
    public static final int OFFLINE_SCANNING_SECURE_DELETE_FAILED_ID = 604;

    public static final int QUARANTINE_STARTED_ID = 801;
    public static final int QUARANTINE_COMPLETED_ID = 802;
    public static final int QUARANTINE_FAILED_ID = 803;
    public static final int QUARANTINE_SECURE_DELETION_FAILED_ID = 804;

    public static final int INBOUND_BACKUP_DELETION_STARTED_ID = 901;
    public static final int INBOUND_BACKUP_DELETION_COMPLETED_ID = 902;
    public static final int INBOUND_BACKUP_DELETION_FAILED_ID = 903;
    public static final int INBOUND_BACKUP_DELETION_SECURE_DELETION_FAILED_ID = 904;

    static {
        add(PROBLEM_RESOLUTION_BACKUP, Started, PROBLEM_RESOLUTION_BACKUP_STARTED_ID);
        add(PROBLEM_RESOLUTION_BACKUP, Failed, PROBLEM_RESOLUTION_BACKUP_FAILED_ID);
        add(PROBLEM_RESOLUTION_QUARANTINE, Started, PROBLEM_RESOLUTION_QUARANTINE_STARTED_ID);
        add(PROBLEM_RESOLUTION_QUARANTINE, Failed, PROBLEM_RESOLUTION_QUARANTINE_FAILED_ID);

        add(INBOUND_RECEIPT, TrackingIdCreated, INBOUND_RECEIPT_TRACKINGIDCREATED_ID);
        add(INBOUND_RECEIPT, Completed, INBOUND_RECEIPT_COMPLETED_ID);
        add(INBOUND_RECEIPT, Failed, INBOUND_RECEIPT_FAILED_ID);
        add(INBOUND_RECEIPT, SecureDeletionFailed, INBOUND_RECEIPT_SECURE_DELETION_FAILED_ID);

        add(INBOUND_BACKUP, Started, INBOUND_BACKUP_STARTED_ID);
        add(INBOUND_BACKUP, Completed, INBOUND_BACKUP_COMPLETED_ID);
        add(INBOUND_BACKUP, Failed, INBOUND_BACKUP_FAILED_ID);
        add(INBOUND_BACKUP, SecureDeletionFailed, INBOUND_BACKUP_SECURE_DELETION_FAILED_ID);

        add(INBOUND_VERIFICATION, Started, INBOUND_VERIFICATION_STARTED_ID);
        add(INBOUND_VERIFICATION, Completed, INBOUND_VERIFICATION_COMPLETED_ID);
        add(INBOUND_VERIFICATION, Failed, INBOUND_VERIFICATION_FAILED_ID);
        add(INBOUND_VERIFICATION, SecureDeletionFailed, INBOUND_VERIFICATION_SECURE_DELETION_FAILED_ID);

        add(INBOUND_SUBSTITUTION, Started, INBOUND_SUBSTITUTION_STARTED_ID);
        add(INBOUND_SUBSTITUTION, Completed, INBOUND_SUBSTITUTION_COMPLETED_ID);
        add(INBOUND_SUBSTITUTION, Failed, INBOUND_SUBSTITUTION_FAILED_ID);
        add(INBOUND_SUBSTITUTION, SecureDeletionFailed, INBOUND_SUBSTITUTION_SECURE_DELETION_FAILED_ID);

        add(INBOUND_DISPATCH_PREPARATION, Started, INBOUND_DISPATCH_PREPARATION_STARTED_ID);
        add(INBOUND_DISPATCH_PREPARATION, Completed, INBOUND_DISPATCH_PREPARATION_COMPLETED_ID);
        add(INBOUND_DISPATCH_PREPARATION, Failed, INBOUND_DISPATCH_PREPARATION_FAILED_ID);
        add(INBOUND_DISPATCH_PREPARATION, SecureDeletionFailed, INBOUND_DISPATCH_PREPARATION_SECURE_DELETION_FAILED_ID);

        add(INBOUND_DISPATCH, Started, INBOUND_DISPATCH_STARTED_ID);
        add(INBOUND_DISPATCH, Completed, INBOUND_DISPATCH_COMPLETED_ID);
        add(INBOUND_DISPATCH, Failed, INBOUND_DISPATCH_FAILED_ID);
        add(INBOUND_DISPATCH, SecureDeletionFailed, INBOUND_DISPATCH_SECURE_DELETION_FAILED_ID);

        add(OUTBOUND_RECEIPT, TrackingIdCreated, OUTBOUND_RECEIPT_TRACKINGIDCREATED_ID);
        add(OUTBOUND_RECEIPT, Completed, OUTBOUND_RECEIPT_COMPLETED_ID);
        add(OUTBOUND_RECEIPT, Failed, OUTBOUND_RECEIPT_FAILED_ID);
        add(OUTBOUND_RECEIPT, SecureDeletionFailed, OUTBOUND_RECEIPT_SECURE_DELETION_FAILED_ID);

        add(OUTBOUND_VERIFICATION, Started, OUTBOUND_VERIFICATION_STARTED_ID);
        add(OUTBOUND_VERIFICATION, Completed, OUTBOUND_VERIFICATION_COMPLETED_ID);
        add(OUTBOUND_VERIFICATION, Failed, OUTBOUND_VERIFICATION_FAILED_ID);
        add(OUTBOUND_VERIFICATION, SecureDeletionFailed, OUTBOUND_VERIFICATION_SECURE_DELETION_FAILED_ID);

        add(OUTBOUND_RESTORATION, Started, OUTBOUND_RESTORATION_STARTED_ID);
        add(OUTBOUND_RESTORATION, Completed, OUTBOUND_RESTORATION_COMPLETED_ID);
        add(OUTBOUND_RESTORATION, Failed, OUTBOUND_RESTORATION_FAILED_ID);
        add(OUTBOUND_RESTORATION, SecureDeletionFailed, OUTBOUND_RESTORATION_SECURE_DELETION_FAILED_ID);

        add(OUTBOUND_DISPATCH_PREPARATION, Started, OUTBOUND_DISPATCH_PREPARATION_STARTED_ID);
        add(OUTBOUND_DISPATCH_PREPARATION, Completed, OUTBOUND_DISPATCH_PREPARATION_COMPLETED_ID);
        add(OUTBOUND_DISPATCH_PREPARATION, Failed, OUTBOUND_DISPATCH_PREPARATION_FAILED_ID);
        add(OUTBOUND_DISPATCH_PREPARATION, SecureDeletionFailed, OUTBOUND_DISPATCH_PREPARATION_SECURE_DELETION_FAILED_ID);

        add(OUTBOUND_DISPATCH, Started, OUTBOUND_DISPATCH_STARTED_ID);
        add(OUTBOUND_DISPATCH, Completed, OUTBOUND_DISPATCH_COMPLETED_ID);
        add(OUTBOUND_DISPATCH, Failed, OUTBOUND_DISPATCH_FAILED_ID);
        add(OUTBOUND_DISPATCH, SecureDeletionFailed, OUTBOUND_DISPATCH_SECURE_DELETION_FAILED_ID);

        add(QUARANTINE, Started, QUARANTINE_STARTED_ID);
        add(QUARANTINE, Completed, QUARANTINE_COMPLETED_ID);
        add(QUARANTINE, Failed, QUARANTINE_FAILED_ID);
        add(QUARANTINE, SecureDeletionFailed, QUARANTINE_SECURE_DELETION_FAILED_ID);

        add(INBOUND_BACKUP_DELETION, Started, INBOUND_BACKUP_DELETION_STARTED_ID);
        add(INBOUND_BACKUP_DELETION, Completed, INBOUND_BACKUP_DELETION_COMPLETED_ID);
        add(INBOUND_BACKUP_DELETION, Failed, INBOUND_BACKUP_DELETION_FAILED_ID);
        add(INBOUND_BACKUP_DELETION, SecureDeletionFailed, INBOUND_BACKUP_DELETION_SECURE_DELETION_FAILED_ID);

        add(OFFLINE_SCANNING, Started, OFFLINE_SCANNING_STARTED_ID);
        add(OFFLINE_SCANNING, Completed, OFFLINE_SCANNING_COMPLETED_ID);
        add(OFFLINE_SCANNING, Failed, OFFLINE_SCANNING_FAILED_ID);
        add(OFFLINE_SCANNING, SecureDeletionFailed, OFFLINE_SCANNING_SECURE_DELETE_FAILED_ID);

        add(ONLINE_TRANSACTION, Started, ONLINE_TRANSACTION_STARTED_ID);
        add(ONLINE_TRANSACTION, Completed, ONLINE_TRANSACTION_COMPLETED_ID);
        add(ONLINE_TRANSACTION, Failed, ONLINE_TRANSACTION_FAILED_ID);

        add(ONLINE_FILE, Started, ONLINE_FILE_SUBSTITUTION_STARTED_ID);
        add(ONLINE_FILE, Completed, ONLINE_FILE_SUBSTITUTION_COMPLETED_ID);
        add(ONLINE_FILE, Failed, ONLINE_FILE_SUBSTITUTION_FAILED_ID);

        add(ONLINE_FILE_OUTBOUND, Started, ONLINE_FILE_RESTORATION_STARTED_ID);
        add(ONLINE_FILE_OUTBOUND, Completed, ONLINE_FILE_RESTORATION_COMPLETED_ID);
        add(ONLINE_FILE_OUTBOUND, Failed, ONLINE_FILE_RESTORATION_FAILED_ID);
    }

    public static int getId(final LogType status, final CallingProcess callingProcess) {
        return processToIdMap.get(new CallingProcessStatus(status, callingProcess));
    }



    private static void add(final CallingProcess callingProcess, final LogType status, final int id) {
        CallingProcessStatus callingProcessStatus = new CallingProcessStatus(status, callingProcess);
        processToIdMap.put(callingProcessStatus, id);
    }

    private CallingProcessStatus(final LogType logType, final CallingProcess callingProcess) {
        this.status = logType;
        this.callingProcess = callingProcess;
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CallingProcessStatus that = (CallingProcessStatus) o;
        return status == that.status && callingProcess == that.callingProcess;
    }

    @Override
    public int hashCode() {
        return Objects.hash(status, callingProcess);
    }
}
