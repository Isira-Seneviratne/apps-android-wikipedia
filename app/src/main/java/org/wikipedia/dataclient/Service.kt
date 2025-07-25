package org.wikipedia.dataclient

import org.wikipedia.captcha.Captcha
import org.wikipedia.dataclient.discussiontools.DiscussionToolsEditResponse
import org.wikipedia.dataclient.discussiontools.DiscussionToolsInfoResponse
import org.wikipedia.dataclient.discussiontools.DiscussionToolsSubscribeResponse
import org.wikipedia.dataclient.discussiontools.DiscussionToolsSubscriptionList
import org.wikipedia.dataclient.donate.PaymentResponseContainer
import org.wikipedia.dataclient.mwapi.CreateAccountResponse
import org.wikipedia.dataclient.mwapi.MwParseResponse
import org.wikipedia.dataclient.mwapi.MwPostResponse
import org.wikipedia.dataclient.mwapi.MwQueryResponse
import org.wikipedia.dataclient.mwapi.MwStreamConfigsResponse
import org.wikipedia.dataclient.mwapi.ParamInfoResponse
import org.wikipedia.dataclient.mwapi.ShortenUrlResponse
import org.wikipedia.dataclient.mwapi.SiteMatrix
import org.wikipedia.dataclient.mwapi.TemplateDataResponse
import org.wikipedia.dataclient.okhttp.OfflineCacheInterceptor
import org.wikipedia.dataclient.rollback.RollbackPostResponse
import org.wikipedia.dataclient.watch.WatchPostResponse
import org.wikipedia.dataclient.wikidata.Claims
import org.wikipedia.dataclient.wikidata.Entities
import org.wikipedia.dataclient.wikidata.EntityPostResponse
import org.wikipedia.dataclient.wikidata.Search
import org.wikipedia.edit.Edit
import org.wikipedia.login.LoginResponse
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Headers
import retrofit2.http.POST
import retrofit2.http.Query
import java.time.Instant

/**
 * Retrofit service layer for all API interactions, including regular MediaWiki and RESTBase.
 */
interface Service {

    // ------- Search -------

    @GET(
        MW_API_PREFIX + "action=query&redirects=&converttitles=&prop=description|pageimages|coordinates|info&piprop=thumbnail" +
                "&pilicense=any&generator=prefixsearch&gpsnamespace=0&inprop=varianttitles|displaytitle&pithumbsize=" + PREFERRED_THUMB_SIZE
    )
    suspend fun prefixSearch(@Query("gpssearch") searchTerm: String?,
                             @Query("gpslimit") maxResults: Int,
                             @Query("gpsoffset") gpsOffset: Int?): MwQueryResponse

    @GET(
        MW_API_PREFIX + "action=query&converttitles=" +
                "&prop=description|pageimages|pageprops|coordinates|info&ppprop=mainpage|disambiguation" +
                "&generator=search&gsrnamespace=0&gsrwhat=text" +
                "&inprop=varianttitles|displaytitle" +
                "&gsrinfo=&gsrprop=redirecttitle&piprop=thumbnail&pilicense=any&pithumbsize=" +
                PREFERRED_THUMB_SIZE
    )
    suspend fun fullTextSearch(
        @Query("gsrsearch") searchTerm: String?,
        @Query("gsrlimit") gsrLimit: Int,
        @Query("gsroffset") gsrOffset: Int?
    ): MwQueryResponse

    @GET(MW_API_PREFIX + "action=query&list=allusers&auwitheditsonly=1")
    suspend fun prefixSearchUsers(
            @Query("auprefix") prefix: String,
            @Query("aulimit") maxResults: Int
    ): MwQueryResponse

    @GET(
        MW_API_PREFIX + "action=query&generator=search&prop=imageinfo&iiprop=extmetadata|url" +
                "&gsrnamespace=6&iiurlwidth=" + PREFERRED_THUMB_SIZE
    )
    suspend fun fullTextSearchCommons(
        @Query("gsrsearch") searchTerm: String,
        @Query("gsrlimit") gsrLimit: Int,
        @Query("gsroffset") gsrOffset: Int?,
    ): MwQueryResponse

    @GET(
        MW_API_PREFIX + "action=query&converttitles=" +
                "&prop=description|info" +
                "&generator=search&gsrnamespace=0&gsrwhat=text" +
                "&inprop=varianttitles|displaytitle" +
                "&gsrinfo=&gsrprop=redirecttitle"
    )
    suspend fun fullTextSearchTemplates(
        @Query("gsrsearch") searchTerm: String,
        @Query("gsrlimit") gsrLimit: Int,
        @Query("gsroffset") gsrOffset: Int?,
    ): MwQueryResponse

