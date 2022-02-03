package com.steeshock.android.streetworkout.presentation.views

import android.os.Bundle
import android.view.*
import androidx.appcompat.widget.SearchView
import androidx.fragment.app.viewModels
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.findNavController
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.steeshock.android.streetworkout.R
import com.steeshock.android.streetworkout.common.BaseFragment
import com.steeshock.android.streetworkout.common.MainActivity
import com.steeshock.android.streetworkout.common.appComponent
import com.steeshock.android.streetworkout.data.model.Place
import com.steeshock.android.streetworkout.databinding.FragmentPlacesBinding
import com.steeshock.android.streetworkout.presentation.adapters.CategoryAdapter
import com.steeshock.android.streetworkout.presentation.adapters.PlaceAdapter
import com.steeshock.android.streetworkout.presentation.viewStates.EmptyViewState.*
import com.steeshock.android.streetworkout.presentation.viewStates.PlacesViewState
import com.steeshock.android.streetworkout.presentation.viewmodels.PlacesViewModel
import javax.inject.Inject

class PlacesFragment : BaseFragment() {

    @Inject
    lateinit var factory: ViewModelProvider.Factory

    private val viewModel: PlacesViewModel by viewModels { factory }

    private lateinit var placesAdapter: PlaceAdapter
    private lateinit var categoriesAdapter: CategoryAdapter

    private var _binding: FragmentPlacesBinding? = null
    private val binding get() = _binding!!

    override fun injectComponent() {
        context?.appComponent?.providePlacesComponent()?.inject(this)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPlacesBinding.inflate(inflater, container, false)
        (container?.context as MainActivity).setSupportActionBar(_binding?.toolbar)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        placesAdapter = PlaceAdapter(object : PlaceAdapter.Callback {
                override fun onPlaceClicked(place: Place) {}

                override fun onLikeClicked(place: Place) {
                    viewModel.onLikeClicked(place)
                }

                override fun onPlaceLocationClicked(place: Place) {
                    navigateToMap(place)
                }
            })

        categoriesAdapter = CategoryAdapter {
            viewModel.onFilterByCategory(it)
        }

        binding.fab.setOnClickListener {
            showAddPlaceFragment(it)
        }

        binding.placesRecycler.setHasFixedSize(true)
        binding.placesRecycler.adapter = placesAdapter

        binding.categoriesRecycler.adapter = categoriesAdapter
        binding.categoriesRecycler.layoutManager =
            LinearLayoutManager(binding.root.context, LinearLayoutManager.HORIZONTAL, false)

        setupEmptyViews()
        initData()
    }

    private fun navigateToMap(place: Place) {
        val placeUUID = place.place_uuid
        val action = PlacesFragmentDirections.actionNavigationPlacesToNavigationMap(placeUUID)
        this.findNavController().navigate(action)
    }

    private fun initData() {
        with(viewModel) {

            observablePlaces.observe(viewLifecycleOwner) {
                placesAdapter.setPlaces(it)
            }

            observableCategories.observe(viewLifecycleOwner) {
                categoriesAdapter.setCategories(it)
            }

            viewState.observe(viewLifecycleOwner) {
                renderViewState(it)
            }

            binding.refresher.setOnRefreshListener {
                fetchData(viewModel)
            }
        }
    }

    private fun renderViewState(viewState: PlacesViewState) {
        binding.refresher.isRefreshing = viewState.isLoading

        when (viewState.emptyState) {
            EMPTY_PLACES -> {
                binding.placesRecycler.visibility = View.GONE
                binding.emptyResults.mainLayout.visibility = View.GONE
                binding.emptyList.mainLayout.visibility = View.VISIBLE
            }
            EMPTY_SEARCH_RESULTS -> {
                binding.placesRecycler.visibility = View.GONE
                binding.emptyList.mainLayout.visibility = View.GONE
                binding.emptyResults.mainLayout.visibility = View.VISIBLE
            }
            NOT_EMPTY -> {
                binding.placesRecycler.visibility = View.VISIBLE
                binding.emptyList.mainLayout.visibility = View.GONE
                binding.emptyResults.mainLayout.visibility = View.GONE
            }
        }
    }

    private fun fetchData(placesViewModel: PlacesViewModel) {
        placesViewModel.fetchPlaces()
        placesViewModel.fetchCategories()
    }

    // region Menu
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
                viewModel.filterDataBySearchString(s)
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
                viewModel.clearDatabase()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
    // endregion

    private fun showAddPlaceFragment(it: View) {
        it.findNavController().navigate(R.id.action_navigation_places_to_navigation_add_place)
    }

    private fun setupEmptyViews() {
        binding.emptyList.image.setImageResource(R.drawable.ic_rage_face)
        binding.emptyList.title.setText(R.string.empty_places_list_state_message)

        binding.emptyResults.image.setImageResource(R.drawable.ic_jackie_face)
        binding.emptyResults.title.setText(R.string.empty_state_message)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}