package ua.com.radiokot.photoprism.features.envconnection.logic

import io.reactivex.rxjava3.core.Single
import ua.com.radiokot.photoprism.api.config.model.PhotoPrismClientConfig
import ua.com.radiokot.photoprism.api.config.service.PhotoPrismClientConfigService
import ua.com.radiokot.photoprism.base.data.storage.ObjectPersistence
import ua.com.radiokot.photoprism.env.data.model.*
import ua.com.radiokot.photoprism.env.data.storage.EnvSessionHolder
import ua.com.radiokot.photoprism.env.logic.SessionCreator
import ua.com.radiokot.photoprism.extension.kLogger
import ua.com.radiokot.photoprism.extension.toSingle

typealias PhotoPrismConfigServiceFactory =
            (envConnectionParams: EnvConnectionParams, sessionId: String) -> PhotoPrismClientConfigService

/**
 * Creates [EnvSession] for the given [EnvConnectionParams] and [EnvAuth].
 * On success, sets the session to the [envSessionHolder] and [envSessionPersistence]
 * and the [auth] to the [envAuthPersistence], if present.
 *
 * @see InvalidCredentialsException
 * @see EnvIsNotPublicException
 */
class ConnectToEnvUseCase(
    private val connectionParams: EnvConnectionParams,
    private val auth: EnvAuth,
    private val configServiceFactory: PhotoPrismConfigServiceFactory,
    private val sessionCreator: SessionCreator,
    private val envSessionHolder: EnvSessionHolder?,
    private val envSessionPersistence: ObjectPersistence<EnvSession>?,
    private val envAuthPersistence: ObjectPersistence<EnvAuth>?,
) {
    private val log = kLogger("ConnectToEnvUseCase")

    private lateinit var sessionId: String
    private lateinit var config: Config

    fun perform(): Single<EnvSession> {
        return getSessionId()
            .doOnSuccess {
                sessionId = it

                log.debug {
                    "perform(): got_session_id:" +
                            "\nid=$it"
                }
            }
            .flatMap { getConfig() }
            .doOnSuccess {
                config = it

                log.debug {
                    "perform(): got_config:" +
                            "\nconfig=$it"
                }
            }
            .flatMap { checkConfig() }
            .doOnSuccess {
                log.debug { "perform(): config_checked" }
            }
            .map {
                EnvSession(
                    id = sessionId,
                    envConnectionParams = connectionParams,
                    previewToken = config.previewToken,
                    downloadToken = config.downloadToken,
                )
            }
            .doOnSuccess { session ->
                log.debug {
                    "perform(): successfully_created_session:" +
                            "\nsession=$session"
                }

                envSessionHolder?.apply {
                    set(session)

                    log.debug {
                        "perform(): session_holder_set:" +
                                "\nholder=$this"
                    }
                }
                envSessionPersistence?.apply {
                    saveItem(session)

                    log.debug {
                        "perform(): session_saved_to_persistence:" +
                                "\npersistence=$this"
                    }
                }
                envAuthPersistence?.apply {
                    saveItem(auth)

                    log.debug {
                        "perform(): auth_saved_to_persistence:" +
                                "\npersistence=$this"
                    }
                }
            }
    }

    private fun getSessionId(): Single<String> = {
        sessionCreator.createSession(
            auth = auth,
        )
    }
        .toSingle()

    private fun getConfig(): Single<Config> = {
        configServiceFactory(connectionParams, sessionId)
            .getClientConfig()
            .let(::Config)
    }.toSingle()

    private fun checkConfig(): Single<Boolean> =
        if (auth is EnvAuth.Public && !config.isPublic)
            Single.error(EnvIsNotPublicException())
        else
            Single.just(true)

    private data class Config(
        val previewToken: String,
        val downloadToken: String,
        val isPublic: Boolean,
    ) {
        constructor(source: PhotoPrismClientConfig) : this(
            previewToken = source.previewToken,
            downloadToken = source.downloadToken,
            isPublic = source.public
        )
    }
}