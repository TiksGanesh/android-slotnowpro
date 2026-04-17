package app.slotnow.slotnowpro.data.remote.interceptor

import android.Manifest
import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import androidx.annotation.RequiresPermission
import app.slotnow.slotnowpro.util.AppLogger
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import okhttp3.Interceptor
import okhttp3.Response

/**
 * Network interceptor that validates internet connectivity before allowing network requests.
 *
 * This interceptor checks whether the device has an active internet connection with validated
 * capabilities. If no connection is available, it throws [NoInternetException] to prevent
 * attempting network operations while offline.
 *
 * The connectivity check requires [android.Manifest.permission.ACCESS_NETWORK_STATE] permission.
 * System-level issues (unavailable services, missing network state) are logged for debugging.
 *
 * @throws NoInternetException if the device has no internet connection
 */
class ConnectivityInterceptor @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val appLogger: AppLogger
) : Interceptor {

    private companion object {
        private const val LOG_TAG = "ConnectivityInterceptor"
    }

    @RequiresPermission(Manifest.permission.ACCESS_NETWORK_STATE)
    override fun intercept(chain: Interceptor.Chain): Response {
        if (!hasInternetConnection()) {
            throw NoInternetException()
        }
        return chain.proceed(chain.request())
    }

    @RequiresPermission(Manifest.permission.ACCESS_NETWORK_STATE)
    private fun hasInternetConnection(): Boolean {
        val connectivityManager =
            context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
        if (connectivityManager == null) {
            appLogger.logErrorDebug(
                LOG_TAG,
                "ConnectivityManager unavailable - system-level issue detected"
            )
            return false
        }

        val activeNetwork = connectivityManager.activeNetwork
        if (activeNetwork == null) {
            appLogger.logInfoDebug(LOG_TAG, "No active network available")
            return false
        }

        val capabilities = connectivityManager.getNetworkCapabilities(activeNetwork)
        if (capabilities == null) {
            appLogger.logErrorDebug(
                LOG_TAG,
                "Network capabilities unavailable for active network"
            )
            return false
        }

        val hasInternet = capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
        val isValidated = capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)

        if (!hasInternet || !isValidated) {
            appLogger.logInfoDebug(
                LOG_TAG,
                "Network state: internet=$hasInternet, validated=$isValidated"
            )
            return false
        }

        return true
    }
}
