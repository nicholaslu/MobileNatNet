package jp.ac.titech.e.sc.hfg.mobilenatnet

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.fragment.app.setFragmentResult
import androidx.navigation.fragment.findNavController
import jp.ac.titech.e.sc.hfg.mobilenatnet.R
import jp.ac.titech.e.sc.hfg.mobilenatnet.databinding.FragmentSettingBinding

/**
 * A simple [Fragment] subclass as the default destination in the navigation.
 */
class SettingFragment : Fragment() {

    private var _binding: FragmentSettingBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!
    private var natNetSetting = MainActivity.NatNetSetting()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        _binding = FragmentSettingBinding.inflate(inflater, container, false)
        return binding.root

    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.buttonSet.setOnClickListener {
            natNetSetting.clientAddress = binding.localIpInput.text.toString()
            natNetSetting.serverAddress = binding.serverIpInput.text.toString()
            natNetSetting.multicastAddress = binding.multicastIpInput.text.toString()
            natNetSetting.useMulticast = binding.switchMulticast.isChecked
            setFragmentResult("requestKey", bundleOf("bundleKey" to natNetSetting))
            findNavController().navigate(R.id.action_settingFragment_to_monitorFragment)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}