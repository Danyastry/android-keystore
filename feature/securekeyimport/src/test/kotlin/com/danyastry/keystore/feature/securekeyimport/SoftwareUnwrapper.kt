package com.danyastry.keystore.feature.securekeyimport

import java.security.PrivateKey
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

object SoftwareUnwrapper {

    fun unwrap(wrapped: ByteArray, wrappingKey: PrivateKey): ByteArray {
        val fields = DerReader.read(wrapped).children()
        require(fields.size == 6) { "SecureKeyWrapper has 6 fields, found ${fields.size}" }
        val transportKey = Cipher.getInstance(SecureKeyWrapper.RSA_TRANSFORMATION).run {
            init(Cipher.DECRYPT_MODE, wrappingKey, SecureKeyWrapper.OAEP_PARAMETERS)
            doFinal(fields[1].content)
        }
        return Cipher.getInstance("AES/GCM/NoPadding").run {
            init(Cipher.DECRYPT_MODE, SecretKeySpec(transportKey, "AES"), GCMParameterSpec(128, fields[2].content))
            updateAAD(fields[3].encoded)
            doFinal(fields[4].content + fields[5].content)
        }
    }
}
