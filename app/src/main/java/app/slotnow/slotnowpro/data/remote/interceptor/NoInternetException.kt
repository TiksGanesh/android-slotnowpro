package app.slotnow.slotnowpro.data.remote.interceptor

import java.io.IOException

class NoInternetException(
    message: String = "No active internet connection"
) : IOException(message)