    @GET(
        MW_API_PREFIX + "action=query&generator=search&gsrnamespace=0&gsrqiprofile=classic_noboostlinks" +
                "&origin=*&piprop=thumbnail&pilicense=any&prop=pageimages|description|info|pageprops" +
                "&inprop=varianttitles&smaxage=86400&maxage=86400&pithumbsize=" + PREFERRED_THUMB_SIZE
    )
    suspend fun searchMoreLike(
        @Query("gsrsearch") searchTerm: String?,
        @Query("gsrlimit") gsrLimit: Int,
        @Query("pilimit") piLimit: Int,
        @Query("gsroffset") gsrOffset: Int? = null,
    ): MwQueryResponse

    // ------- Miscellaneous -------

    @GET(MW_API_PREFIX + "action=fancycaptchareload")
    suspend fun getNewCaptcha(): Captcha

    @GET(MW_API_PREFIX + "action=query&prop=langlinks&lllimit=500&redirects=&converttitles=")
    suspend fun getLangLinks(@Query("titles") title: String): MwQueryResponse

    @GET(MW_API_PREFIX + "action=query&prop=description&redirects=1")
    suspend fun getDescription(@Query("titles") titles: String): MwQueryResponse

    @GET(MW_API_PREFIX + "action=query&prop=info|description|pageimages&pilicense=any&inprop=varianttitles|displaytitle&redirects=1&pithumbsize=" + PREFERRED_THUMB_SIZE)
    suspend fun getInfoByPageIdsOrTitles(@Query("pageids") pageIds: String? = null, @Query("titles") titles: String? = null): MwQueryResponse

    @GET(MW_API_PREFIX + "action=query&meta=siteinfo&siprop=general|autocreatetempuser")
    suspend fun getPageIds(@Query("titles") titles: String): MwQueryResponse

    @GET(MW_API_PREFIX + "action=query&prop=imageinfo&iiprop=timestamp|user|url|mime|metadata|extmetadata&iiurlwidth=" + PREFERRED_THUMB_SIZE)
    suspend fun getImageInfo(
        @Query("titles") titles: String,
        @Query("iiextmetadatalanguage") lang: String
    ): MwQueryResponse

    @GET(MW_API_PREFIX + "action=query&prop=videoinfo&viprop=timestamp|user|url|mime|metadata|extmetadata|derivatives&viurlwidth=" + PREFERRED_THUMB_SIZE)
    suspend fun getVideoInfo(
        @Query("titles") titles: String,
        @Query("viextmetadatalanguage") lang: String
    ): MwQueryResponse

    @GET(MW_API_PREFIX + "action=query&prop=imageinfo|entityterms&iiprop=timestamp|user|url|mime|extmetadata&iiurlwidth=" + PREFERRED_THUMB_SIZE)
    suspend fun getImageInfoWithEntityTerms(
            @Query("titles") titles: String,
            @Query("iiextmetadatalanguage") metadataLang: String,
            @Query("wbetlanguage") entityLang: String
    ): MwQueryResponse

    @GET(MW_API_PREFIX + "action=query&prop=info&inprop=protection")
    suspend fun getProtection(@Query("titles") titles: String): MwQueryResponse

    @GET(MW_API_PREFIX + "action=query&meta=userinfo&prop=info&inprop=protection&uiprop=groups")
    suspend fun getProtectionWithUserInfo(@Query("titles") titles: String): MwQueryResponse

    @GET(MW_API_PREFIX + "action=sitematrix&smtype=language&smlangprop=code|name|localname&maxage=" + SITE_INFO_MAXAGE + "&smaxage=" + SITE_INFO_MAXAGE)
    suspend fun getSiteMatrix(): SiteMatrix

    @GET(MW_API_PREFIX + "action=query&meta=siteinfo&siprop=namespaces")
    suspend fun getPageNamespaceWithSiteInfo(
        @Query("titles") title: String?,
        @Header(OfflineCacheInterceptor.SAVE_HEADER) saveHeader: String? = null,
        @Header(OfflineCacheInterceptor.LANG_HEADER) langHeader: String? = null,
        @Header(OfflineCacheInterceptor.TITLE_HEADER) titleHeader: String? = null
    ): MwQueryResponse

