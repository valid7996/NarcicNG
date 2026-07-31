package com.hamedvpn.vpngit.ui

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.hamedvpn.vpngit.AppConfig
import com.hamedvpn.vpngit.R
import com.hamedvpn.vpngit.databinding.ActivityServerListBinding
import com.hamedvpn.vpngit.databinding.ItemServerResultBinding
import com.hamedvpn.vpngit.handler.AutoConnectManager
import com.hamedvpn.vpngit.handler.MmkvManager
import com.hamedvpn.vpngit.util.CountryUtils
import com.hamedvpn.vpngit.util.Utils

class ServerListActivity : BaseActivity() {

    private val binding by lazy { ActivityServerListBinding.inflate(layoutInflater) }
    private val adapter = ResultAdapter { guid ->
        setResult(RESULT_OK, Intent().putExtra(EXTRA_SELECTED_GUID, guid))
        finish()
    }

    private val mReceiver = object : BroadcastReceiver() {
        override fun onReceive(ctx: Context?, intent: Intent?) {
            when (intent?.getIntExtra("key", 0)) {
                AppConfig.MSG_MEASURE_CONFIG_SUCCESS, AppConfig.MSG_MEASURE_CONFIG_FINISH -> reload()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentViewWithToolbar(binding.root, showHomeAsUp = true, title = getString(R.string.server_list_title))

        binding.recyclerView.layoutManager = LinearLayoutManager(this)
        binding.recyclerView.adapter = adapter
    }

    override fun onResume() {
        super.onResume()
        ContextCompat.registerReceiver(
            this,
            mReceiver,
            IntentFilter(AppConfig.BROADCAST_ACTION_ACTIVITY),
            Utils.receiverFlags()
        )
        reload()
    }

    override fun onPause() {
        super.onPause()
        runCatching { unregisterReceiver(mReceiver) }
    }

    private fun reload() {
        if (!AutoConnectManager.isPanelConfigured()) {
            binding.emptyState.text = getString(R.string.server_list_empty)
            binding.emptyState.isVisible = true
            binding.recyclerView.isVisible = false
            return
        }

        val subId = AutoConnectManager.ensureSubscription()
        val guids = MmkvManager.decodeServerList(subId)
        val selected = MmkvManager.getSelectServer()

        val rows = guids.mapNotNull { guid ->
            val delay = MmkvManager.decodeServerAffiliationInfo(guid)?.testDelayMillis ?: 0L
            val profile = MmkvManager.decodeServerConfig(guid) ?: return@mapNotNull null
            val (flag, name) = CountryUtils.countryFromRemarks(profile.remarks)
            ResultRow(
                guid = guid,
                flag = flag ?: CountryUtils.UNKNOWN_FLAG,
                countryName = name,
                delayMillis = delay,
                isSelected = guid == selected
            )
        }.sortedBy { if (it.delayMillis <= 0L) Long.MAX_VALUE else it.delayMillis }

        adapter.submitList(rows)
        binding.emptyState.isVisible = rows.isEmpty()
        binding.recyclerView.isVisible = rows.isNotEmpty()
    }

    companion object {
        const val EXTRA_SELECTED_GUID = "selectedGuid"
    }
}

private data class ResultRow(
    val guid: String,
    val flag: String,
    val countryName: String?,
    val delayMillis: Long,
    val isSelected: Boolean
)

private class ResultAdapter(private val onClick: (String) -> Unit) : RecyclerView.Adapter<ResultAdapter.ViewHolder>() {

    private val items = mutableListOf<ResultRow>()

    fun submitList(newItems: List<ResultRow>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val itemBinding = ItemServerResultBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(itemBinding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) = holder.bind(items[position], onClick)

    override fun getItemCount() = items.size

    class ViewHolder(private val itemBinding: ItemServerResultBinding) : RecyclerView.ViewHolder(itemBinding.root) {
        fun bind(row: ResultRow, onClick: (String) -> Unit) {
            val context = itemBinding.root.context
            itemBinding.tvFlag.text = row.flag
            itemBinding.tvCountryName.text = row.countryName ?: context.getString(R.string.home_unknown_location)
            itemBinding.tvPing.text = if (row.delayMillis > 0L) context.getString(R.string.home_ping_ms, row.delayMillis) else "---"
            itemBinding.tvPing.setTextColor(
                ContextCompat.getColor(
                    context,
                    if (row.delayMillis in 1 until 300) R.color.colorPing else R.color.home_warning
                )
            )
            itemBinding.ivSelected.isVisible = row.isSelected
            itemBinding.root.setOnClickListener { onClick(row.guid) }
        }
    }
}
