package com.hamedvpn.vpngit.ui

import android.os.Bundle
import com.hamedvpn.vpngit.AppConfig
import com.hamedvpn.vpngit.BuildConfig
import com.hamedvpn.vpngit.R
import com.hamedvpn.vpngit.databinding.ActivityAboutBinding
import com.hamedvpn.vpngit.util.Utils

class AboutActivity : BaseActivity() {
    private val binding by lazy { ActivityAboutBinding.inflate(layoutInflater) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentViewWithToolbar(binding.root, showHomeAsUp = true, title = getString(R.string.title_about))



        binding.tvVersion.text = getString(R.string.about_version_format, BuildConfig.VERSION_NAME)
    }
}
