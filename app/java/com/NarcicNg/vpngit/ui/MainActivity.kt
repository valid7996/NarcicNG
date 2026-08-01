package com.hamedvpn.vpngit.ui

import android.content.Intent
import android.net.VpnService
import android.os.Build
import android.os.Bundle
import android.view.KeyEvent
import android.view.Menu
import android.view.MenuItem
import android.view.animation.AlphaAnimation
import android.view.animation.Animation
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.core.view.GravityCompat
import androidx.lifecycle.lifecycleScope
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.navigation.NavigationView
import com.hamedvpn.vpngit.AppConfig
import com.hamedvpn.vpngit.R
import com.hamedvpn.vpngit.core.CoreServiceManager
import com.hamedvpn.vpngit.databinding.ActivityMainBinding
import com.hamedvpn.vpngit.dto.AutoConnectState
import com.hamedvpn.vpngit.enums.PermissionType
import com.hamedvpn.vpngit.extension.toast
import com.hamedvpn.vpngit.handler.MmkvManager
import com.hamedvpn.vpngit.handler.SettingsChangeManager
import com.hamedvpn.vpngit.handler.SettingsManager
import com.hamedvpn.vpngit.handler.PanelManager
import com.hamedvpn.vpngit.handler.AutoConnectManager
import com.hamedvpn.vpngit.handler.SubscriptionUpdater
import com.hamedvpn.vpngit.util.CountryUtils
import com.hamedvpn.vpngit.util.LogUtil
import com.hamedvpn.vpngit.util.Utils
import com.hamedvpn.vpngit.viewmodel.MainViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class MainActivity : HelperBaseActivity(), NavigationView.OnNavigationItemSelectedListener {
    private val binding by lazy {
        ActivityMainBinding.inflate(layoutInflater)
    }

    val mainViewModel: MainViewModel by viewModels()

    private var pulseAnimation: Animation? = null

    private enum class ConnectButtonStyle { IDLE, TESTING, CONNECTED }

    private val requestVpnPermission = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK) {
            startV2Ray()
        } else {

            mainViewModel.markDisconnected()
            toast(R.string.home_permission_denied)
        }
    }
    private val requestActivityLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        if (SettingsChangeManager.consumeRestartService() && mainViewModel.isRunning.value == true) {
            restartV2Ray()
        }
    }
    private val selectServerLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        val guid = result.data?.getStringExtra(ServerListActivity.EXTRA_SELECTED_GUID)
        if (result.resultCode == RESULT_OK && !guid.isNullOrEmpty()) {
            MmkvManager.setSelectServer(guid)
            mainViewModel.connectToServer(guid)
            if (mainViewModel.isRunning.value == true) {
                restartV2Ray()
            } else {
                startV2RayWithPermissionCheck()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        setupToolbar(binding.toolbar, false, getString(R.string.app_name))

        setupNavigationDrawer()

        binding.cardLocation.setOnClickListener {
            selectServerLauncher.launch(Intent(this, ServerListActivity::class.java))
        }
        binding.layoutConnectButton.setOnClickListener { handleConnectButtonClick() }
        binding.cardBottomStatus.setOnClickListener {
            if (mainViewModel.autoConnectState.value !is AutoConnectState.Connecting) {
                mainViewModel.startAutoConnectFlow()
            }
        }

        setupViewModel()
        SubscriptionUpdater.sync()

        checkAndRequestPermission(PermissionType.POST_NOTIFICATIONS) {}
        checkMaintenanceMode()
    }

    


    private fun checkMaintenanceMode() {
        if (!PanelManager.isPanelConfigured()) return

        lifecycleScope.launch {
            try {
                val isMaintenance = PanelManager.checkMaintenanceMode()
                if (isMaintenance) {
                    MaterialAlertDialogBuilder(this@MainActivity)
                        .setTitle(getString(R.string.app_name))
                        .setMessage(R.string.maintenance_mode_message)
                        .setPositiveButton(android.R.string.ok, null)
                        .show()
                }
            } catch (e: Exception) {
                LogUtil.e(AppConfig.TAG, "Failed to check maintenance mode", e)
            }
        }
    }

    private fun setupNavigationDrawer() {
        val toggle = ActionBarDrawerToggle(
            this,
            binding.drawerLayout,
            binding.toolbar,
            R.string.navigation_drawer_open,
            R.string.navigation_drawer_close
        )
        binding.drawerLayout.addDrawerListener(toggle)
        toggle.syncState()
        binding.navView.setNavigationItemSelectedListener(this)

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (binding.drawerLayout.isDrawerOpen(GravityCompat.START)) {
                    binding.drawerLayout.closeDrawer(GravityCompat.START)
                } else {
                    isEnabled = false
                    onBackPressedDispatcher.onBackPressed()
                    isEnabled = true
                }
            }
        })
    }

    private fun setupViewModel() {
        mainViewModel.isRunning.observe(this) { running ->
            if (running == true) {

                mainViewModel.refreshConnectedState()
            } else {

                val state = mainViewModel.autoConnectState.value
                if (state is AutoConnectState.Connected || state is AutoConnectState.Connecting) {
                    mainViewModel.markDisconnected()
                }
            }
        }
        mainViewModel.autoConnectState.observe(this) { renderHomeUi(it) }
        mainViewModel.requestStartVpn.observe(this) {
            if (mainViewModel.isRunning.value == true) {
                restartV2Ray()
            } else {
                startV2RayWithPermissionCheck()
            }
        }
        mainViewModel.startListenBroadcast()
        mainViewModel.initAssets(assets)
    }

    private fun handleConnectButtonClick() {
        val currentState = mainViewModel.autoConnectState.value
        if (currentState is AutoConnectState.Connecting || currentState is AutoConnectState.Testing) {

            return
        }

        if (mainViewModel.isRunning.value == true) {
            CoreServiceManager.stopVService(this)
            mainViewModel.markDisconnected()
            return
        }

        val guid = MmkvManager.getSelectServer()
        if (!guid.isNullOrEmpty()) {
            mainViewModel.connectToServer(guid)
            startV2RayWithPermissionCheck()
        } else if (!AutoConnectManager.isPanelConfigured()) {
            toast(R.string.server_list_empty)
        } else {
            mainViewModel.startAutoConnectFlow()
        }
    }

    private fun startV2RayWithPermissionCheck() {
        if (SettingsManager.isVpnMode()) {
            val intent = VpnService.prepare(this)
            if (intent == null) {
                startV2Ray()
            } else {
                requestVpnPermission.launch(intent)
            }
        } else {
            startV2Ray()
        }
    }

    private fun startV2Ray() {
        if (MmkvManager.getSelectServer().isNullOrEmpty()) {
            toast(R.string.home_no_server_selected)
            return
        }

        if (Build.VERSION.SDK_INT >= 36 && MmkvManager.decodeSettingsBool(AppConfig.PREF_PROXY_SHARING)) {
            checkAndRequestPermission(PermissionType.ACCESS_LOCAL_NETWORK) {}
        }

        CoreServiceManager.startVService(this)
    }

    fun restartV2Ray() {
        if (mainViewModel.isRunning.value == true) {
            CoreServiceManager.stopVService(this)
        }
        lifecycleScope.launch {
            delay(500)
            startV2Ray()
        }
    }

    private fun renderHomeUi(state: AutoConnectState) {
        when (state) {
            is AutoConnectState.Connected -> showConnectedUi(state.guid, state.delayMillis)
            is AutoConnectState.Testing -> showTestingUi(state)
            AutoConnectState.Connecting -> showConnectingUi()
            AutoConnectState.AllFailed -> showAllFailedUi()
            AutoConnectState.Idle -> showIdleUi()
        }
    }

    private fun showConnectedUi(guid: String, delayMillis: Long) {
        setConnectButtonStyle(ConnectButtonStyle.CONNECTED)
        binding.tvConnectLabel.text = getString(R.string.home_connected)
        binding.tvConnectSub.text = getString(R.string.home_ping_ms, delayMillis)
        binding.tvStatus.text = getString(R.string.home_tap_to_disconnect)

        val profile = MmkvManager.decodeServerConfig(guid)
        val (flag, name) = CountryUtils.countryFromRemarks(profile?.remarks)
        binding.tvLocationFlag.text = flag ?: CountryUtils.UNKNOWN_FLAG
        binding.tvLocationName.text = name ?: getString(R.string.home_unknown_location)

        binding.tvStatusIcon.text = getString(R.string.home_icon_connected)
        binding.tvStatusTitle.text = getString(R.string.home_connected_title)
        binding.tvStatusSubtitle.text = name ?: getString(R.string.home_unknown_location)
        binding.tvStatusCount.text = getString(R.string.home_ping_ms, delayMillis)
    }

    private fun showTestingUi(state: AutoConnectState.Testing) {
        setConnectButtonStyle(ConnectButtonStyle.TESTING)
        binding.tvConnectLabel.text = getString(R.string.home_connecting)
        binding.tvConnectSub.text = ""
        binding.tvStatus.text = getString(R.string.home_please_wait)

        binding.tvStatusIcon.text = getString(R.string.home_icon_testing)
        binding.tvStatusTitle.text = getString(R.string.home_testing_title)
        binding.tvStatusSubtitle.text = getString(R.string.home_testing_subtitle)
        binding.tvStatusCount.text = if (state.total > 0) "${state.testedCount}/${state.total}" else "…"
    }

    private fun showConnectingUi() {
        setConnectButtonStyle(ConnectButtonStyle.TESTING)
        binding.tvConnectLabel.text = getString(R.string.home_connecting)
        binding.tvConnectSub.text = ""
        binding.tvStatus.text = getString(R.string.home_connecting_subtitle)

        binding.tvStatusIcon.text = getString(R.string.home_icon_testing)
        binding.tvStatusTitle.text = getString(R.string.home_connecting_title)
        binding.tvStatusSubtitle.text = getString(R.string.home_connecting_subtitle)
        binding.tvStatusCount.text = ""
    }

    private fun showAllFailedUi() {
        setConnectButtonStyle(ConnectButtonStyle.IDLE)
        binding.tvConnectLabel.text = getString(R.string.home_connect)
        binding.tvConnectSub.text = ""
        binding.tvStatus.text = getString(R.string.home_all_failed)

        binding.tvStatusIcon.text = getString(R.string.home_icon_warning)
        binding.tvStatusTitle.text = getString(R.string.home_all_failed_title)
        binding.tvStatusSubtitle.text = getString(R.string.home_tap_to_retry)
        binding.tvStatusCount.text = ""
    }

    private fun showIdleUi() {
        setConnectButtonStyle(ConnectButtonStyle.IDLE)
        binding.tvConnectLabel.text = getString(R.string.home_connect)
        binding.tvConnectSub.text = ""
        binding.tvStatus.text = getString(R.string.home_tap_to_connect)

        val guid = MmkvManager.getSelectServer()
        val profile = guid?.let { MmkvManager.decodeServerConfig(it) }
        if (profile != null) {
            val (flag, name) = CountryUtils.countryFromRemarks(profile.remarks)
            binding.tvLocationFlag.text = flag ?: CountryUtils.UNKNOWN_FLAG
            binding.tvLocationName.text = name ?: getString(R.string.home_unknown_location)
        } else {
            binding.tvLocationFlag.text = "🌐"
            binding.tvLocationName.text = getString(R.string.home_choose_location)
        }

        binding.tvStatusIcon.text = getString(R.string.home_icon_idle)
        binding.tvStatusTitle.text = getString(R.string.home_idle_title)
        binding.tvStatusSubtitle.text = getString(R.string.home_idle_subtitle)
        binding.tvStatusCount.text = ""
    }

    private fun setConnectButtonStyle(style: ConnectButtonStyle) {
        when (style) {
            ConnectButtonStyle.IDLE -> {
                binding.layoutConnectButton.setBackgroundResource(R.drawable.bg_connect_button_idle)
                stopPulse()
            }

            ConnectButtonStyle.TESTING -> {
                binding.layoutConnectButton.setBackgroundResource(R.drawable.bg_connect_button_testing)
                startPulse()
            }

            ConnectButtonStyle.CONNECTED -> {
                binding.layoutConnectButton.setBackgroundResource(R.drawable.bg_connect_button_connected)
                stopPulse()
            }
        }
    }

    private fun startPulse() {
        if (pulseAnimation != null) return
        val anim = AlphaAnimation(0.55f, 1.0f).apply {
            duration = 700
            repeatMode = Animation.REVERSE
            repeatCount = Animation.INFINITE
        }
        pulseAnimation = anim
        binding.layoutConnectButton.startAnimation(anim)
    }

    private fun stopPulse() {
        if (pulseAnimation != null) {
            binding.layoutConnectButton.clearAnimation()
            pulseAnimation = null
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem) = when (item.itemId) {
        R.id.retry_servers -> {
            if (mainViewModel.autoConnectState.value !is AutoConnectState.Connecting) {
                mainViewModel.startAutoConnectFlow()
            }
            true
        }

        else -> super.onOptionsItemSelected(item)
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        if (keyCode == KeyEvent.KEYCODE_BACK || keyCode == KeyEvent.KEYCODE_BUTTON_B) {
            moveTaskToBack(false)
            return true
        }
        return super.onKeyDown(keyCode, event)
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.per_app_proxy_settings -> requestActivityLauncher.launch(Intent(this, PerAppProxyActivity::class.java))
            R.id.user_asset_setting -> requestActivityLauncher.launch(Intent(this, UserAssetActivity::class.java))
            R.id.settings -> requestActivityLauncher.launch(Intent(this, SettingsActivity::class.java))
            R.id.about -> startActivity(Intent(this, AboutActivity::class.java))
        }

        binding.drawerLayout.closeDrawer(GravityCompat.START)
        return true
    }
}
