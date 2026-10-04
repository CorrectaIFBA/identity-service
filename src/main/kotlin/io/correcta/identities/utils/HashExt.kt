package io.correcta.identities.utils

object HashExt {

    fun String.toSha256Hash(): String {
        val bytes = this.toByteArray()
        val md = java.security.MessageDigest.getInstance("SHA-256")
        val digest = md.digest(bytes)

        return digest.joinToString("") { "%02x".format(it) }
    }

    fun String.toSha512Hash(): String {
        val bytes = this.toByteArray()
        val md = java.security.MessageDigest.getInstance("SHA-512")
        val digest = md.digest(bytes)

        return digest.joinToString("") { "%02x".format(it) }
    }

}