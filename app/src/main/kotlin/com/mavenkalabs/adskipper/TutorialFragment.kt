package com.mavenkalabs.adskipper

import android.content.res.Configuration
import android.media.MediaPlayer
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.net.toUri
import androidx.fragment.app.DialogFragment
import com.mavenkalabs.adskipper.databinding.FragmentTutorialBinding

class TutorialFragment : DialogFragment() {
    private var binding: FragmentTutorialBinding? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentTutorialBinding.inflate(inflater, container, false)
        val nightMode =
            (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES

        binding?.videoTutorial?.setVideoURI(
            ("android.resource://"
                    + requireContext().packageName
                    + "/"
                    + (if (nightMode) R.raw.tutorial_dark else R.raw.tutorial_light)).toUri()
        )
        binding?.videoTutorial?.start()
        binding?.videoTutorial?.setOnCompletionListener { player: MediaPlayer? -> dismiss() }

        return binding?.getRoot()
    }


    override fun onDestroyView() {
        super.onDestroyView()
        binding = null
    }

    companion object {
        val TAG: String = TutorialFragment::class.java.getSimpleName()
    }
}
