package com.example.photogallery

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.database.Cursor
import android.database.MatrixCursor
import android.os.Bundle
import android.provider.BaseColumns
import android.util.Log
import android.view.*
import android.view.inputmethod.InputMethodManager
import androidx.appcompat.widget.SearchView
import androidx.browser.customtabs.CustomTabsIntent
import androidx.core.content.ContextCompat
import androidx.cursoradapter.widget.CursorAdapter
import androidx.cursoradapter.widget.SimpleCursorAdapter
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import androidx.work.*
import com.example.photogallery.databinding.FragmentPhotoGalleryBinding
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

private const val POLL_WORK = "POLL_WORK"
private const val TAG: String = "PhotoGalleryFragment"

class PhotoGalleryFragment : Fragment() {

    private val photoGalleryViewModel: PhotoGalleryViewModel by viewModels()
    private lateinit var adapter: PhotoListAdapter
    private var searchView: SearchView? = null
    private var pollingMenuItem: MenuItem? = null
    private var cursorAdapter: SimpleCursorAdapter? = null
    private val suggestions:MutableSet<String> = mutableSetOf<String>()

    private var _binding: FragmentPhotoGalleryBinding? = null
    private val binding
            get() = checkNotNull(_binding){
                "Cannot access binding because it is null. Is the view visible?"
            }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)

    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(R.menu.fragment_photo_gallery, menu)

        val searchItem = menu.findItem(R.id.menu_item_search)
        searchView = searchItem.actionView as SearchView
        pollingMenuItem = menu.findItem(R.id.menu_item_toggle_polling)

        searchView?.apply {
            queryHint = "Type your query"
        }

        cursorAdapter = getAdapter()
        searchView?.suggestionsAdapter = cursorAdapter
        fillDropList()

        searchView?.setOnQueryTextListener(object : SearchView.OnQueryTextListener{
            override fun onQueryTextSubmit(query: String?): Boolean {
                Log.d(TAG, "OnTextSubmit: $query")
                photoGalleryViewModel.setQuery(query ?: "")
                query?.let {
                    suggestions.add(it)
                    fillDropList()
                }
                closeKeyboard()
                return true
            }

            override fun onQueryTextChange(p0: String?): Boolean {
                Log.d(TAG, "OnTextChange: $p0")
                return false
            }
        })

        searchView?.setOnSuggestionListener(object : SearchView.OnSuggestionListener{
            @SuppressLint("Range")
            override fun onSuggestionSelect(position: Int): Boolean {
                return false
            }

            @SuppressLint("Range")
            override fun onSuggestionClick(p0: Int): Boolean {
                val cursor: Cursor = cursorAdapter!!.getItem(p0) as Cursor
                val txt: String = cursor.getString(cursor.getColumnIndex("name"))
                searchView?.setQuery(txt, true)
                return true
            }
        })
    }

    private fun getAdapter(): SimpleCursorAdapter {
        val from = arrayOf("name")
        val to = intArrayOf(R.id.name)
        val cursorAdapter = SimpleCursorAdapter(
            context ,
            R.layout.search_item ,
            null ,
            from ,
            to ,
            CursorAdapter.FLAG_REGISTER_CONTENT_OBSERVER)
        return cursorAdapter;
    }

    private fun fillDropList(){
        val c = MatrixCursor(arrayOf(BaseColumns._ID, "name"))
        for (i in 0 .. suggestions.size -1 ) {
            c.addRow(arrayOf<Any>(i, suggestions.elementAt(i)))
        }
        cursorAdapter?.changeCursor(c)
    }

    private fun closeKeyboard(){
        val imm =
            requireActivity().getSystemService(Activity.INPUT_METHOD_SERVICE) as InputMethodManager
        var view = requireActivity().currentFocus
        if (view == null) {
            view = View(activity)
        }
        imm.hideSoftInputFromWindow(view.windowToken, 0)
    }



    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when(item.itemId){
            R.id.menu_item_clear -> {
                photoGalleryViewModel.setQuery("")
                true
            }
            R.id.menu_item_toggle_polling -> {
                photoGalleryViewModel.toggleIsPooling()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentPhotoGalleryBinding.inflate(inflater, container, false)
        binding.photoGrid.layoutManager = GridLayoutManager(requireContext(), 3)
        return binding.root
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        adapter = PhotoListAdapter(){
            photoPageUri ->
//            val intent = Intent(Intent.ACTION_VIEW, photoPageUri)
//            startActivity(intent) for opening web page in browser

            findNavController().navigate(
                PhotoGalleryFragmentDirections.showPhoto(photoPageUri)
            ) //for web view

//            CustomTabsIntent.Builder()
//                .setToolbarColor(ContextCompat.getColor(
//                    requireContext(), androidx.appcompat.R.color.primary_dark_material_dark
//                ))
//                .setShowTitle(true)
//                .build()
//                .launchUrl(requireContext(), photoPageUri)
        }
        binding.photoGrid.adapter = adapter
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                try{
                    photoGalleryViewModel.uiState.collectLatest {
                            data ->
                            updatePoolingState(data.isPolling)
                            searchView?.setQuery(data.query, false)
                            adapter.submitData(data.images)

                        // цікава поведінка - якщо updatePoolingState буде нижче від усіх функцій,
                        // то вона не буде спрацьовувати при емітах
                    }
                } catch (ex: Exception){
                    Log.e(TAG, "Error receive data", ex)
                }

            }
        }
    }

    private fun updatePoolingState(isPolling: Boolean) {
        Log.i(TAG, "updatePoolingState")
        val toggleItemTitle = if(isPolling){
            R.string.stop_pooling
        } else {
            R.string.start_pooling
        }
        Log.d(TAG, pollingMenuItem.toString())
        pollingMenuItem?.setTitle(toggleItemTitle)

        if(isPolling){
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.UNMETERED)
                .build()
            var periodicReques =
                PeriodicWorkRequestBuilder<PollWorker>(15, TimeUnit.MINUTES)
                    .setConstraints(constraints)
                    .build()

            WorkManager.getInstance(requireContext()).enqueueUniquePeriodicWork(
                POLL_WORK,
                ExistingPeriodicWorkPolicy.KEEP,
                periodicReques
            )
        } else{
          WorkManager.getInstance(requireContext()).cancelUniqueWork(POLL_WORK)
        }
    }


    override fun onDestroy() {
        super.onDestroy()
        _binding = null
    }
}