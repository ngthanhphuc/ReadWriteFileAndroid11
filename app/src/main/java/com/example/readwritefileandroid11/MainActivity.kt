package com.example.readwritefileandroid11

import android.Manifest
import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.provider.Settings
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.material.snackbar.Snackbar
import kotlinx.android.synthetic.main.activity_main.*
import java.io.IOException
import java.nio.charset.StandardCharsets

private const val PERMISSION_STORAGE = 100

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        if (!hasStoragePermission(baseContext)) {
            requestStoragePermission()
        }

        handleEvent()
    }

    private fun handleEvent() {

        createAndWriteButton.setOnClickListener {

            val collection = MediaStore.Files.getContentUri(MediaStore.VOLUME_EXTERNAL)

            val newMedia = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, "menuCategory")
                put(MediaStore.MediaColumns.MIME_TYPE, "text/plain")
                put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOCUMENTS + "/SunSun/")
            }

            val newMediaUri = application.contentResolver.insert(collection, newMedia)

            val outputStream = contentResolver.openOutputStream(newMediaUri!!)

            outputStream?.write("This is menu category data.".toByteArray())

            outputStream?.close()

            Toast.makeText(this, "File created successfully", Toast.LENGTH_SHORT).show();
        }

        findAndReadButton.setOnClickListener {
            val collection = MediaStore.Files.getContentUri(MediaStore.VOLUME_EXTERNAL)

            val selection = MediaStore.MediaColumns.RELATIVE_PATH + "=?"
            val selectionArgs = arrayOf(Environment.DIRECTORY_DOCUMENTS + "/SunSun/")
            var uri: Uri? = null

            application.contentResolver.query(
                    collection,
                    null,
                    selection,
                    selectionArgs,
                    null
            )?.let { cursor ->
                while (cursor.moveToNext()) {

                    val fileName = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DISPLAY_NAME))
                    if (fileName == "menuCategory.txt") {
                        val id = cursor.getLong(cursor.getColumnIndex(MediaStore.MediaColumns._ID))
                        uri = ContentUris.withAppendedId(collection, id)
                        break
                    }
                }
                if (uri == null) {
                    Toast.makeText(this, "File not found", Toast.LENGTH_SHORT).show()
                } else {
                    try {
                        val inputStream = contentResolver.openInputStream(uri!!)
                        val size = inputStream?.available()
                        val bytes = size?.let { it1 -> ByteArray(it1) }
                        inputStream?.read(bytes)
                        inputStream?.close()
                        val jsonString = String(bytes!!, StandardCharsets.UTF_8)
                        val builder = AlertDialog.Builder(this)
                        builder.setTitle("File Content")
                        builder.setMessage(jsonString)
                        builder.setPositiveButton("OK", null)
                        builder.create().show()
                    } catch (e: IOException) {
                        Toast.makeText(this, "Fail to read file", Toast.LENGTH_SHORT).show()
                    }
                }
            }

        }
    }

    override fun onRequestPermissionsResult(
            requestCode: Int,
            permissions: Array<out String>,
            grantResults: IntArray
    ) {
        when (requestCode) {
            PERMISSION_STORAGE -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(this, "permission granted", Toast.LENGTH_LONG).show()
                } else {
                    showPermissionMissing()
                }
            }
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

    private fun showPermissionMissing() {

        Snackbar.make(cl_container, "You need to grant storage permission.", Snackbar.LENGTH_SHORT)
                .setAction("Grant") {
                    startActivity(getSettingsIntent(packageName))
                }.show()
    }

    private fun getSettingsIntent(packageName: String): Intent {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:$packageName"))
        intent.addCategory(Intent.CATEGORY_DEFAULT)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        return intent
    }

    private fun requestStoragePermission() {
        val permissions = arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE)
        ActivityCompat.requestPermissions(this, permissions, PERMISSION_STORAGE)
    }

    private fun hasStoragePermission(context: Context): Boolean {
        if (hasSdkHigherThan(Build.VERSION_CODES.Q)) {
            return ContextCompat.checkSelfPermission(context, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
        }

        return ContextCompat.checkSelfPermission(context, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
    }

    private fun hasSdkHigherThan(sdk: Int): Boolean {
        return Build.VERSION.SDK_INT > sdk
    }
}