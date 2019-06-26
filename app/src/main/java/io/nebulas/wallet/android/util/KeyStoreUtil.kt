package io.nebulas.wallet.android.util


import android.os.Build
import android.security.KeyPairGeneratorSpec
import io.nebulas.wallet.android.app.WalletApplication
import io.nebulas.wallet.android.common.Constants
import java.io.ByteArrayOutputStream
import java.math.BigInteger
import java.security.KeyPairGenerator
import java.security.KeyStore
import java.util.*
import javax.crypto.Cipher
import javax.security.auth.x500.X500Principal


/**
 * 使用ksyStore加密工具类
 *
 * Created by Heinoc on 2018/3/17.
 */
object KeyStoreUtil {
    lateinit var keyStore: KeyStore


    private fun initKeyStore(alias: String = Constants.KEYSTORE_ALIAS) {
        try {
            keyStore = KeyStore.getInstance("AndroidKeyStore")
            keyStore.load(null)
        } catch (e: Exception) {
            e.printStackTrace()
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
            createNewKeys(alias)
        }
    }

    private fun createNewKeys(alias: String = Constants.KEYSTORE_ALIAS) {
        if ("" != alias) {
            try {
                // Create new key if needed
                if (!keyStore.containsAlias(alias)) {
                    val start = Calendar.getInstance()
                    val end = Calendar.getInstance()
                    end.add(Calendar.YEAR, 1)
                    var spec: KeyPairGeneratorSpec? = null
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
                        spec = KeyPairGeneratorSpec.Builder(WalletApplication.INSTANCE)
                                .setAlias(alias)
                                .setSubject(X500Principal("CN=Sample Name, O=Android Authority"))
                                .setSerialNumber(BigInteger.ONE)
                                .setStartDate(start.time)
                                .setEndDate(end.time)
                                .build()
                    }
                    val generator = KeyPairGenerator.getInstance("RSA", "AndroidKeyStore")
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
                        generator.initialize(spec)
                    }

                    generator.generateKeyPair()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }

        }


    }


    /**
     * 加密方法，支持分段加密
     * @param needEncryptWord　需要加密的字符串
     * @param alias　加密秘钥
     * @return
     */
    fun encryptString(needEncryptWord: String, alias: String = Constants.KEYSTORE_ALIAS): ByteArray {
        var result = "".toByteArray(charset("UTF-8"))
        if ("" != alias && "" != needEncryptWord) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
                initKeyStore(alias)
            }
            var outputStream: ByteArrayOutputStream? = null
            try {
                val privateKeyEntry = keyStore.getEntry(alias, null) as KeyStore.PrivateKeyEntry
                if (needEncryptWord.isEmpty()) {
                    return result
                }

                //            Cipher inCipher = Cipher.getInstance("RSA/ECB/PKCS1Padding", "AndroidOpenSSL");
                val inCipher = Cipher.getInstance("RSA/ECB/PKCS1Padding")
                //            inCipher.init(Cipher.ENCRYPT_MODE, publicKey);
                inCipher.init(Cipher.ENCRYPT_MODE, privateKeyEntry.certificate.publicKey)


                var blockSize = if(inCipher.blockSize == 0) 117 else inCipher.blockSize
                //blockSize 为密钥长度（bytes） - 11
                var keyByteSize = blockSize + 11
                var plainBytes = needEncryptWord.toByteArray(charset("UTF-8"))
                var totalBlocks = Math.ceil(plainBytes.size / blockSize.toDouble()).toInt()
                outputStream = ByteArrayOutputStream(totalBlocks * keyByteSize)

                var offset = 0
                while (offset < plainBytes.size) {
                    var inputLen = plainBytes.size - offset
                    if (inputLen > blockSize)
                        inputLen = blockSize

                    outputStream.write(inCipher.doFinal(plainBytes, offset, inputLen))

                    offset += blockSize
                }

                outputStream.flush()
                result = outputStream.toByteArray()

            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                if (null != outputStream)
                    outputStream.close()
                outputStream = null
            }

        }

        return result

    }


    /**
     * 解方法，支持分段解密
     * @param needDecryptWord　需要解密的byte数组
     * @param alias　加密秘钥
     * @return
     */
    fun decryptString(needDecryptWord: ByteArray, alias: String = Constants.KEYSTORE_ALIAS): String {
        var result = ""
        if (needDecryptWord.isNotEmpty()) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
                initKeyStore(alias)
            }
            var outputStream: ByteArrayOutputStream? = null
            try {
                val privateKeyEntry = keyStore.getEntry(alias, null) as KeyStore.PrivateKeyEntry
                //            RSAPrivateKey privateKey = (RSAPrivateKey) privateKeyEntry.getPrivateKey();

                //            Cipher output = Cipher.getInstance("RSA/ECB/PKCS1Padding", "AndroidOpenSSL");
                val outCipher = Cipher.getInstance("RSA/ECB/PKCS1Padding")
                //            output.init(Cipher.DECRYPT_MODE, privateKey);
                outCipher.init(Cipher.DECRYPT_MODE, privateKeyEntry.privateKey)

                var blockSize = 245
                //blockSize 为密钥长度（bytes） - 11
                var keyByteSize = 256
                var totalBlocks = needDecryptWord.size / keyByteSize
                outputStream = ByteArrayOutputStream(totalBlocks * blockSize)

                var offset = 0
                while (offset < needDecryptWord.size) {
                    var inputLen = needDecryptWord.size - offset
                    if (inputLen > keyByteSize)
                        inputLen = keyByteSize

                    outputStream.write(outCipher.doFinal(needDecryptWord, offset, inputLen))

                    offset += keyByteSize
                }

                outputStream.flush()
                result = outputStream.toString()

            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                if (null != outputStream)
                    outputStream.close()
                outputStream = null
            }
        }

        return result

    }


