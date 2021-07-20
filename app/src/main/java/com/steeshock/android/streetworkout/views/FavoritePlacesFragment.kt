package com.steeshock.android.streetworkout.views

import android.os.Bundle
import android.view.*
import androidx.appcompat.widget.SearchView
import androidx.appcompat.widget.Toolbar
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.navigation.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.steeshock.android.streetworkout.R
import com.steeshock.android.streetworkout.adapters.PlaceAdapter
import com.steeshock.android.streetworkout.common.BaseFragment
import com.steeshock.android.streetworkout.common.MainActivity
import com.steeshock.android.streetworkout.data.model.Place
import com.steeshock.android.streetworkout.databinding.FragmentFavoritePlacesBinding
import com.steeshock.android.streetworkout.utils.InjectorUtils
import com.steeshock.android.streetworkout.viewmodels.FavoritePlacesViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi

@ExperimentalCoroutinesApi
class FavoritePlacesFragment : BaseFragment() {

    private val favoritePlacesViewModel: FavoritePlacesViewModel by viewModels {
        InjectorUtils.provideFavoritePlacesViewModelFactory(requireActivity())
    }

    private lateinit var placesAdapter: PlaceAdapter
    private lateinit var fragmentFavoritePlacesBinding: FragmentFavoritePlacesBinding

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        fragmentFavoritePlacesBinding = FragmentFavoritePlacesBinding.inflate(inflater, container, false)

        fragmentFavoritePlacesBinding.viewmodel = favoritePlacesViewModel

        fragmentFavoritePlacesBinding.lifecycleOwner = this

        (container?.context as MainActivity).setSupportActionBar(fragmentFavoritePlacesBinding.toolbar)

        return fragmentFavoritePlacesBinding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {

        placesAdapter =
            PlaceAdapter(object :
                PlaceAdapter.Callback {
                override fun onPlaceClicked(item: Place) {
                    showBottomSheet()
                }

                override fun onLikeClicked(item: Place) {
                    removePlaceFromFavorites(item)
                }

                override fun onPlaceLocationClicked(item: Place) {
                    val navController = requireActivity().findNavController(R.id.nav_host_fragment)
                    navController.navigate(R.id.action_navigation_favorites_to_navigation_map)
                }
            })

        favoritePlacesViewModel.allFavoritePlacesLive.observe(viewLifecycleOwner, Observer { places ->
            places?.let { placesAdapter.setPlaces(it) }
        })

        fragmentFavoritePlacesBinding.placesRecycler.adapter = placesAdapter
        fragmentFavoritePlacesBinding.placesRecycler.layoutManager =
            LinearLayoutManager(fragmentFavoritePlacesBinding.root.context)

        super.onViewCreated(view, savedInstanceState)
    }

    private fun removePlaceFromFavorites(place: Place) {
        place.changeFavoriteState()
        favoritePlacesViewModel.insertPlace(place)
    }

    fun showBottomSheet() {
        ItemListDialogFragment.newInstance(30)
            .show((requireActivity() as MainActivity).supportFragmentManager, "detail_place_tag")
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
                filterDataBySearchString(s)
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

    private fun filterDataBySearchString(searchString: String?) {
        placesAdapter.filterItemsBySearchString(searchString)
    }
}