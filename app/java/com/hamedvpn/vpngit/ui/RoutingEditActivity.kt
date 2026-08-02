package com.hamedvpn.vpngit.ui

import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.ArrayAdapter
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.lifecycleScope
import com.hamedvpn.vpngit.AppConfig.BUILTIN_OUTBOUND_TAGS
import com.hamedvpn.vpngit.AppConfig.TAG_PROXY
import com.hamedvpn.vpngit.R
import com.hamedvpn.vpngit.databinding.ActivityRoutingEditBinding
import com.hamedvpn.vpngit.dto.entities.RulesetItem
import com.hamedvpn.vpngit.extension.nullIfBlank
import com.hamedvpn.vpngit.extension.toast
import com.hamedvpn.vpngit.extension.toastSuccess
import com.hamedvpn.vpngit.handler.SettingsManager
import com.hamedvpn.vpngit.util.Utils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class RoutingEditActivity : BaseActivity() {
    private val binding by lazy { ActivityRoutingEditBinding.inflate(layoutInflater) }
    private val position by lazy { intent.getIntExtra("position", -1) }
    private val processPickerLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK) {
            val selectedPackages = AppPickerActivity.getSelectedPackages(result.data)
            binding.etProcess.text = Utils.getEditable(selectedPackages.joinToString(","))
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentViewWithToolbar(binding.root, showHomeAsUp = true, title = getString(R.string.routing_settings_rule_title))

        setupOutboundTagInput()
        setupProcessPicker()

        val rulesetItem = SettingsManager.getRoutingRuleset(position)
        if (rulesetItem != null) {
            bindingServer(rulesetItem)
        } else {
            clearServer()
        }

        SettingsManager.canUseProcessRouting().let { canUse ->
            binding.etProcess.isEnabled = canUse
            binding.btnProcessPicker.isEnabled = canUse
        }
    }

    private fun setupProcessPicker() {
        binding.btnProcessPicker.setOnClickListener {
            processPickerLauncher.launch(
                AppPickerActivity.createIntent(
                    context = this,
                    selectedPackages = getSelectedProcessPackages(),
                    title = getString(R.string.routing_settings_process)
                )
            )
        }
    }

    private fun getSelectedProcessPackages(): List<String> {
        return binding.etProcess.text
            .toString()
            .split(",")
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .distinct()
    }

    
    private fun setupOutboundTagInput() {
        val profileRemarks = SettingsManager.getProfileRemarks()

        val suggestions = (BUILTIN_OUTBOUND_TAGS.toList() + profileRemarks).distinct()
        val adapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, suggestions)
        binding.spOutboundTag.setAdapter(adapter)

        binding.spOutboundTag.threshold = 0

        binding.btnOutboundTagDropdown.setOnClickListener {
            binding.spOutboundTag.requestFocus()
            binding.spOutboundTag.showDropDown()
        }

        binding.spOutboundTag.setOnClickListener {
            binding.spOutboundTag.showDropDown()
        }
    }

    private fun bindingServer(rulesetItem: RulesetItem): Boolean {
        binding.etRemarks.text = Utils.getEditable(rulesetItem.remarks)
        binding.chkLocked.isChecked = rulesetItem.locked == true
        binding.etDomain.text = Utils.getEditable(rulesetItem.domain?.joinToString(","))
        binding.etIp.text = Utils.getEditable(rulesetItem.ip?.joinToString(","))
        binding.etProcess.text = Utils.getEditable(rulesetItem.process?.joinToString(","))
        binding.etPort.text = Utils.getEditable(rulesetItem.port)
        binding.etProtocol.text = Utils.getEditable(rulesetItem.protocol?.joinToString(","))
        binding.etNetwork.text = Utils.getEditable(rulesetItem.network)

        binding.spOutboundTag.setText(rulesetItem.outboundTag, false)
        return true
    }

    private fun clearServer(): Boolean {
        binding.etRemarks.text = null
        binding.spOutboundTag.setText(BUILTIN_OUTBOUND_TAGS.first(), false)
        return true
    }

    private fun saveServer(): Boolean {
        val rulesetItem = SettingsManager.getRoutingRuleset(position) ?: RulesetItem()

        rulesetItem.apply {
            remarks = binding.etRemarks.text.toString()
            locked = binding.chkLocked.isChecked
            domain = binding.etDomain.text.toString().nullIfBlank()?.split(",")?.map { it.trim() }?.filter { it.isNotEmpty() }
            ip = binding.etIp.text.toString().nullIfBlank()?.split(",")?.map { it.trim() }?.filter { it.isNotEmpty() }
            process = binding.etProcess.text.toString().nullIfBlank()?.split(",")?.map { it.trim() }?.filter { it.isNotEmpty() }
            protocol = binding.etProtocol.text.toString().nullIfBlank()?.split(",")?.map { it.trim() }?.filter { it.isNotEmpty() }
            port = binding.etPort.text.toString().nullIfBlank()
            network = binding.etNetwork.text.toString().nullIfBlank()
            outboundTag = binding.spOutboundTag.text.toString().trim().ifEmpty { TAG_PROXY }
        }

        if (rulesetItem.remarks.isNullOrEmpty()) {
            toast(R.string.sub_setting_remarks)
            return false
        }

        SettingsManager.saveRoutingRuleset(position, rulesetItem)
        toastSuccess(R.string.toast_success)
        finish()
        return true
    }

    private fun deleteServer(): Boolean {
        if (position >= 0) {
            AlertDialog.Builder(this).setMessage(R.string.del_config_comfirm)
                .setPositiveButton(android.R.string.ok) { _, _ ->
                    lifecycleScope.launch(Dispatchers.IO) {
                        SettingsManager.removeRoutingRuleset(position)
                        launch(Dispatchers.Main) {
                            finish()
                        }
                    }
                }
                .setNegativeButton(android.R.string.cancel) { _, _ ->

                }
                .show()
        }
        return true
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.action_server, menu)
        val delConfig = menu.findItem(R.id.del_config)

        if (position < 0) {
            delConfig?.isVisible = false
        }

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

