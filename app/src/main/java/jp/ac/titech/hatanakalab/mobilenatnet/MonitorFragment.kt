package jp.ac.titech.hatanakalab.mobilenatnet

import NatNetClient
import android.os.AsyncTask
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.setFragmentResultListener
import com.google.android.material.snackbar.Snackbar
import jp.ac.titech.hatanakalab.mobilenatnet.databinding.FragmentMonitorBinding


/**
 * A simple [Fragment] subclass as the second destination in the navigation.
 */
class MonitorFragment : Fragment() {

    private var _binding: FragmentMonitorBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!
    private lateinit var natNetSetting: MainActivity.NatNetSetting
    private val rigidBodyText = RigidBodyText()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Use the Kotlin extension in the fragment-ktx artifact
        setFragmentResultListener("requestKey") { _, bundle ->
            natNetSetting = bundle.getParcelable("bundleKey")!!
        }
    }


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        _binding = FragmentMonitorBinding.inflate(inflater, container, false)
        rigidBodyText.bindTextView(binding.textMonitor)
        return binding.root

    }

    @OptIn(ExperimentalUnsignedTypes::class)
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.buttonStart.setOnClickListener {
            val natNetClientThread = Thread {
                val streamingClient = NatNetClient()
                streamingClient.localIpAddress = natNetSetting.clientAddress
                streamingClient.serverIpAddress = natNetSetting.serverAddress
                streamingClient.useMulticast = natNetSetting.useMulticast
                streamingClient.multicastAddress = natNetSetting.multicastAddress
                streamingClient.rigidBodyListener =
                    { id: Int, pos: ArrayList<Double>, rot: ArrayList<Double> ->
                        rigidBodyText.getRigidBody(id, pos, rot)
                    }
                streamingClient.run()
            }.start()
            Snackbar.make(
                view,
                "Connect with IP setting ${natNetSetting.clientAddress}, ${natNetSetting.serverAddress}, ${natNetSetting.multicastAddress}, use multicast: ${natNetSetting.useMulticast}",
                Snackbar.LENGTH_LONG
            ).setAnchorView(R.id.fab).show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    @OptIn(ExperimentalUnsignedTypes::class)
    private fun printConfiguration(natNetClient: NatNetClient) {
        println("Connection Configuration:")
        println("  Client:          %s".format(natNetClient.localIpAddress))
        println("  Server:          %s".format(natNetClient.serverIpAddress))
        println("  Command Port:    %d".format(natNetClient.commandPort))
        println("  Data Port:       %d".format(natNetClient.dataPort))

        if (natNetClient.useMulticast) {
            println("  Using Multicast")
            println("  Multicast Group: %s".format(natNetClient.multicastAddress))
        } else {
            println("  Using Unicast")
        }

//        NatNet Server Info
        val applicationName = natNetClient.getApplicationName()
        val natNetRequestedVersion = natNetClient.getNatNetRequestedVersion()
        val natNetVersionServer = natNetClient.getNatNetVersionServer()
        val serverVersion = natNetClient.getServerVersion()

        println("  NatNet Server Info")
        println("    Application Name %s".format(applicationName))
        println(
            "    NatNetVersion  %d %d %d %d".format(
                natNetVersionServer[0],
                natNetVersionServer[1],
                natNetVersionServer[2],
                natNetVersionServer[3]
            )
        )
        println(
            "    ServerVersion  %d %d %d %d".format(
                serverVersion[0],
                serverVersion[1],
                serverVersion[2],
                serverVersion[3]
            )
        )
        println("  NatNet Bitstream Requested")
        println(
            "    NatNetVersion  %d %d %d %d".format(
                natNetRequestedVersion[0], natNetRequestedVersion[1],
                natNetRequestedVersion[2], natNetRequestedVersion[3]
            )
        )
    }

    private data class RigidBodyData(
        var id: Int,
        var name: String?,
        var pos: ArrayList<Double>,
        var rot: ArrayList<Double>,
    )

    class RigidBodyText {
        private var rigidBodyMap = mutableMapOf<Int, RigidBodyData>()
        lateinit var text: TextView

        fun bindTextView(textView: TextView) {
            text = textView
        }

        fun getRigidBody(newId: Int, pos: ArrayList<Double>, rot: ArrayList<Double>) {
            if (newId in rigidBodyMap.keys) {
                rigidBodyMap[newId]?.pos = pos
                rigidBodyMap[newId]?.rot = rot
                Log.d("NatNet", "Update map")
            } else if (newId !in rigidBodyMap.keys) {
                val rigidBodyData = RigidBodyData(newId, getNameFromId(newId), pos, rot)
                rigidBodyMap[newId] = rigidBodyData
                Log.d("NatNet", "Create map, newId: $newId, dict: ${rigidBodyMap.keys}")
            }
            showAsText()
        }

        private fun getNameFromId(id: Int): String {
            return id.toString()
        }

        private fun showAsText() {
            var outStr = ""
            for (i in rigidBodyMap.keys) {
                val data = rigidBodyMap[i]
                outStr += data?.let {
                    "%s, id: %2d, position: %2.2f, %2.2f, %2.2f, rotation: %2.2f, %2.2f, %2.2f\n".format(
                        it.name, it.id,
                        it.pos[0], it.pos[1], it.pos[2],
                        it.rot[0], it.rot[1], it.rot[2]
                    )
                }
            }
            Log.d("NatNet", outStr.length.toString())
            val mainHandler = Handler(Looper.getMainLooper())
            try {
                mainHandler.post { text.text = outStr }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}