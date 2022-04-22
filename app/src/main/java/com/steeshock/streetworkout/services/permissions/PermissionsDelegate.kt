package com.steeshock.streetworkout.services.permissions

import android.app.Activity
import android.content.pm.PackageManager.PERMISSION_GRANTED
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts.RequestPermission
import androidx.core.app.ActivityCompat.shouldShowRequestPermissionRationale
import androidx.core.content.ContextCompat.checkSelfPermission
import androidx.fragment.app.FragmentActivity
import com.steeshock.streetworkout.R
import com.steeshock.streetworkout.utils.extensions.showAlertDialog

/**
 * Delegate interface for handling permission requests
 */
interface PermissionsDelegate {

    fun registerPermissionDelegate(activity: Activity)

    fun checkPermission(
        permission: String,
        onPermissionGranted: () -> Unit = {},
        onCustomRationale: ((startPermissionRequestCallback: () -> Unit) -> Unit)? = null,
        onCustomDenied: (() -> Unit)? = null,
    )
}

class PermissionsDelegateImpl : PermissionsDelegate {

    private lateinit var activity: Activity

    private var requestPermissionLauncher: ActivityResultLauncher<String>? = null

    private var onPermissionGranted: (() -> Unit)? = null
    private var onCustomDenied: (() -> Unit)? = null
    private lateinit var permission: String

    override fun registerPermissionDelegate(activity: Activity) {
        this.activity = activity
        requestPermissionLauncher =
            (this.activity as? FragmentActivity)?.registerForActivityResult(RequestPermission()
            ) { isGranted: Boolean ->
                when {
                    isGranted -> onPermissionGranted?.invoke()
                    else -> showDeniedPermissionDialog(permission, onCustomDenied)
                }
            }
    }

    override fun checkPermission(
        permission: String,
        onPermissionGranted: () -> Unit,
        onCustomRationale: ((() -> Unit) -> Unit)?,
        onCustomDenied: (() -> Unit)?,
    ) {
        this.onPermissionGranted = onPermissionGranted
        this.onCustomDenied = onCustomDenied
        this.permission = permission

        when {
            checkSelfPermission(activity, permission) == PERMISSION_GRANTED -> {
                onPermissionGranted.invoke()
            }
            shouldShowRequestPermissionRationale(activity, permission) -> {
                showRationaleDialog(permission, onCustomRationale)
            }
            else -> {
                requestPermissionLauncher?.launch(permission)
            }
        }
    }

    private fun showRationaleDialog(
        permission: String,
        onCustomRationale: ((startPermissionRequestCallback: () -> Unit) -> Unit)?,
    ) {
        when (onCustomRationale) {
            null -> {
                showDefaultRationaleDialog(permission)
            }
            else -> {
                onCustomRationale.invoke { requestPermissionLauncher?.launch(permission) }
            }
        }
    }

    private fun showDeniedPermissionDialog(
        permission: String,
        onCustomDenied: (() -> Unit)?,
    ) {
        when (onCustomDenied) {
            null -> {
                showDefaultDeniedPermissionDialog(permission)
            }
            else -> {
                onCustomDenied.invoke()
            }
        }
    }

    /**
     * In an educational UI, explain to the user why your app requires this
     * permission for a specific feature to behave as expected. In this UI,
     * include a "cancel" or "no thanks" button that allows the user to
     * continue using your app without granting the permission.
     */
    private fun showDefaultRationaleDialog(permission: String) {
        activity.showAlertDialog(
            title = activity.getString(R.string.permission_rationale_default_title),
            message = activity.getString(R.string.permission_geolocation_denied_explanation),
            positiveText = activity.getString(R.string.ok_item),
            negativeText = activity.getString(R.string.thanks_item),
            onPositiveAction = { requestPermissionLauncher?.launch(permission) },
        )
    }

    /**
     * Explain to the user that the feature is unavailable because the
     * features requires a permission that the user has denied. At the
     * same time, respect the user's decision. Don't link to system
     * settings in an effort to convince the user to change their decision.
     */
    private fun showDefaultDeniedPermissionDialog(permission: String) {
        activity.showAlertDialog(
            title = activity.getString(R.string.permission_rationale_default_title),
            message = activity.getString(R.string.permission_geolocation_denied_explanation),
            positiveText = activity.getString(R.string.clear_item),
        )
    }
}