    @GET(MW_API_PREFIX + "action=query&meta=siteinfo&maxage=" + SITE_INFO_MAXAGE + "&smaxage=" + SITE_INFO_MAXAGE)
    suspend fun getSiteInfo(): MwQueryResponse

    @GET(MW_API_PREFIX + "action=query&meta=siteinfo&siprop=general|magicwords")
    suspend fun getSiteInfoWithMagicWords(): MwQueryResponse

    @GET(MW_API_PREFIX + "action=parse&prop=text&mobileformat=1")
    suspend fun parsePage(@Query("page") pageTitle: String): MwParseResponse

    @GET(MW_API_PREFIX + "action=parse&prop=text&mobileformat=1&contentmodel=wikitext&disablelimitreport=1")
    suspend fun parseText(@Query("text") text: String): MwParseResponse

    @GET(MW_API_PREFIX + "action=parse&prop=text&mobileformat=1&mainpage=1")
    suspend fun parseTextForMainPage(@Query("page") mainPageTitle: String): MwParseResponse

    @GET(MW_API_PREFIX + "action=query&prop=info&generator=categories&inprop=varianttitles|displaytitle&gclshow=!hidden&gcllimit=500")
    suspend fun getCategories(@Query("titles") titles: String): MwQueryResponse

    @GET(MW_API_PREFIX + "action=query&prop=categories&clshow=!hidden&cllimit=100")
    suspend fun getCategoriesProps(@Query("titles") titles: String): MwQueryResponse

    @GET(MW_API_PREFIX + "action=query&prop=description|pageimages|info&pilicense=any&generator=categorymembers&inprop=varianttitles|displaytitle&gcmprop=ids|title")
    suspend fun getCategoryMembers(
        @Query("gcmtitle") title: String,
        @Query("gcmtype") type: String,
        @Query("gcmlimit") count: Int,
        @Query("gcmcontinue") continueStr: String?
    ): MwQueryResponse

    @GET(MW_API_PREFIX + "action=query&generator=random&redirects=1&grnnamespace=0&prop=pageprops|description|info&inprop=protection")
    suspend fun getRandomPages(
        @Query("grnlimit") count: Int = 50,
    ): MwQueryResponse

    @GET(MW_API_PREFIX + "action=query&generator=random&redirects=1&grnnamespace=6&prop=info|description|imageinfo|revisions|globalusage&inprop=protection&gunamespace=0&rvprop=ids|timestamp|flags|comment|user|content&rvslots=mediainfo&iiprop=timestamp|user|url|mime|extmetadata&iilocalonly=1&iiurlwidth=" + PREFERRED_THUMB_SIZE)
    suspend fun getRandomImages(
        @Query("grnlimit") count: Int = 10,
    ): MwQueryResponse

    @Headers("Cache-Control: no-cache")
    @GET(MW_API_PREFIX + "action=query&list=recentchanges&rcprop=title|timestamp|ids|oresscores|sizes|tags|user|parsedcomment|comment|flags&rcnamespace=0&rctype=edit|new")
    suspend fun getRecentEdits(
        @Query("rclimit") count: Int,
        @Query("rcstart") startTimeStamp: String,
        @Query("rcdir") direction: String?,
        @Query("rctoponly") latestRevisions: String?,
        @Query("rcshow") filters: String?,
        @Query("rccontinue") continueStr: String?
    ): MwQueryResponse

    @FormUrlEncoded
    @POST(MW_API_PREFIX + "action=options")
    suspend fun postSetOptions(
        @Field("change") change: String,
        @Field("token") token: String
    ): MwPostResponse

    @GET(MW_API_PREFIX + "action=streamconfigs&format=json&constraints=destination_event_service=eventgate-analytics-external")
    suspend fun getStreamConfigs(): MwStreamConfigsResponse

    @GET(MW_API_PREFIX + "action=query&meta=allmessages&amenableparser=1")
    suspend fun getMessages(
            @Query("ammessages") messages: String,
            @Query("amargs") args: String?,
            @Query("amlang") lang: String? = null
    ): MwQueryResponse

    @FormUrlEncoded
    @POST(MW_API_PREFIX + "action=shortenurl")
    suspend fun shortenUrl(
            @Field("url") url: String,
    ): ShortenUrlResponse

    @GET(MW_API_PREFIX + "action=query&generator=geosearch&prop=coordinates|description|pageimages|info&inprop=varianttitles|displaytitle&pilicense=any")
    suspend fun getGeoSearch(
        @Query("ggscoord", encoded = true) coordinates: String,
        @Query("ggsradius") radius: Int,
        @Query("ggslimit") ggsLimit: Int,
        @Query("colimit") coLimit: Int,
    ): MwQueryResponse

