package org.wikipedia.util

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.withContext
import org.json.JSONException
import org.wikipedia.R
import org.wikipedia.WikipediaApp
import org.wikipedia.createaccount.CreateAccountException
import org.wikipedia.dataclient.ServiceFactory
import org.wikipedia.dataclient.WikiSite
import org.wikipedia.dataclient.mwapi.MwException
import org.wikipedia.dataclient.mwapi.MwServiceError
import org.wikipedia.dataclient.okhttp.HttpStatusException
import org.wikipedia.login.LoginFailedException
import java.net.SocketException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.util.concurrent.TimeoutException
import javax.net.ssl.SSLException

@Suppress("SameParameterValue")
object ThrowableUtil {
    // TODO: replace with Apache Commons Lang ExceptionUtils.
    fun getInnermostThrowable(e: Throwable): Throwable {
        var t = e
        while (t.cause != null) {
            t = t.cause!!
        }
        return t
    }

    // TODO: replace with Apache Commons Lang ExceptionUtils.
    private fun throwableContainsException(e: Throwable, exClass: Class<*>): Boolean {
        var t: Throwable? = e
        while (t != null) {
            if (exClass.isInstance(t)) {
                return true
            }
            t = t.cause
        }
        return false
    }

    fun getAppError(context: Context, e: Throwable): AppError {
        val inner = getInnermostThrowable(e)
        // look at what kind of exception it is...
        return if (isNetworkError(e)) {
            AppError(context.getString(R.string.error_network_error),
                    context.getString(R.string.format_error_server_message,
                            inner.localizedMessage))
        } else if (e is HttpStatusException) {
            AppError(e.message!!, e.code.toString())
        } else if (inner is LoginFailedException || inner is CreateAccountException ||
                inner is MwException) {
            AppError(inner.localizedMessage!!, "")
        } else if (throwableContainsException(e, JSONException::class.java)) {
            AppError(context.getString(R.string.error_response_malformed),
                    inner.localizedMessage)
        } else {
            // everything else has fallen through, so just treat it as an "unknown" error
            AppError(context.getString(R.string.error_unknown),
                    inner.localizedMessage)
        }
    }

    fun isOffline(caught: Throwable?): Boolean {
        return caught is UnknownHostException || caught is SocketException
    }

    fun isTimeout(caught: Throwable?): Boolean {
        return caught is SocketTimeoutException
    }

    fun is404(caught: Throwable): Boolean {
        return caught is HttpStatusException && caught.code == 404
    }

    fun isEmptyException(caught: Throwable): Boolean {
        return caught is EmptyException
    }

    fun isNetworkError(e: Throwable): Boolean {
        return throwableContainsException(e, UnknownHostException::class.java) ||
                throwableContainsException(e, TimeoutException::class.java) ||
                throwableContainsException(e, SSLException::class.java)
    }

    fun isNotLoggedIn(t: Throwable?): Boolean {
        return t is MwException && t.error.code?.contains("notloggedin") == true
    }

    suspend fun getBlockMessageHtml(blockInfo: MwServiceError.BlockInfo, wikiSite: WikiSite = WikipediaApp.instance.wikiSite): String {
        var html = ""
        withContext(Dispatchers.IO) {
            val userInfoCall = async { ServiceFactory.get(wikiSite).getUserInfo() }
            val blockedPageCall = async { ServiceFactory.get(wikiSite).parsePage("MediaWiki:Blockedtext") }
            val reasonTextCall = async { ServiceFactory.get(wikiSite).parseText(blockInfo.blockReason) }

            html = parseBlockedError(blockedPageCall.await().text, blockInfo,
                reasonTextCall.await().text, userInfoCall.await().query?.userInfo!!.name)
        }
        return html
    }

    private fun parseBlockedError(template: String, info: MwServiceError.BlockInfo, reason: String, userName: String): String {
        return template.replace("$1", "<a href=\"${StringUtil.userPageTitleFromName(info.blockedBy, WikipediaApp.instance.wikiSite).mobileUri}\">${info.blockedBy}</a>")
            .replace("$2", reason)
            .replace("$3", "") // IP address of user (TODO: somehow get from API?)
            .replace("$4", "") // unknown parameter (unused?)
            .replace("$5", info.blockId.toString())
            .replace("$6", parseBlockedDate(info.blockExpiry))
            .replace("$7", "<a href=\"${StringUtil.userPageTitleFromName(userName, WikipediaApp.instance.wikiSite).mobileUri}\">$userName</a>")
            .replace("$8", parseBlockedDate(info.blockTimeStamp))
    }

    private fun parseBlockedDate(dateStr: String): String {
        try {
            return DateUtil.iso8601DateParse(dateStr).toString()
        } catch (_: Exception) {}
        return dateStr
    }

    class EmptyException : Exception()
    class AppError(val error: String, val detail: String?)
}
