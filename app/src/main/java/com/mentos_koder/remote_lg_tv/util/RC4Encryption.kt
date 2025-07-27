package com.mentos_koder.remote_lg_tv.util

import android.content.Context
import android.util.Log
import java.security.MessageDigest
import javax.crypto.Cipher
import javax.crypto.spec.SecretKeySpec
import java.io.IOException

object RC4Encryption {

    private const val RC4_KEY = "@@DANang2024!!!"

    // Đọc dữ liệu
    fun readRawFile(context: Context, resourceId: Int) : String{
        val resourceId = resourceId// ID của tệp trong thư mục res/raw
        val inputStream = context.resources.openRawResource(resourceId)
        val encryptedData = inputStream.readBytes()
        val decryptedData = encryptData(encryptedData)
        return if (decryptedData != null) {
            val fileContent = String(decryptedData, Charsets.UTF_8)
            Log.d("RC4", "Dữ liệu từ tệp đã giải mã: $fileContent")
            fileContent
        } else {
            Log.d("RC4", "Không thể giải mã tệp")
            ""
        }
    }

    // Mã hóa dữ liệu bằng RC4
    fun encryptData(data: ByteArray): ByteArray? {
        return try {
            val cipher = Cipher.getInstance("RC4")
            val keySpec = SecretKeySpec(md5(RC4_KEY), "RC4")
            cipher.init(Cipher.ENCRYPT_MODE, keySpec)
            cipher.doFinal(data)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    // Lưu trữ tệp đã mã hóa vào thư mục files
    fun saveEncryptedFile(context: Context, encryptedData: ByteArray, fileName: String) {
        try {
            val outputStream = context.openFileOutput(fileName, Context.MODE_PRIVATE)

            // Ghi dữ liệu đã mã hóa vào tệp
            outputStream.write(encryptedData)

            // Đóng luồng đầu ra
            outputStream.close()
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    // Hàm băm MD5 để tạo khóa từ chuỗi
    private fun md5(input: String): ByteArray {
        val md = MessageDigest.getInstance("MD5")
        return md.digest(input.toByteArray())
    }


}