    @GET("api.php?format=json&action=getPaymentMethods")
    suspend fun getPaymentMethods(@Query("country") country: String): PaymentResponseContainer

    @FormUrlEncoded
    @POST("api.php?format=json&action=submitPayment")
    suspend fun submitPayment(
        @Field("amount") amount: String,
        @Field("app_version") appVersion: String,
        @Field("banner") banner: String,
        @Field("city") city: String,
        @Field("country") country: String,
        @Field("currency") currency: String,
        @Field("donor_country") donorCountry: String,
        @Field("email") email: String,
        @Field("full_name") fullName: String,
        @Field("language") language: String,
        @Field("recurring") recurring: String,
        @Field("payment_token") paymentToken: String,
        @Field("opt_in") optIn: String,
        @Field("pay_the_fee") payTheFee: String,
        @Field("payment_method") paymentMethod: String,
        @Field("payment_network") paymentNetwork: String,
        @Field("postal_code") postalCode: String,
        @Field("state_province") stateProvince: String,
        @Field("street_address") streetAddress: String
    ): PaymentResponseContainer

    // ------- CSRF, Login, and Create Account -------

    @GET(MW_API_PREFIX + "action=query&meta=tokens")
    @Headers("Cache-Control: no-cache")
    suspend fun getToken(@Query("type") type: String = "csrf"): MwQueryResponse

    @FormUrlEncoded
    @POST(MW_API_PREFIX + "action=createaccount&createmessageformat=html")
    suspend fun postCreateAccount(
        @Field("username") user: String,
        @Field("password") pass: String,
        @Field("retype") retype: String,
        @Field("createtoken") token: String,
        @Field("createreturnurl") returnurl: String,
        @Field("email") email: String?,
        @Field("captchaId") captchaId: String?,
        @Field("captchaWord") captchaWord: String?
    ): CreateAccountResponse

    @GET(MW_API_PREFIX + "action=query&meta=tokens&type=login")
    @Headers("Cache-Control: no-cache")
    suspend fun getLoginToken(): MwQueryResponse

    @FormUrlEncoded
    @POST(MW_API_PREFIX + "action=clientlogin&rememberMe=")
    suspend fun postLogIn(
        @Field("username") user: String? = null,
        @Field("password") pass: String? = null,
        @Field("retype") retype: String? = null,
        @Field("OATHToken") twoFactorCode: String? = null,
        @Field("token") emailAuthToken: String? = null,
        @Field("captchaId") captchaId: String? = null,
        @Field("captchaWord") captchaWord: String? = null,
        @Field("loginreturnurl") returnUrl: String? = null,
        @Field("logintoken") loginToken: String? = null,
        @Field("logincontinue") loginContinue: Boolean? = null
    ): LoginResponse

    @FormUrlEncoded
    @POST(MW_API_PREFIX + "action=logout")
    suspend fun postLogout(@Field("token") token: String): MwPostResponse

    @GET(MW_API_PREFIX + "action=query&meta=authmanagerinfo|tokens&amirequestsfor=create&type=createaccount")
    suspend fun getAuthManagerInfo(): MwQueryResponse

    @GET(MW_API_PREFIX + "action=query&meta=authmanagerinfo&amirequestsfor=login")
    suspend fun getAuthManagerForLogin(): MwQueryResponse

    @GET(MW_API_PREFIX + "action=query&meta=userinfo&uiprop=groups|blockinfo|editcount|latestcontrib|hasmsg|options")
    suspend fun getUserInfo(): MwQueryResponse

    @GET(MW_API_PREFIX + "action=query&list=users&usprop=editcount|groups|registration|rights")
    suspend fun userInfo(@Query("ususers") userName: String): MwQueryResponse

    @GET(MW_API_PREFIX + "action=query&list=users&usprop=editcount|groups|registration|rights&meta=allmessages")
    suspend fun userInfoWithMessages(@Query("ususers") userName: String, @Query("ammessages") messages: String): MwQueryResponse

    @GET(MW_API_PREFIX + "action=query&meta=globaluserinfo&guiprop=editcount|groups|rights")
    suspend fun globalUserInfo(@Query("guiuser") userName: String): MwQueryResponse

    @GET(MW_API_PREFIX + "action=query&list=users&usprop=groups|cancreate")
    suspend fun getUserList(@Query("ususers") userNames: String): MwQueryResponse

    // ------- Notifications -------

