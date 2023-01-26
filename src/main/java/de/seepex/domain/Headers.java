package de.seepex.domain;

public enum Headers {

    INVOKED_METHOD_RETURN_TYPE,
    RPC_INVOKED_AT,
    RPC_RTT,
    PAYLOAD_SIZE,
    IS_COMPRESSED,
    USER_ID,
    TYPE_HINTS,
    APPLICATION_HEADERS,  // these are used to transport application specific infos (like ModificationEventMetadataKeys)
    EXCEPTION_TEXT,       // if the target method fails with an exception, the exception content will be transported in this header
    EXCEPTION_CLASS_NAME, // if the target method fails with an exception, the exception class name will be transported in this header
    CALLER_CLASS,         // name of class / service making the rpc call -> used for stats
    CALLER_SERVICE,       // name of the microservice which is making the call (extracted from the EnableSpxRpc annotation)
    CALLER_HOSTNAME,      // name of the host which is making the rpc call
    REPLIED_BY_HOSTNAME,  // name of the host which is replying to the rpc call
}