//    /**
//     * 加密方法
//     * @param needEncryptWord　需要加密的字符串
//     * @param alias　加密秘钥
//     * @return
//     */
//    fun encryptString(needEncryptWord: String, alias: String = Constants.KEYSTORE_ALIAS): String {
//        if ("" != alias && "" != needEncryptWord) {
//            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
//                initKeyStore(alias)
//            }
//            val encryptStr = ""
//            var vals: ByteArray? = null
//            try {
//                val privateKeyEntry = keyStore.getEntry(alias, null) as KeyStore.PrivateKeyEntry
//                if (needEncryptWord.isEmpty()) {
//                    return encryptStr
//                }
//
//                //            Cipher inCipher = Cipher.getInstance("RSA/ECB/PKCS1Padding", "AndroidOpenSSL");
//                val inCipher = Cipher.getInstance("RSA/ECB/PKCS1Padding")
//                //            inCipher.init(Cipher.ENCRYPT_MODE, publicKey);
//                inCipher.init(Cipher.ENCRYPT_MODE, privateKeyEntry.certificate.publicKey)
//
//                val outputStream = ByteArrayOutputStream()
//                val cipherOutputStream = CipherOutputStream(outputStream, inCipher)
//                cipherOutputStream.write(needEncryptWord.toByteArray(charset("UTF-8")))
//                cipherOutputStream.close()
//
//                vals = outputStream.toByteArray()
//            } catch (e: Exception) {
//                e.printStackTrace()
//            }
//
//            return Base64.encodeToString(vals, Base64.DEFAULT)
//        } else
//            return ""
//    }
//
//
//    fun decryptString(needDecryptWord: String, alias: String = Constants.KEYSTORE_ALIAS): String {
//        if ("" != alias && "" != needDecryptWord) {
//            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
//                initKeyStore(alias)
//            }
//            var decryptStr = ""
//            try {
//                val privateKeyEntry = keyStore.getEntry(alias, null) as KeyStore.PrivateKeyEntry
//                //            RSAPrivateKey privateKey = (RSAPrivateKey) privateKeyEntry.getPrivateKey();
//
//                //            Cipher output = Cipher.getInstance("RSA/ECB/PKCS1Padding", "AndroidOpenSSL");
//                val output = Cipher.getInstance("RSA/ECB/PKCS1Padding")
//                //            output.init(Cipher.DECRYPT_MODE, privateKey);
//                output.init(Cipher.DECRYPT_MODE, privateKeyEntry.privateKey)
//                val cipherInputStream = CipherInputStream(
//                        ByteArrayInputStream(Base64.decode(needDecryptWord, Base64.DEFAULT)), output)
//                val values = ArrayList<Byte>()
//                var nextByte = cipherInputStream.read()
//
//                while (nextByte != -1) {
//                    values.add(nextByte.toByte())
//
//                    nextByte = cipherInputStream.read()
//                }
//
//                val bytes = ByteArray(values.size)
//                for (i in bytes.indices) {
//                    bytes[i] = values[i]
//                }
//
//                decryptStr = String(bytes, 0, bytes.size, charset("UTF-8"))
//
//            } catch (e: Exception) {
//                e.printStackTrace()
//            }
//
//            return decryptStr
//        }
//        return ""
//    }


}