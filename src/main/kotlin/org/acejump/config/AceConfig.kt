package org.acejump.config

import com.intellij.openapi.components.*
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.options.Configurable
import org.acejump.label.Pattern
import org.acejump.label.Pattern.Companion.KeyLayout
import org.acejump.label.mapIndices
import java.awt.Color

/**
 * Ensures consistiency between [AceSettings] and [AceSettingsPanel].
 * Persists the state of the AceJump IDE settings across IDE restarts.
 * https://www.jetbrains.org/intellij/sdk/docs/basics/persisting_state_of_components.html
 */

@State(name = "AceConfig", storages = [(Storage("\$APP_CONFIG\$/AceJump.xml"))])
object AceConfig: Configurable, PersistentStateComponent<AceSettings> {
  private val logger = Logger.getInstance(AceConfig::class.java)
  private var settings = AceSettings()
    set(value) {
      allPossibleTags = value.allowedChars.bigrams()
      field = value
    }

  val allowedChars: String get() = settings.allowedChars
  val layout: KeyLayout get() = settings.layout
  val jumpModeColor: Color get() = settings.jumpModeColor
  val textHighlightColor: Color get() = settings.textHighlightColor
  val targetModeColor: Color get() = settings.targetModeColor
  val definitionModeColor: Color get() = settings.definitionModeColor
  val tagForegroundColor: Color get() = settings.tagForegroundColor
  val tagBackgroundColor: Color get() = settings.tagBackgroundColor
  val supportPinyin: Boolean get() = settings.supportPinyin

  private val nearby: Map<Char, Map<Char, Int>> = mapOf(
    // Values are QWERTY keys sorted by physical proximity to the map key
    'j' to "jikmnhuolbgypvftcdrxsezawq8796054321",
    'f' to "ftgvcdryhbxseujnzawqikmolp5463728190",
    'k' to "kolmjipnhubgyvftcdrxsezawq9807654321",
    'd' to "drfcxsetgvzawyhbqujnikmolp4352617890",
    'l' to "lkopmjinhubgyvftcdrxsezawq0987654321",
    's' to "sedxzawrfcqtgvyhbujnikmolp3241567890",
    'a' to "aqwszedxrfctgvyhbujnikmolp1234567890",
    'h' to "hujnbgyikmvftolcdrpxsezawq6758493021",
    'g' to "gyhbvftujncdrikmxseolzawpq5647382910",
    'y' to "yuhgtijnbvfrokmcdeplxswzaq6758493021",
    't' to "tygfruhbvcdeijnxswokmzaqpl5647382910",
    'u' to "uijhyokmnbgtplvfrcdexswzaq7869504321",
    'r' to "rtfdeygvcxswuhbzaqijnokmpl4536271890",
    'n' to "nbhjmvgyuiklocftpxdrzseawq7685940321",
    'v' to "vcfgbxdrtyhnzseujmawikqolp5463728190",
    'm' to "mnjkbhuilvgyopcftxdrzseawq8970654321",
    'c' to "cxdfvzsertgbawyhnqujmikolp4352617890",
    'b' to "bvghncftyujmxdrikzseolawqp6574839201",
    'i' to "iokjuplmnhybgtvfrcdexswzaq8970654321",
    'e' to "erdswtfcxzaqygvuhbijnokmpl3425167890",
    'x' to "xzsdcawerfvqtgbyhnujmikolp3241567890",
    'z' to "zasxqwedcrfvtgbyhnujmikolp1234567890",
    'o' to "oplkimjunhybgtvfrcdexswzaq9087654321",
    'w' to "wesaqrdxztfcygvuhbijnokmpl2314567890",
    'p' to "plokimjunhybgtvfrcdexswzaq0987654321",
    'q' to "qwaeszrdxtfcygvuhbijnokmpl1234567890",
    '1' to "1234567890qawzsexdrcftvgybhunjimkolp",
    '2' to "2134567890qwasezxdrcftvgybhunjimkolp",
    '3' to "3241567890weqasdrzxcftvgybhunjimkolp",
    '4' to "4352617890erwsdftqazxcvgybhunjimkolp",
    '5' to "5463728190rtedfgywsxcvbhuqaznjimkolp",
    '6' to "6574839201tyrfghuedcvbnjiwsxmkoqazlp",
    '7' to "7685940321yutghjirfvbnmkoedclpwsxqaz",
    '8' to "8796054321uiyhjkotgbnmlprfvedcwsxqaz",
    '9' to "9807654321ioujklpyhnmtgbrfvedcwsxqaz",
    '0' to "0987654321opiklujmyhntgbrfvedcwsxqaz")
    .mapValues { (_, v) -> v.mapIndices() }

  private fun distance(fromKey: Char, toKey: Char) = nearby[fromKey]!![toKey]

  val defaultTagOrder: Comparator<String> = compareBy(
    { it[0].isDigit() || it[1].isDigit() },
    { distance(it[0], it.last()) },
    layout.priority { it[0] })

  private var allPossibleTags: Set<String> = settings.allowedChars.bigrams()

  private fun String.bigrams() = run { flatMap { e -> map { c -> "$e$c" } } }
      .sortedWith(defaultTagOrder).toSet()

  fun getCompatibleTags(query: String, matching: (String) -> Boolean) =
    Pattern.filter(allPossibleTags, query).filter(matching).toSet()

  override fun getState() = settings

  override fun loadState(state: AceSettings) {
    logger.info("Loaded AceConfig settings: $settings")
    settings = state
  }

  private val panel by lazy { AceSettingsPanel() }

  override fun getDisplayName() = "AceJump"

  override fun createComponent() = panel.rootPanel

  override fun isModified() =
    panel.allowedChars != settings.allowedChars ||
      panel.keyboardLayout != settings.layout ||
      panel.jumpModeColor != settings.jumpModeColor ||
      panel.targetModeColor != settings.targetModeColor ||
      panel.textHighlightColor != settings.textHighlightColor ||
      panel.tagForegroundColor != settings.tagForegroundColor ||
      panel.tagBackgroundColor != settings.tagBackgroundColor ||
      panel.supportPinyin != settings.supportPinyin

  private fun String.distinctAlphanumerics() =
    if (isEmpty()) settings.layout.text
    else toList().distinct().filter(Char::isLetterOrDigit).joinToString("")

  override fun apply() {
    panel.allowedChars.distinctAlphanumerics().let {
      settings.allowedChars = it
      allPossibleTags = it.bigrams()
    }

    settings.layout = panel.keyboardLayout
    panel.jumpModeColor?.let { settings.jumpModeRGB = it.rgb }
    panel.targetModeColor?.let { settings.targetModeRGB = it.rgb }
    panel.textHighlightColor?.let { settings.textHighlightRGB = it.rgb }
    panel.tagForegroundColor?.let { settings.tagForegroundRGB = it.rgb }
    panel.tagBackgroundColor?.let { settings.tagBackgroundRGB = it.rgb }
    panel.supportPinyin.let { settings.supportPinyin = it }
    logger.info("User applied new settings: $settings")
  }

  override fun reset() = panel.reset(settings)
}