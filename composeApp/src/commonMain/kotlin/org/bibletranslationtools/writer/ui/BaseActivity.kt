package org.bibletranslationtools.writer.ui

/**
 * This should be extended by all activities in the app so that we can perform verification on
 * activities such as recovery from crashes.
 *
 */
//abstract class BaseActivity : ComponentActivity(), Foreground.Listener {
//
//    private val catalogClient: ResourceCatalogClient by inject()
//    private val preRepository: IPreferenceRepository by inject()
//
//    protected open val isBootActivity: Boolean = false
//
//    private var foreground: Foreground? = null
//    var isDarkTheme by mutableStateOf<Boolean?>(null)
//
//    override fun onCreate(savedInstanceState: Bundle?) {
//        super.onCreate(savedInstanceState)
//
//        updateIsDarkTheme(
//            preRepository.getDefaultPref(
//                IPreferenceRepository.KEY_PREF_COLOR_THEME,
//                getString(Res.string.pref_default_color_theme)
//            )
//        )
//
//        try {
//            foreground = Foreground.get()
//            foreground?.addListener(this)
//        } catch (e: IllegalStateException) {
//            Logger.i(this.javaClass.name, "Foreground was not initialized")
//        }
//    }
//
//    override fun onResume() {
//        super.onResume()
//
//        updateIsDarkTheme(
//            preRepository.getDefaultPref(
//                IPreferenceRepository.KEY_PREF_COLOR_THEME,
//                getString(Res.string.pref_default_color_theme)
//            )
//        )
//
//        if (!isBootActivity) {
//            val crashFiles = Logger.listStacktraces()
//
//            if (crashFiles.isNotEmpty()) {
//                val intent = Intent(this, MainActivity::class.java).apply {
//                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
//                }
//                startActivity(intent)
//                finish()
//            }
//        }
//    }
//
//    override fun onBecameForeground() {
//        if (!isBootActivity && !catalogClient.isLibraryDeployed) {
//            Logger.w(this.javaClass.name, "The library was not deployed.")
//
//            val intent = Intent(this, MainActivity::class.java).apply {
//                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
//            }
//            startActivity(intent)
//            finish()
//        }
//    }
//
//    override fun onBecameBackground() {
//    }
//
//    override fun onDestroy() {
//        foreground?.removeListener(this)
//        super.onDestroy()
//    }
//
//    private fun updateIsDarkTheme(theme: String) {
//        val lightValue = resources.getString(Res.string.theme_value_light)
//        val darkValue = resources.getString(Res.string.theme_value_dark)
//        val systemValue = resources.getString(Res.string.theme_value_system)
//
//        val isDark = when (theme) {
//            lightValue -> false
//            darkValue -> true
//            systemValue -> null
//            else -> null
//        }
//
//        isDarkTheme = isDark
//    }
//}
