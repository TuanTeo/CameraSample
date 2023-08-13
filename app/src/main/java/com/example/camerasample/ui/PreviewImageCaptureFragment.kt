package com.example.camerasample.ui

import android.graphics.Bitmap
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import com.bumptech.glide.Glide
import com.example.camerasample.databinding.FragmentPreviewImageCaptureBinding

class PreviewImageCaptureFragment(private val bitmap: Bitmap, private val width: Int, private val height: Int): DialogFragment() {

    private lateinit var viewBinding: FragmentPreviewImageCaptureBinding

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View {
        viewBinding = FragmentPreviewImageCaptureBinding.inflate(layoutInflater)

        Glide
            .with(requireContext())
            .load(bitmap)
//            .load(cropBitmap(bitmap, width, height))
            .into(viewBinding.imageCapture)

        viewBinding.imageResolution.text =
            "width:" + bitmap.width.toString() + "\nheight: " + bitmap.height.toString()

        return viewBinding.root
    }

    override fun onStart() {
        super.onStart()
        val width = ViewGroup.LayoutParams.MATCH_PARENT
        val height = ViewGroup.LayoutParams.MATCH_PARENT
        dialog!!.window!!.setLayout(width, height)
    }

    private fun cropBitmap(bitmap: Bitmap, desiredWidth: Int, desiredHeight: Int): Bitmap {
        val width = bitmap.width
        val height = bitmap.height

        // Calculate the aspect ratio of the original image
        val aspectRatio = width.toFloat() / height.toFloat()

        // Calculate the new dimensions based on the desired width and height
        val newWidth: Int
        val newHeight: Int

        if (desiredWidth.toFloat() / desiredHeight.toFloat() > aspectRatio) {
            newWidth = (desiredHeight * aspectRatio).toInt()
            newHeight = desiredHeight
        } else {
            newWidth = desiredWidth
            newHeight = (desiredWidth / aspectRatio).toInt()
        }

        // Calculate the starting coordinates for cropping
        val startX = (width - newWidth) / 2
        val startY = (height - newHeight) / 2

        // Create the cropped Bitmap using the calculated coordinates and dimensions

        return Bitmap.createBitmap(bitmap, startX, startY, newWidth, newHeight)
    }
}