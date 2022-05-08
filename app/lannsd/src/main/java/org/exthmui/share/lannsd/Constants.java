package org.exthmui.share.lannsd;

public class Constants {
    public static final String COMMAND_CANCEL = "CANCEL";
    public static final String COMMAND_ACCEPT = "ACCEPT";
    public static final String COMMAND_REJECT = "REJECT";
    public static final String COMMAND_SUCCESS = "SUCCESS";
    public static final String COMMAND_FAILURE = "FAILURE";
    public static final String COMMAND_TRANSFER = "TRANSFER";
    public static final String COMMAND_TRANSFER_END = "TRANSFER_END";

    public static final String SHARE_PROTOCOL_VERSION_1 = "1.0";

    public static final String LOCAL_SERVICE_INSTANCE_NAME = "_share";
    public static final String LOCAL_SERVICE_SERVICE_TYPE = "_share._udp.";

    public static final String RECORD_STRING_CHARSET = "UTF-8";

    // Key longer than 9 character is not allowed
    public static final String RECORD_KEY_SHARE_PROTOCOL_VERSION = "protocolV";
    public static final String RECORD_KEY_SERVER_PORT = "sPort";
    public static final String RECORD_KEY_UID = "uid";
    public static final String RECORD_KEY_ACCOUNT_SERVER_SIGN = "servSign";

    public static final String FILE_INFO_EXTRA_KEY_MD5 = "md5";
}
