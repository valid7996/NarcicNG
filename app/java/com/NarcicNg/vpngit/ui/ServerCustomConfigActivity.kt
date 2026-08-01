package com.hamedvpn.vpngit.ui

import android.os.Bundle
import android.text.TextUtils
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.app.AlertDialog
import com.blacksquircle.ui.editorkit.utils.EditorTheme
import com.blacksquircle.ui.language.json.JsonLanguage
import com.hamedvpn.vpngit.AppConfig
import com.hamedvpn.vpngit.R
import com.hamedvpn.vpngit.databinding.ActivityServerCustomConfigBinding
import com.hamedvpn.vpngit.dto.entities.ProfileItem
import com.hamedvpn.vpngit.enums.EConfigType
import com.hamedvpn.vpngit.extension.toast
import com.hamedvpn.vpngit.extension.toastSuccess
import com.hamedvpn.vpngit.fmt.CustomFmt
import com.hamedvpn.vpngit.handler.AngConfigManager
import com.hamedvpn.vpngit.handler.MmkvManager
import com.hamedvpn.vpngit.handler.SettingsChangeManager
import com.hamedvpn.vpngit.util.LogUtil
import com.hamedvpn.vpngit.util.Utils

class ServerCustomConfigActivity : BaseActivity() {
    private val binding by lazy { ActivityServerCustomConfigBinding.inflate(layoutInflater) }

    private val editGuid by lazy { intent.getStringExtra("guid").orEmpty() }
    private val isRunning by lazy {
        intent.getBooleanExtra("isRunning", false)
                && editGuid.isNotEmpty()
                && editGuid == MmkvManager.getSelectServer()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentViewWithToolbar(binding.root, showHomeAsUp = true, title = EConfigType.CUSTOM.toString())

        if (!Utils.getDarkModeStatus(this)) {
            // binding.editor.colorScheme = ...
        }
        binding.editor.language = JsonLanguage()
        val config = MmkvManager.decodeServerConfig(editGuid)
        if (config != null) {
            bindingServer(config)
        } else {
            clearServer()
        }
    }

    
    private fun bindingServer(config: ProfileItem): Boolean {
        binding.etRemarks.text = Utils.getEditable(config.remarks)
        val raw = MmkvManager.decodeServerRaw(editGuid)
        val configContent = raw.orEmpty()

        binding.editor.setTextContent(Utils.getEditable(configContent))
        return true
    }

    
    private fun clearServer(): Boolean {
        binding.etRemarks.text = null
        return true
    }

    
    private fun saveServer(): Boolean {
        if (TextUtils.isEmpty(binding.etRemarks.text.toString())) {
            toast(R.string.server_lab_remarks)
            return false
        }

        val profileItem = try {
            CustomFmt.parse(binding.editor.text.toString())
        } catch (e: Exception) {
            LogUtil.e(AppConfig.TAG, "Failed to parse custom configuration", e)
            toast("${getString(R.string.toast_malformed_josn)} ${e.cause?.message}")
            return false
        }

        val config = MmkvManager.decodeServerConfig(editGuid) ?: ProfileItem.create(EConfigType.CUSTOM)
        binding.etRemarks.text.let {
            config.remarks = if (it.isNullOrEmpty()) profileItem?.remarks.orEmpty() else it.toString()
        }
        config.server = profileItem?.server
        config.serverPort = profileItem?.serverPort
        config.description = AngConfigManager.generateDescription(config)

        MmkvManager.encodeServerConfig(editGuid, config)
        MmkvManager.encodeServerRaw(editGuid, binding.editor.text.toString())
        if (isRunning) {
            SettingsChangeManager.makeRestartService()
        }
        toastSuccess(R.string.toast_success)
        finish()
        return true
    }

    
    private fun deleteServer(): Boolean {
        if (editGuid.isNotEmpty()) {
            AlertDialog.Builder(this).setMessage(R.string.del_config_comfirm)
                .setPositiveButton(android.R.string.ok) { _, _ ->
                    MmkvManager.removeServer(editGuid)
                    finish()
                }
                .setNegativeButton(android.R.string.cancel) { _, _ ->

                }
                .show()
        }
        return true
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.action_server, menu)

        val delButton = menu.findItem(R.id.del_config)
        delButton?.isVisible = editGuid.isNotEmpty() && !isRunning

        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem) = when (item.itemId) {
        R.id.del_config -> {
            deleteServer()
            true
        }

        R.id.save_config -> {
            saveServer()
            true
        }

        else -> super.onOptionsItemSelected(item)
    }
}

