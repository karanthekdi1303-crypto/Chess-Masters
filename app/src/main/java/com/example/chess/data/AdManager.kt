package com.example.chess.data

import android.app.Activity
import android.content.Context
import android.util.Log
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback

/**
 * AdManager handles the initialization, caching, and display of Google Mobile Ads (AdMob).
 *
 * IMPORTANT ADMOB SAFETY RULES & DEPLOYMENT INSTRUCTIONS:
 * 1. For development and testing, ALWAYS use the official Google Test Ads IDs configured below.
 * 2. NEVER use real production AdMob IDs while testing, as this violates AdMob policies
 *    and can lead to account suspension/termination.
 * 3. Before publishing the app to the Google Play Store, replace these test IDs with your
 *    actual AdMob App ID (in AndroidManifest.xml) and Ad Unit IDs (below).
 */
object AdManager {
    private const val TAG = "AdManager"

    // ==========================================
    // ADMOB AD UNIT CONFIGURATION (Update before App Release)
    // ==========================================

    /**
     * AdMob Test App ID (For reference, configured in AndroidManifest.xml):
     * ca-app-pub-3940256099942544~3347511713
     */

    /**
     * AdMob Test Banner Ad Unit ID.
     * Replace with real Banner Ad Unit ID upon publication.
     */
    const val BANNER_AD_UNIT_ID = "ca-app-pub-3940256099942544/9214589741"

    /**
     * AdMob Test Interstitial Ad Unit ID.
     * Replace with real Interstitial Ad Unit ID upon publication.
     */
    const val INTERSTITIAL_AD_UNIT_ID = "ca-app-pub-3940256099942544/1033173712"

    private var mInterstitialAd: InterstitialAd? = null
    private var isInitializing = false
    private var isInitialized = false

    /**
     * Initializes the Mobile Ads SDK. Needs to be called on App Startup.
     */
    fun initialize(context: Context) {
        if (isInitialized || isInitializing) return
        isInitializing = true
        Log.d(TAG, "Initializing Google Mobile Ads SDK...")
        MobileAds.initialize(context) { status ->
            isInitialized = true
            isInitializing = false
            Log.d(TAG, "Google Mobile Ads SDK Initialized: $status")
            // Preload the first interstitial ad on initialization success
            loadInterstitial(context)
        }
    }

    /**
     * Preloads an Interstitial Ad in the background.
     */
    fun loadInterstitial(context: Context) {
        val adRequest = AdRequest.Builder().build()
        Log.d(TAG, "Loading Interstitial Ad with ID: $INTERSTITIAL_AD_UNIT_ID")
        InterstitialAd.load(
            context,
            INTERSTITIAL_AD_UNIT_ID,
            adRequest,
            object : InterstitialAdLoadCallback() {
                override fun onAdFailedToLoad(adError: LoadAdError) {
                    Log.e(TAG, "Interstitial Ad failed to load: ${adError.message}")
                    mInterstitialAd = null
                }

                override fun onAdLoaded(interstitialAd: InterstitialAd) {
                    Log.d(TAG, "Interstitial Ad successfully loaded.")
                    mInterstitialAd = interstitialAd
                }
            }
        )
    }

    /**
     * Shows the preloaded Interstitial Ad and executes onAdDismissed when closed or failed.
     * Automatically triggers a pre-fetch for the next interstitial ad.
     */
    fun showInterstitial(activity: Activity, onAdDismissed: () -> Unit) {
        val interstitial = mInterstitialAd
        if (interstitial != null) {
            interstitial.fullScreenContentCallback = object : FullScreenContentCallback() {
                override fun onAdClicked() {
                    Log.d(TAG, "Interstitial Ad clicked.")
                }

                override fun onAdDismissedFullScreenContent() {
                    Log.d(TAG, "Interstitial Ad dismissed.")
                    mInterstitialAd = null
                    // Preload the next ad instantly for smooth consecutive play sessions
                    loadInterstitial(activity)
                    onAdDismissed()
                }

                override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                    Log.e(TAG, "Interstitial Ad failed to show: ${adError.message}")
                    mInterstitialAd = null
                    onAdDismissed()
                }

                override fun onAdImpression() {
                    Log.d(TAG, "Interstitial Ad recorded an impression.")
                }

                override fun onAdShowedFullScreenContent() {
                    Log.d(TAG, "Interstitial Ad presented.")
                }
            }
            interstitial.show(activity)
        } else {
            Log.d(TAG, "Interstitial ad was not loaded yet. Executing direct path.")
            // Call callback instantly so transition is fully snappy and zero-delay!
            onAdDismissed()
            // Reload on separate main looper message to avoid blocking the current click handler
            android.os.Handler(android.os.Looper.getMainLooper()).post {
                try {
                    loadInterstitial(activity)
                } catch (e: Exception) {
                    Log.e(TAG, "Failed asynchronously triggering load: ${e.message}")
                }
            }
        }
    }
}
