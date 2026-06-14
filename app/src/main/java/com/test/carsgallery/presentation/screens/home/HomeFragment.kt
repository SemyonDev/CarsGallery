package com.test.carsgallery.presentation.screens.home

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.test.carsgallery.R
import com.test.carsgallery.databinding.FragmentHomeBinding
import com.test.carsgallery.presentation.compose.mvi.ComposeMviActivity
import com.test.carsgallery.presentation.compose.mvvm.ComposeMvvmActivity

/**
 * Entry chooser: three buttons that launch the three parallel implementations of the same gallery.
 *
 * - **XML** stays inside this Navigation-component graph (navigates to [galleryFragment]).
 * - **Compose MVVM** and **Compose MVI** are each self-contained Compose + Navigation 3 worlds,
 *   so they launch as their own activities to keep the two navigation systems cleanly separated.
 */
class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.buttonXml.setOnClickListener {
            findNavController().navigate(R.id.action_home_to_gallery)
        }
        binding.buttonComposeMvvm.setOnClickListener {
            startActivity(Intent(requireContext(), ComposeMvvmActivity::class.java))
        }
        binding.buttonComposeMvi.setOnClickListener {
            startActivity(Intent(requireContext(), ComposeMviActivity::class.java))
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
