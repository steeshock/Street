package com.steeshock.android.streetworkout.views

import android.os.Bundle
import android.view.*
import androidx.appcompat.widget.SearchView
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.navigation.fragment.navArgs
import com.steeshock.android.streetworkout.R
import com.steeshock.android.streetworkout.common.BaseFragment
import com.steeshock.android.streetworkout.common.MainActivity
import com.steeshock.android.streetworkout.databinding.FragmentMapBinding
import com.steeshock.android.streetworkout.utils.InjectorUtils
import com.steeshock.android.streetworkout.viewmodels.MapViewModel
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.gms.maps.model.MarkerOptions
import com.steeshock.android.streetworkout.data.model.Place
import kotlinx.coroutines.ExperimentalCoroutinesApi


@ExperimentalCoroutinesApi
class MapFragment : BaseFragment(), OnMapReadyCallback {
    private val mapViewModel: MapViewModel by viewModels {
        InjectorUtils.provideMapViewModelFactory(requireActivity())
    }

    //private val args: MapFragmentArgs by navArgs()

    private lateinit var mMap: GoogleMap

    private lateinit var fragmentMapBinding: FragmentMapBinding

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        fragmentMapBinding = FragmentMapBinding.inflate(inflater, container, false)

        val mapFragment = childFragmentManager.findFragmentById(R.id.map) as SupportMapFragment?

        fragmentMapBinding.viewmodel = mapViewModel

        (container?.context as MainActivity).setSupportActionBar(fragmentMapBinding.toolbar)

        mapFragment?.getMapAsync(this)

        return fragmentMapBinding.root
    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap

        mapViewModel.allPlacesLive.observe(viewLifecycleOwner, Observer { places ->
            places?.let { showAllPlaces(it) }
        })

        val placeId = arguments?.get("place_id")

        if (placeId != null && placeId != -1 && placeId is Int){
            moveToPointLocation(placeId)
        }
    }

    private fun moveToPointLocation(placeId: Int) {

        val place = mapViewModel.allPlacesLive.value?.find { i -> i.place_id == placeId }

        if (place != null){
            val placeLocation = LatLng(place.latitude, place.longitude)
            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(placeLocation, 10f))
        }
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.activity_menu, menu)

        val myActionMenuItem = menu.findItem(R.id.action_search)

        val searchView = myActionMenuItem.actionView as SearchView
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String): Boolean {
                if (!searchView.isIconified) {
                    searchView.isIconified = true
                }
                myActionMenuItem.collapseActionView()
                return false
            }

            override fun onQueryTextChange(s: String?): Boolean {
                //Здесь слушаем именение текста
                return false
            }
        })

        super.onCreateOptionsMenu(menu, inflater)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_search -> {
                true
            }
            R.id.action_map -> {
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun showAllPlaces(places: List<Place>) {
        if (places.isNotEmpty()) {
            lateinit var pin: LatLng
            lateinit var bounds: LatLngBounds
            val pinsPositions = mutableListOf<LatLng>()
            val builder = LatLngBounds.Builder()

            for (place in places) {
                pin = LatLng(place.latitude, place.longitude)
                mMap.addMarker(MarkerOptions().position(pin).title(place.title))
                pinsPositions.add(pin)
            }

            for (position in pinsPositions) {
                builder.include(position)
            }

            bounds = builder.build()

            mMap.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, 12))
        }
    }
}
