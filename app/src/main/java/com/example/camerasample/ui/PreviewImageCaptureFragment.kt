package com.example.camerasample.ui

import android.graphics.Bitmap
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import com.bumptech.glide.Glide
import com.example.camerasample.databinding.FragmentPreviewImageCaptureBinding

class PreviewImageCaptureFragment(private val bitmap: Bitmap): DialogFragment() {

    private lateinit var viewBinding: FragmentPreviewImageCaptureBinding

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View {
        viewBinding = FragmentPreviewImageCaptureBinding.inflate(layoutInflater)

        Glide
            .with(requireContext())
            .load(bitmap)
            .centerCrop()
            .into(viewBinding.imageCapture)

        viewBinding.imageResolution.text =
            "width:" + bitmap.width.toString() + "/nheight: " + bitmap.height.toString()

        return viewBinding.root
    }

    override fun onStart() {
        super.onStart()
        val width = ViewGroup.LayoutParams.MATCH_PARENT
        val height = ViewGroup.LayoutParams.MATCH_PARENT
        dialog!!.window!!.setLayout(width, height)
    }
}