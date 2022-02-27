package com.steeshock.android.streetworkout.presentation.views

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.app.Dialog
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Geocoder
import android.location.Location
import android.os.Bundle
import android.os.Handler
import android.os.ResultReceiver
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.viewModels
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.findNavController
import com.github.dhaval2404.imagepicker.ImagePicker
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.tasks.OnSuccessListener
import com.steeshock.android.streetworkout.R
import com.steeshock.android.streetworkout.common.BaseFragment
import com.steeshock.android.streetworkout.common.Constants
import com.steeshock.android.streetworkout.common.appComponent
import com.steeshock.android.streetworkout.data.model.Category
import com.steeshock.android.streetworkout.data.model.Place
import com.steeshock.android.streetworkout.databinding.FragmentAddPlaceBinding
import com.steeshock.android.streetworkout.presentation.viewStates.AddPlaceViewState
import com.steeshock.android.streetworkout.presentation.viewmodels.AddPlaceViewModel
import com.steeshock.android.streetworkout.services.FetchAddressIntentService
import com.steeshock.android.streetworkout.utils.extensions.gone
import com.steeshock.android.streetworkout.utils.extensions.visible
import java.util.*
import javax.inject.Inject

class AddPlaceFragment : BaseFragment() {

    @Inject
    lateinit var factory: ViewModelProvider.Factory

    private val viewModel: AddPlaceViewModel by viewModels { factory }

    private lateinit var imagePicker: ImagePicker.Builder

    private var _binding: FragmentAddPlaceBinding? = null
    private val binding get() = _binding!!

    private var allCategories = emptyList<Category>()

