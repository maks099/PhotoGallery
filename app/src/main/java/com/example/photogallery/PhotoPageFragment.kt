package com.example.photogallery

import android.annotation.SuppressLint
import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.navArgs
import com.example.photogallery.databinding.FragmentPhotoPageBinding

private const val TAG = "PhotoPageFragment"

class PhotoPageFragment : Fragment() {

    private val args: PhotoPageFragmentArgs by navArgs()
    private var _binding: FragmentPhotoPageBinding? = null
    private val binding
        get() = checkNotNull(_binding)
    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreateView(
        inflater: LayoutInflater ,
        container: ViewGroup? ,
        savedInstanceState: Bundle?
    ): View? {
         _binding = FragmentPhotoPageBinding.inflate(
            inflater,
            container,
            false
        )
        binding.apply {
            progressBar.max = 100
            webView.apply {
                settings.javaScriptEnabled = true
                webViewClient = WebViewClient()
                loadUrl(args.photoPageUri.toString())

                webChromeClient = object  : WebChromeClient(){
                    override fun onProgressChanged(view: WebView? , newProgress: Int) {
                        if(newProgress == 100){
                            progressBar.visibility = View.GONE
                        } else{
                            progressBar.apply {
                                visibility = View.VISIBLE
                                progress = newProgress
                            }
                        }
                    }

                    override fun onReceivedTitle(view: WebView? , title: String?) {
                        super.onReceivedTitle(view , title)
                        val parent = requireActivity() as AppCompatActivity
                        parent.supportActionBar?.subtitle = title
                    }
                }
            }
        }

        val webView = binding.webView
        val callback = object:OnBackPressedCallback(true){
            override fun handleOnBackPressed(){
                if(webView.canGoBack()){
                    webView.goBack()
                } else {
                    this.remove();
                    requireActivity().onBackPressed();
                }
            }
        }

        requireActivity().onBackPressedDispatcher.addCallback(
            viewLifecycleOwner,
            callback
        )
        return binding.root
    }


}