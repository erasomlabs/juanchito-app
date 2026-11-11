package erasom.juanchito.app

import android.app.Activity
import android.content.Context
import android.util.Log
import android.view.ViewGroup
import android.widget.FrameLayout
import com.google.android.gms.ads.*
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback
import com.google.android.gms.ads.rewardedinterstitial.RewardedInterstitialAd
import com.google.android.gms.ads.rewardedinterstitial.RewardedInterstitialAdLoadCallback
import com.google.android.ump.ConsentInformation
import com.google.android.ump.ConsentRequestParameters
import com.google.android.ump.UserMessagingPlatform

class MonetizationManager(private val activity: Activity) {

    companion object {
        private const val TAG = "MonetizationManager"
        const val BANNER_ID = "ca-app-pub-2946370347896607/6762788580"
        const val REWARDED_ID = "ca-app-pub-2946370347896607/6302922880"
        const val REWARDED_INTERSTITIAL_ID = "ca-app-pub-2946370347896607/2565436973"
        const val INTERSTITIAL_ID = "ca-app-pub-2946370347896607/6962184854"
        const val NATIVE_ID = "ca-app-pub-2946370347896607/1647607984"
    }

    private var interstitialAd: InterstitialAd? = null
    private var rewardedAd: RewardedAd? = null
    private var rewardedInterstitialAd: RewardedInterstitialAd? = null
    private var adView: AdView? = null
    private var consentInformation: ConsentInformation? = null

    private var gamesPlayedCount = 0

    fun initPrivacyAndAds(onReady: () -> Unit) {
        val params = ConsentRequestParameters.Builder().build()
        consentInformation = UserMessagingPlatform.getConsentInformation(activity)

        consentInformation?.requestConsentInfoUpdate(
            activity,
            params,
            {
                UserMessagingPlatform.loadAndShowConsentFormIfRequired(activity) { formError ->
                    if (formError != null) {
                        Log.w(TAG, "Consent error: ${formError.message}")
                    }
                    if (consentInformation?.canRequestAds() == true) {
                        initializeMobileAds(onReady)
                    } else {
                        onReady()
                    }
                }
            },
            { requestConsentError ->
                Log.w(TAG, "Consent request error: ${requestConsentError.message}")
                initializeMobileAds(onReady)
            }
        )
    }

    private fun initializeMobileAds(onReady: () -> Unit) {
        MobileAds.initialize(activity) {
            loadInterstitial()
            loadRewarded()
            loadRewardedInterstitial()
            onReady()
        }
    }

    fun attachBanner(container: ViewGroup) {
        if (BillingManager.getInstance(activity).isAdsRemoved()) return
        if (adView != null) return

        val ad = AdView(activity)
        ad.setAdSize(AdSize.BANNER)
        ad.adUnitId = BANNER_ID
        adView = ad
        container.removeAllViews()
        container.addView(ad)
        ad.loadAd(AdRequest.Builder().build())
    }

    fun hideBanner() {
        adView?.visibility = android.view.View.GONE
    }

    fun showBanner() {
        if (!BillingManager.getInstance(activity).isAdsRemoved()) {
            adView?.visibility = android.view.View.VISIBLE
        }
    }

    fun loadInterstitial() {
        if (BillingManager.getInstance(activity).isAdsRemoved()) return
        InterstitialAd.load(
            activity,
            INTERSTITIAL_ID,
            AdRequest.Builder().build(),
            object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(ad: InterstitialAd) {
                    interstitialAd = ad
                }
                override fun onAdFailedToLoad(error: LoadAdError) {
                    interstitialAd = null
                }
            }
        )
    }

    fun onGameFinished(onComplete: () -> Unit) {
        if (BillingManager.getInstance(activity).isAdsRemoved()) {
            onComplete()
            return
        }
        gamesPlayedCount++
        if (gamesPlayedCount % 3 == 0 && interstitialAd != null) {
            interstitialAd?.fullScreenContentCallback = object : FullScreenContentCallback() {
                override fun onAdDismissedFullScreenContent() {
                    loadInterstitial()
                    onComplete()
                }
                override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                    loadInterstitial()
                    onComplete()
                }
            }
            interstitialAd?.show(activity)
            interstitialAd = null
        } else {
            onComplete()
        }
    }

    fun loadRewarded() {
        RewardedAd.load(
            activity,
            REWARDED_ID,
            AdRequest.Builder().build(),
            object : RewardedAdLoadCallback() {
                override fun onAdLoaded(ad: RewardedAd) {
                    rewardedAd = ad
                }
                override fun onAdFailedToLoad(error: LoadAdError) {
                    rewardedAd = null
                }
            }
        )
    }

    fun showRewardedRevive(onRewarded: () -> Unit, onFailedOrDismissed: () -> Unit) {
        val currentAd = rewardedAd
        if (currentAd != null) {
            var rewardEarned = false
            currentAd.fullScreenContentCallback = object : FullScreenContentCallback() {
                override fun onAdDismissedFullScreenContent() {
                    loadRewarded()
                    if (rewardEarned) onRewarded() else onFailedOrDismissed()
                }
                override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                    loadRewarded()
                    onFailedOrDismissed()
                }
            }
            currentAd.show(activity) {
                rewardEarned = true
            }
            rewardedAd = null
        } else {
            loadRewarded()
            onFailedOrDismissed()
        }
    }

    fun loadRewardedInterstitial() {
        RewardedInterstitialAd.load(
            activity,
            REWARDED_INTERSTITIAL_ID,
            AdRequest.Builder().build(),
            object : RewardedInterstitialAdLoadCallback() {
                override fun onAdLoaded(ad: RewardedInterstitialAd) {
                    rewardedInterstitialAd = ad
                }
                override fun onAdFailedToLoad(error: LoadAdError) {
                    rewardedInterstitialAd = null
                }
            }
        )
    }

    fun showRewardedInterstitial(onReward: () -> Unit) {
        val currentAd = rewardedInterstitialAd
        if (currentAd != null) {
            currentAd.show(activity) {
                onReward()
            }
            rewardedInterstitialAd = null
            loadRewardedInterstitial()
        } else {
            loadRewardedInterstitial()
        }
    }
}