    override fun injectComponent() {
        context?.appComponent?.providePlacesComponent()?.inject(this)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentAddPlaceBinding.inflate(inflater, container, false)
        resultReceiver = AddressResultReceiver(Handler())
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity())
        return binding.root
    }

    private fun initViews() {
        binding.toolbar.setNavigationOnClickListener { view ->
            view.findNavController().navigateUp()
        }

        binding.placeTitle.addTextChangedListener {
            if (!it.isNullOrEmpty()) {
                binding.placeTitleInput.error = null
            }
        }

        binding.placeAddress.addTextChangedListener {
            if (!it.isNullOrEmpty()) {
                binding.placeAddressInput.error = null
            }
        }

        binding.myPositionButton.setOnClickListener {
            getPosition()
        }

        binding.categoryButton.setOnClickListener {
            showCategories()
        }

        binding.takeImageButton.setOnClickListener {
            openImagePicker()
        }

        binding.clearButton.setOnClickListener {
            showModalDialog(
                title = getString(R.string.clear_fields_alert),
                message = getString(R.string.clear_fields_message),
                positiveText = getString(R.string.ok_item),
                negativeText = getString(R.string.cancel_item),
                onPositiveAction = { resetFields() },
            )
        }

        binding.sendButton.setOnClickListener {
            if (validatePlace()) {
                showModalDialog(
                    title = getString(R.string.clear_fields_alert),
                    message = getString(R.string.publish_permission_message),
                    positiveText = getString(R.string.ok_item),
                    negativeText = getString(R.string.cancel_item),
                    onPositiveAction = { viewModel.onAddNewPlace(place = getNewPlace()) },
                )
            }
        }
        binding.placeTitle.addTextChangedListener {
            binding.placeTitleInput.error = null
        }
        binding.placeAddress.addTextChangedListener {
            binding.placeTitleInput.error = null
        }
        imagePicker = ImagePicker.with(requireActivity())
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initViews()

        viewModel.allCategories.observe(viewLifecycleOwner) { categories ->
            categories?.let { allCategories = it }

            if (viewModel.checkedCategoriesArray == null) {
                viewModel.checkedCategoriesArray = BooleanArray(allCategories.size)
            }
        }

        viewModel.viewState.observe(viewLifecycleOwner) {
            renderViewState(it)
        }
    }

    private fun renderViewState(viewState: AddPlaceViewState) {
        if (viewState.loadCompleted) {
            resetFields()
        }
        if (viewState.isImagePickingInProgress) {
            binding.takeImageButton.gone()
            binding.progressImageBar.visible()
            binding.placeImages.setText(R.string.hint_images_loading)
        } else {
            binding.takeImageButton.visible()
            binding.progressImageBar.gone()
            binding.placeImages.setText(viewState.selectedImagesMessage)
        }
        if (viewState.isLocationInProgress) {
            binding.myPositionButton.gone()
            binding.progressLocationBar.visible()
        } else {
            binding.myPositionButton.visible()
            binding.progressLocationBar.gone()
        }
        if (viewState.isSendingInProgress) {
            binding.progressSending.visible()
        } else {
            binding.progressSending.gone()
        }

        binding.placeCategories.isEnabled = !viewState.isSendingInProgress
        binding.placeTitle.isEnabled = !viewState.isSendingInProgress
        binding.placeDescription.isEnabled = !viewState.isSendingInProgress
        binding.placePosition.isEnabled = !viewState.isSendingInProgress
        binding.placeAddress.isEnabled = !viewState.isSendingInProgress
        binding.placeImages.isEnabled = !viewState.isSendingInProgress
        binding.categoryButton.isClickable = !viewState.isSendingInProgress
        binding.placeImages.isClickable = !viewState.isSendingInProgress
        binding.takeImageButton.isClickable = !viewState.isSendingInProgress
        binding.clearButton.isClickable = !viewState.isSendingInProgress
        binding.sendButton.isClickable = !viewState.isSendingInProgress
        binding.myPositionButton.isClickable = !viewState.isSendingInProgress
        binding.progressSending.progress = viewState.sendingProgress
        binding.progressSending.max = viewState.maxProgressValue
    }

    private fun getCategoriesDialog(): Dialog {
        val builder: AlertDialog.Builder = AlertDialog.Builder(requireActivity())
        return builder
            .setTitle(getString(R.string.select_category_alert))
            .setMultiChoiceItems(
                allCategories.map { i -> i.category_name }.toTypedArray(),
                viewModel.checkedCategoriesArray
            ) { _, which, isChecked ->

                val selectedCategory = allCategories[which]

                if (isChecked) {
                    selectedCategory.category_id?.let { viewModel.selectedCategories.add(it) }
                } else {
                    selectedCategory.category_id?.let {
                        viewModel.selectedCategories.remove(
                            it
                        )
                    }
                }
            }
            .setPositiveButton(getString(R.string.ok_item)) { _, _ -> addCategories() }
            .create()
    }
    private fun addCategories() {
        if (viewModel.selectedCategories.isEmpty()) {
            binding.placeCategories.text?.clear()
            return
        }

        binding.placeCategories.text?.clear()

        viewModel.selectedCategories.forEach { i ->
            run {
                val category = allCategories.find { j -> j.category_id == i }?.category_name

                if (!category.isNullOrEmpty()) {
                    binding.placeCategories.text?.append(
                        category,
                        "; "
                    )
                }
            }
        }
    }

    //region Image picking
    private val startForProfileImageResult =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result: ActivityResult ->
            if (result.resultCode == Activity.RESULT_OK) {
                viewModel.onImagePicked(result.data)
            }
            viewModel.onOpenedImagePicker(false)
        }
    //endregion

    // TODO Need refactoring
    private fun validatePlace(): Boolean {

        var validationResult = true

        if (binding.placeTitle.text.isNullOrEmpty()) {
            binding.placeTitleInput.error =
                resources.getString(R.string.required_field_empty_error)
            validationResult = false
        }

        if (binding.placeAddress.text.isNullOrEmpty()) {
            binding.placeAddressInput.error =
                resources.getString(R.string.required_field_empty_error)
            validationResult = false
        }

        return validationResult
    }

    private fun getNewPlace(): Place {
        val placeUUID = UUID.randomUUID().toString()
        val position = binding.placePosition.text.toString().split(" ")
        return Place(
            place_uuid = placeUUID,
            title = binding.placeTitle.text.toString(),
            description = binding.placeDescription.text.toString(),
            latitude = if (position.size > 1) position[0].toDouble() else 54.513845,
            longitude = if (position.size > 1) position[1].toDouble() else 36.261215,
            address = binding.placeAddress.text.toString(),
            categories = viewModel.selectedCategories
        )
    }

    private fun resetFields() {
        viewModel.onResetFields()
        binding.let {
            it.placeTitle.text?.clear()
            it.placeDescription.text?.clear()
            it.placeAddress.text?.clear()
            it.placePosition.text?.clear()
            it.placeImages.text?.clear()
            it.placeCategories.text?.clear()
            it.progressLocationBar.gone()
            it.myPositionButton.visible()
            it.myPositionButton.isEnabled = true
            it.placeTitleInput.error = null
            it.placeAddressInput.error = null
        }
        Toast.makeText(requireActivity(), R.string.success_message, Toast.LENGTH_LONG).show()
    }

    private fun getPosition() {
        getMyPosition()
    }

    private fun openImagePicker() {
        viewModel.onOpenedImagePicker(true)
        imagePicker
            .compress(512)
            .crop(900f, 600f)
            .galleryOnly()
            .setDismissListener {
                viewModel.onOpenedImagePicker(false)
            }
            .createIntent { intent ->
                startForProfileImageResult.launch(intent)
            }
    }

    private fun showCategories() {
        getCategoriesDialog().show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    // TODO Need refactoring
    //region GPS
    private val TAG = "LocationTag"

    private val REQUEST_PERMISSIONS_REQUEST_CODE = 34

    private var fusedLocationClient: FusedLocationProviderClient? = null

    private var lastLocation: Location? = null

    private var addressOutput = ""

    private lateinit var resultReceiver: AddressResultReceiver

    private fun getMyPosition() {
        if (!checkPermissions()) {
            requestPermissions()
        } else {
            if (lastLocation != null) {
                startIntentService()
                return
            }

            // If we have not yet retrieved the user location, we process the user's request by setting
            // addressRequested to true. As far as the user is concerned, pressing the Fetch Address
            // button immediately kicks off the process of getting the address.
            viewModel.onLocationProgressChanged(true)
            getAddress()
        }
    }


    @SuppressLint("MissingPermission")
    private fun getAddress() {
        fusedLocationClient?.lastLocation?.addOnSuccessListener(
            requireActivity(),
            OnSuccessListener { location ->
                if (location == null) {
                    Log.w(TAG, "onSuccess:null")
                    return@OnSuccessListener
                }

                lastLocation = location

                // Determine whether a Geocoder is available.
                if (!Geocoder.isPresent()) {
                    Toast.makeText(
                        requireActivity(),
                        R.string.no_geocoder_available,
                        Toast.LENGTH_LONG
                    ).show()
                    return@OnSuccessListener
                }

                // If the user pressed the fetch address button before we had the location,
                // this will be set to true indicating that we should kick off the intent
                // service after fetching the location.
                if (viewModel.viewState.value?.isLocationInProgress == true) startIntentService()
            })?.addOnFailureListener(requireActivity()) { e ->
            Log.w(
                TAG,
                "getLastLocation:onFailure",
                e
            )
        }
    }

    /**
     * Creates an intent, adds location data to it as an extra, and starts the intent service for
     * fetching an address.
     */
    private fun startIntentService() {
        // Create an intent for passing to the intent service responsible for fetching the address.
        val intent: Intent =
            Intent(requireActivity(), FetchAddressIntentService::class.java).apply {
                // Pass the result receiver as an extra to the service.
                putExtra(Constants.RECEIVER, resultReceiver)

                // Pass the location data as an extra to the service.
                putExtra(Constants.LOCATION_DATA_EXTRA, lastLocation)
            }

        // Start the service. If the service isn't already running, it is instantiated and started
        // (creating a process for it if needed); if it is running then it remains running. The
        // service kills itself automatically once all intents are processed.
        requireActivity().startService(intent)
    }

    /**
     * Receiver for data sent from FetchAddressIntentService.
     */
    private inner class AddressResultReceiver(
        handler: Handler,
    ) : ResultReceiver(handler) {

        /**
         * Receives data sent from FetchAddressIntentService and updates the UI in MainActivity.
         */
        override fun onReceiveResult(resultCode: Int, resultData: Bundle) {

            // Display the address string or an error message sent from the intent service.
            addressOutput = resultData.getString(Constants.RESULT_DATA_KEY).toString()
            displayAddressOutput()

            // Show a toast message if an address was found.
            if (resultCode == Constants.SUCCESS_RESULT) {
                Toast.makeText(requireActivity(), R.string.address_found, Toast.LENGTH_SHORT).show()
            }

            // Reset. Enable the Fetch Address button and stop showing the progress bar.
            viewModel.onLocationProgressChanged(false)
        }
    }

    private fun displayAddressOutput() {
        binding.placeAddress.setText(addressOutput)
        binding.placePosition.text?.clear()
        binding.placePosition.text?.append("${lastLocation?.latitude} ${lastLocation?.longitude}")
    }

    private fun checkPermissions(): Boolean {
        val permissionState = ActivityCompat.checkSelfPermission(
            requireActivity(),
            Manifest.permission.ACCESS_FINE_LOCATION
        )
        return permissionState == PackageManager.PERMISSION_GRANTED
    }

    private fun requestPermissions() {
        val shouldProvideRationale = ActivityCompat.shouldShowRequestPermissionRationale(
            requireActivity(),
            Manifest.permission.ACCESS_FINE_LOCATION
        )

        // Provide an additional rationale to the user. This would happen if the user denied the
        // request previously, but didn't check the "Don't ask again" checkbox.
        if (shouldProvideRationale) {
            Log.i(TAG, "Displaying permission rationale to provide additional context.")

            Toast.makeText(requireActivity(), R.string.permission_rationale, Toast.LENGTH_LONG)
                .show()

            startLocationPermissionRequest()

        } else {
            Log.i(TAG, "Requesting permission")
            // Request permission. It's possible this can be auto answered if device policy
            // sets the permission in a given state or the user denied the permission
            // previously and checked "Never ask again".
            startLocationPermissionRequest()
        }
    }

    private fun startLocationPermissionRequest() {
        ActivityCompat.requestPermissions(
            requireActivity(),
            arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
            REQUEST_PERMISSIONS_REQUEST_CODE
        )
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray,
    ) {
        Log.i(TAG, "onRequestPermissionResult")

        if (requestCode != REQUEST_PERMISSIONS_REQUEST_CODE) return

        when {
            grantResults.isEmpty() ->
                // If user interaction was interrupted, the permission request is cancelled and you
                // receive empty arrays.
                Log.i(TAG, "User interaction was cancelled.")
            grantResults[0] == PackageManager.PERMISSION_GRANTED -> // Permission granted.
                getAddress()
            else -> // Permission denied.
                Toast.makeText(
                    requireActivity(),
                    R.string.permission_denied_explanation,
                    Toast.LENGTH_LONG
                ).show()
        }
    }
    //endregion
}