    @Headers("Cache-Control: no-cache")
    @GET(MW_API_PREFIX + "action=query&meta=notifications&notformat=model&notlimit=max")
    suspend fun getAllNotifications(
        @Query("notwikis") wikiList: String?,
        @Query("notfilter") filter: String?,
        @Query("notcontinue") continueStr: String?
    ): MwQueryResponse

    @FormUrlEncoded
    @POST(MW_API_PREFIX + "action=echomarkread")
    suspend fun markRead(
        @Field("token") token: String,
        @Field("list") readList: String?,
        @Field("unreadlist") unreadList: String?
    ): MwQueryResponse

    @Headers("Cache-Control: no-cache")
    @GET(MW_API_PREFIX + "action=query&meta=notifications&notwikis=*&notprop=list&notfilter=!read&notlimit=1")
    suspend fun lastUnreadNotification(): MwQueryResponse

    @Headers("Cache-Control: no-cache")
    @GET(MW_API_PREFIX + "action=query&meta=unreadnotificationpages&unplimit=max&unpwikis=*")
    suspend fun unreadNotificationWikis(): MwQueryResponse

    @FormUrlEncoded
    @POST(MW_API_PREFIX + "action=echopushsubscriptions&command=create&provider=fcm")
    suspend fun subscribePush(
        @Field("token") csrfToken: String,
        @Field("providertoken") providerToken: String
    ): MwQueryResponse

    @FormUrlEncoded
    @POST(MW_API_PREFIX + "action=echopushsubscriptions&command=delete&provider=fcm")
    suspend fun unsubscribePush(
        @Field("token") csrfToken: String,
        @Field("providertoken") providerToken: String
    ): MwQueryResponse

    // ------- Editing -------

    @GET(MW_API_PREFIX + "action=query&prop=revisions|info&meta=siteinfo&siprop=general|autocreatetempuser&rvslots=main&rvprop=content|timestamp|ids&rvlimit=1&converttitles=&intestactions=edit&intestactionsdetail=full&inprop=editintro")
    suspend fun getWikiTextForSectionWithInfo(
        @Query("titles") title: String,
        @Query("rvsection") section: Int?
    ): MwQueryResponse

    @FormUrlEncoded
    @POST(MW_API_PREFIX + "action=edit")
    suspend fun postUndoEdit(
            @Field("title") title: String,
            @Field("summary") summary: String? = null,
            @Field("assert") user: String? = null,
            @Field("token") token: String,
            @Field("undo") undoRevId: Long,
            @Field("undoafter") undoRevAfter: Long? = null,
            @Field("matags") tags: String? = null
    ): Edit

    @FormUrlEncoded
    @POST(MW_API_PREFIX + "action=edit")
    suspend fun postEditSubmit(
        @Field("title") title: String,
        @Field("section") section: String?,
        @Field("sectiontitle") newSectionTitle: String?,
        @Field("summary") summary: String,
        @Field("assert") user: String?,
        @Field("text") text: String?,
        @Field("appendtext") appendText: String?,
        @Field("baserevid") baseRevId: Long,
        @Field("token") token: String,
        @Field("captchaid") captchaId: String?,
        @Field("captchaword") captchaWord: String?,
        @Field("minor") minor: Boolean? = null,
        @Field("watchlist") watchlist: String? = null,
        @Field("matags") tags: String? = null
    ): Edit

    @FormUrlEncoded
    @POST(MW_API_PREFIX + "action=visualeditoredit")
    suspend fun postVisualEditorEdit(
        @Field("paction") action: String,
        @Field("page") title: String,
        @Field("token") token: String,
        @Field("section") section: Int,
        @Field("sectiontitle") newSectionTitle: String?,
        @Field("summary") summary: String,
        @Field("assert") user: String?,
        @Field("captchaid") captchaId: String?,
        @Field("captchaword") captchaWord: String?,
        @Field("minor") minor: Boolean? = null,
        @Field("watchlist") watchlist: String? = null,
        @Field("plugins") plugins: String? = null,
        @Field("data-ge-task-image-recommendation") imageRecommendationJson: String? = null,
    ): Edit

    @GET(MW_API_PREFIX + "action=query&list=usercontribs&ucprop=ids|title|timestamp|comment|size|flags|sizediff|tags&meta=userinfo&uiprop=groups|blockinfo|editcount|latestcontrib|rights|registrationdate")
    suspend fun getUserContributions(
        @Query("ucuser") username: String,
        @Query("uclimit") maxCount: Int,
        @Query("ucnamespace") ns: Int?,
        @Query("uccontinue") uccontinue: String?
    ): MwQueryResponse

