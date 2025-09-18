package com.example.gameforpmd

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.WebView
import androidx.fragment.app.Fragment

class RulesFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_rules, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val webView = view.findViewById<WebView>(R.id.webViewRules)

        // Загружаем HTML из res/raw/rules.html
        val inputStream = resources.openRawResource(R.raw.rules)
        val html = inputStream.bufferedReader().use { it.readText() }

        // Загружаем в WebView с корректной кодировкой
        webView.loadDataWithBaseURL(
            null,
            html,
            "text/html; charset=utf-8",
            "utf-8",
            null
        )
    }
}
