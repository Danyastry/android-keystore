package com.danyastry.keystore.feature.hmac

import java.security.MessageDigest
import javax.crypto.Mac
import javax.crypto.SecretKey

class HmacSigner {

    fun tag(data: ByteArray, key: SecretKey): ByteArray = Mac.getInstance(ALGORITHM).run {
        init(key)
        doFinal(data)
    }

    fun verify(data: ByteArray, tag: ByteArray, key: SecretKey): Boolean = MessageDigest.isEqual(tag(data, key), tag)

    companion object {
        const val ALGORITHM = "HmacSHA256"
        const val TAG_SIZE = 32
    }
}