    @GET(MW_API_PREFIX + "action=query&list=usercontribs&ucprop=ids|title|timestamp|comment|size|flags|sizediff|tags")
    suspend fun getUserContrib(
            @Query("ucuser") username: String,
            @Query("uclimit") maxCount: Int,
            @Query("ucnamespace") ns: String?,
            @Query("ucshow") filter: String?,
            @Query("uccontinue") uccontinue: String?
    ): MwQueryResponse

    @GET(MW_API_PREFIX + "action=query&list=usercontribs&meta=userinfo&uiprop=editcount")
    suspend fun getUserContribsByTimeFrame(
        @Query("ucuser") username: String,
        @Query("uclimit") maxCount: Int,
        @Query("ucstart") startDate: Instant,
        @Query("ucend") endDate: Instant,
        @Query("ucnamespace") ns: Int? = null,
        @Query("uccontinue") uccontinue: String? = null
    ): MwQueryResponse

    @GET(MW_API_PREFIX + "action=query&prop=pageviews")
    suspend fun getPageViewsForTitles(@Query("titles") titles: String): MwQueryResponse

    @FormUrlEncoded
    @POST(MW_API_PREFIX + "action=rollback")
    suspend fun postRollback(
        @Field("title") title: String,
        @Field("summary") summary: String?,
        @Field("user") user: String,
        @Field("token") token: String,
        @Field("matags") tags: String? = null
    ): RollbackPostResponse

    // ------- Wikidata -------

    @GET(MW_API_PREFIX + "action=wbgetentities")
    suspend fun getEntitiesByTitleSuspend(
        @Query("titles") titles: String,
        @Query("sites") sites: String
    ): Entities

    @GET(MW_API_PREFIX + "action=wbsearchentities&type=item&limit=20")
    suspend fun searchEntities(
        @Query("search") searchTerm: String,
        @Query("language") searchLang: String,
        @Query("uselang") resultLang: String
    ): Search

    @GET(MW_API_PREFIX + "action=query&prop=entityterms")
    suspend fun getWikidataEntityTerms(
        @Query("titles") titles: String,
        @Query("wbetlanguage") lang: String
    ): MwQueryResponse

    @GET(MW_API_PREFIX + "action=wbgetclaims")
    suspend fun getClaims(
        @Query("entity") entity: String,
        @Query("property") property: String?
    ): Claims

    @GET(MW_API_PREFIX + "action=wbgetentities&props=descriptions|labels|sitelinks")
    suspend fun getWikidataLabelsAndDescriptions(
        @Query("ids") idList: String,
        @Query("languages") languages: String? = null,
        @Query("sitefilter") siteFilter: String? = null
    ): Entities

    @GET(MW_API_PREFIX + "action=wbgetentities&props=descriptions|labels")
    suspend fun getWikidataDescription(@Query("titles") titles: String,
                                       @Query("sites") sites: String,
                                       @Query("languages") langCode: String): Entities

    @POST(MW_API_PREFIX + "action=wbsetdescription&errorlang=uselang")
    @FormUrlEncoded
    suspend fun postDescriptionEdit(
        @Field("language") language: String,
        @Field("uselang") useLang: String,
        @Field("site") site: String,
        @Field("title") title: String,
        @Field("value") newDescription: String,
        @Field("summary") summary: String?,
        @Field("token") token: String,
        @Field("assert") user: String?,
        @Field("matags") tags: String? = null
    ): EntityPostResponse

    @POST(MW_API_PREFIX + "action=wbsetlabel&errorlang=uselang")
    @FormUrlEncoded
    suspend fun postLabelEdit(
        @Field("language") language: String,
        @Field("uselang") useLang: String,
        @Field("site") site: String,
        @Field("title") title: String,
        @Field("value") newDescription: String,
        @Field("summary") summary: String?,
        @Field("token") token: String,
        @Field("assert") user: String?,
        @Field("matags") tags: String? = null
    ): EntityPostResponse

    @POST(MW_API_PREFIX + "action=wbeditentity&errorlang=uselang")
    @FormUrlEncoded
    suspend fun postEditEntity(
        @Field("id") id: String,
        @Field("token") token: String,
        @Field("data") data: String?,
        @Field("summary") summary: String?,
        @Field("matags") tags: String?
    ): EntityPostResponse

    // ------- Watchlist -------

