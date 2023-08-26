package org.exthmui.share.udptransport.exceptions

class InvalidPacketException : IllegalArgumentException {
    constructor()
    constructor(s: String?) : super(s)
    constructor(message: String?, cause: Throwable?) : super(message, cause)
    constructor(cause: Throwable?) : super(cause)
}