    @Headers("Cache-Control: no-cache")
    @GET(MW_API_PREFIX + "action=query&prop=info&converttitles=&redirects=&inprop=watched")
    suspend fun getWatchedStatus(@Query("titles") titles: String): MwQueryResponse

    @Headers("Cache-Control: no-cache")
    @GET(MW_API_PREFIX + "action=query&prop=info&converttitles=&redirects=&inprop=watched&meta=userinfo&uiprop=options")
    suspend fun getWatchedStatusWithUserOptions(@Query("titles") titles: String): MwQueryResponse

    @Headers("Cache-Control: no-cache")
    @GET(MW_API_PREFIX + "action=query&prop=info&converttitles=&redirects=&inprop=watched&meta=userinfo&uiprop=rights")
    suspend fun getWatchedStatusWithRights(@Query("titles") titles: String): MwQueryResponse

    @Headers("Cache-Control: no-cache")
    @GET(MW_API_PREFIX + "action=query&prop=info|categories&converttitles=&redirects=&inprop=watched&clshow=!hidden&cllimit=100")
    suspend fun getWatchedStatusWithCategories(@Query("titles") titles: String): MwQueryResponse

    @GET(MW_API_PREFIX + "action=query&list=watchlist&wllimit=500&wlprop=ids|title|flags|comment|parsedcomment|timestamp|sizes|user|loginfo")
    @Headers("Cache-Control: no-cache")
    suspend fun getWatchlist(
        @Query("wlallrev") latestRevisions: String?,
        @Query("wlshow") showCriteria: String?,
        @Query("wltype") typeOfChanges: String?
    ): MwQueryResponse

    @GET(MW_API_PREFIX + "action=query&prop=revisions&rvslots=main&rvprop=timestamp|user|ids|comment|tags")
    suspend fun getLastModified(@Query("titles") titles: String): MwQueryResponse

    @GET(MW_API_PREFIX + "action=query&prop=info|revisions&rvslots=main&rvprop=ids|timestamp|size|flags|comment|parsedcomment|user|oresscores&rvdir=newer")
    suspend fun getRevisionDetailsAscending(
        @Query("titles") titles: String?,
        @Query("pageids") pageIds: String?,
        @Query("rvlimit") count: Int,
        @Query("rvstartid") revisionStartId: Long?
    ): MwQueryResponse

    @GET(MW_API_PREFIX + "action=query&prop=info|revisions&rvslots=main&rvprop=ids|timestamp|size|flags|comment|parsedcomment|user|oresscores&rvdir=older")
    suspend fun getRevisionDetailsDescending(
        @Query("titles") titles: String,
        @Query("rvlimit") count: Int,
        @Query("rvstartid") revisionStartId: Long?,
        @Query("rvcontinue") continueStr: String?,
    ): MwQueryResponse

    @GET(MW_API_PREFIX + "action=query&prop=info|revisions&rvslots=main&rvprop=ids|timestamp|size|flags|comment|parsedcomment|user|oresscores&rvdir=older")
    suspend fun getRevisionDetailsWithInfo(
            @Query("pageids") pageIds: String,
            @Query("rvlimit") count: Int,
            @Query("rvstartid") revisionStartId: Long
    ): MwQueryResponse

    @GET(MW_API_PREFIX + "action=query&prop=info|revisions&rvslots=main&rvprop=ids|timestamp|size|flags|comment|parsedcomment|user|oresscores&rvdir=older&inprop=watched&meta=userinfo&uiprop=rights")
    suspend fun getRevisionDetailsWithUserInfo(
        @Query("pageids") pageIds: String,
        @Query("rvlimit") count: Int,
        @Query("rvstartid") revisionStartId: Long
    ): MwQueryResponse

    @POST(MW_API_PREFIX + "action=thank")
    @FormUrlEncoded
    suspend fun postThanksToRevision(
        @Field("rev") revisionId: Long,
        @Field("token") token: String
    ): EntityPostResponse

    @POST(MW_API_PREFIX + "action=watch&converttitles=&redirects=")
    @FormUrlEncoded
    suspend fun watch(
            @Field("unwatch") unwatch: Int?,
            @Field("pageids") pageIds: String?,
            @Field("titles") titles: String?,
            @Field("expiry") expiry: String?,
            @Field("token") token: String
    ): WatchPostResponse

    @GET(MW_API_PREFIX + "action=query&meta=tokens&type=watch")
    @Headers("Cache-Control: no-cache")
    suspend fun getWatchToken(): MwQueryResponse

    // ------- DiscussionTools -------

    @GET(MW_API_PREFIX + "action=discussiontoolspageinfo&prop=threaditemshtml")
    suspend fun getTalkPageTopics(
            @Query("page") page: String,
            @Header(OfflineCacheInterceptor.SAVE_HEADER) saveHeader: String,
            @Header(OfflineCacheInterceptor.LANG_HEADER) langHeader: String,
            @Header(OfflineCacheInterceptor.TITLE_HEADER) titleHeader: String
    ): DiscussionToolsInfoResponse

    @POST(MW_API_PREFIX + "action=discussiontoolssubscribe")
    @FormUrlEncoded
    suspend fun subscribeTalkPageTopic(
            @Field("page") page: String,
            @Field("commentname") topicName: String,
            @Field("token") token: String,
            @Field("subscribe") subscribe: Boolean?,
    ): DiscussionToolsSubscribeResponse

    @GET(MW_API_PREFIX + "action=discussiontoolsgetsubscriptions")
    suspend fun getTalkPageTopicSubscriptions(@Query("commentname") topicNames: String): DiscussionToolsSubscriptionList

    @POST(MW_API_PREFIX + "action=discussiontoolsedit&paction=addtopic")
    @FormUrlEncoded
    suspend fun postTalkPageTopic(
            @Field("page") page: String,
            @Field("sectiontitle") title: String,
            @Field("wikitext") text: String,
            @Field("token") token: String,
            @Field("summary") summary: String? = null,
            @Field("captchaid") captchaId: Long? = null,
            @Field("captchaword") captchaWord: String? = null,
            @Field("matags") tags: String? = null
    ): DiscussionToolsEditResponse

    @POST(MW_API_PREFIX + "action=discussiontoolsedit&paction=addcomment")
    @FormUrlEncoded
    suspend fun postTalkPageTopicReply(
            @Field("page") page: String,
            @Field("commentid") commentId: String,
            @Field("wikitext") text: String,
            @Field("token") token: String,
            @Field("summary") summary: String? = null,
            @Field("captchaid") captchaId: Long? = null,
            @Field("captchaword") captchaWord: String? = null,
            @Field("matags") tags: String? = null
    ): DiscussionToolsEditResponse

    @GET(MW_API_PREFIX + "action=query&generator=growthtasks")
    suspend fun getGrowthTasks(
        @Query("ggttasktypes") taskTypes: String?,
        @Query("ggttopics") topics: String?,
        @Query("ggtlimit") count: Int
    ): MwQueryResponse

    @GET(MW_API_PREFIX + "action=query&generator=search&gsrsearch=hasrecommendation%3Aimage&gsrnamespace=0&gsrsort=random&prop=growthimagesuggestiondata|revisions|pageimages&pilicense=any&rvprop=ids|timestamp|flags|comment|user|content&rvslots=main&rvsection=0")
    suspend fun getPagesWithImageRecommendations(
        @Query("gsrlimit") count: Int
    ): MwQueryResponse

    @POST(MW_API_PREFIX + "action=growthinvalidateimagerecommendation")
    @FormUrlEncoded
    suspend fun invalidateImageRecommendation(
        @Field("tasktype") taskType: String,
        @Field("title") title: String,
        @Field("filename") fileName: String,
        @Field("token") token: String
    ): MwPostResponse

    @GET(MW_API_PREFIX + "action=paraminfo")
    suspend fun getParamInfo(
        @Query("modules") modules: String
    ): ParamInfoResponse

    @GET(MW_API_PREFIX + "action=templatedata&includeMissingTitles=&converttitles=")
    suspend fun getTemplateData(@Query("lang") langCode: String,
                                @Query("titles") titles: String): TemplateDataResponse

    @GET(MW_API_PREFIX + "action=query&prop=info&converttitles=&inprop=varianttitles")
    suspend fun getVariantTitlesByTitles(@Query("titles") titles: String): MwQueryResponse

    companion object {
        const val WIKIPEDIA_URL = "https://wikipedia.org/"
        const val WIKIDATA_URL = "https://www.wikidata.org/"
        const val COMMONS_URL = "https://commons.wikimedia.org/"
        const val URL_FRAGMENT_FROM_COMMONS = "/wikipedia/commons/"
        const val MW_API_PREFIX = "w/api.php?format=json&formatversion=2&errorformat=html&errorsuselocal=1&"
        const val PREFERRED_THUMB_SIZE = 320

        // Maximum cache time for site-specific data, and other things not likely to change very often.
        const val SITE_INFO_MAXAGE = 86400
    }
}
