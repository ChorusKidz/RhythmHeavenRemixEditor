package chrislo27.rhre.analytics

import com.badlogic.gdx.Preferences
import com.segment.analytics.Analytics
import com.segment.analytics.messages.IdentifyMessage
import com.segment.analytics.messages.ScreenMessage
import com.segment.analytics.messages.TrackMessage
import ionium.screen.Updateable
import ionium.templates.Main
import java.util.*
import java.util.concurrent.TimeUnit


object AnalyticsHandler {

	val analytics: Analytics = Analytics.builder("DFuo21VuGjo4E3UQcEndex4CpCUblmxF").flushInterval(2,
																								   TimeUnit.SECONDS).build()
	var uuid: UUID? = null
		private set
	val anonymousUUID: UUID = UUID.randomUUID()

	fun init(prefs: Preferences) {
		if (prefs.contains("analyticsUUID")) {
			val u = prefs.getString("analyticsUUID", "") ?: ""

			if ("[0-9a-f]{8}-[0-9a-f]{4}-4[0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}".toRegex().matches(u)) {
				uuid = UUID.fromString(u)
			}
		}

		if (uuid == null) {
			uuid = UUID.randomUUID()

			analytics.enqueue(IdentifyMessage.builder().userId(uuid!!.toString().toLowerCase(Locale.ROOT)).anonymousId(
					anonymousUUID).context(mapOf<String, String>(
					"os" to System.getProperty("os.name"),
					"cores" to Runtime.getRuntime().availableProcessors().toString(),
					"memoryUsage" to (((Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()) / 1024).toString()
							+ " KB / " + (Runtime.getRuntime().totalMemory() / 1024).toString() + " KB")
																)))
		}

		prefs.putString("analyticsUUID", uuid!!.toString().toLowerCase(Locale.ROOT))
		prefs.flush()

		track("start", mapOf())
	}

	fun getTrackBuilder(event: String): TrackMessage.Builder {
		return TrackMessage.builder(event).userId(uuid!!.toString().toLowerCase(Locale.ROOT)).anonymousId(
				anonymousUUID).context(
				mapOf("editorVersion" to Main.version))
	}

	fun track(event: String, props: Map<String, String>) {
		analytics.enqueue(getTrackBuilder(event).properties(props))
	}

	fun screen(screen: Updateable<*>) {
		analytics.enqueue(ScreenMessage.builder(screen.javaClass.simpleName).userId(
				uuid!!.toString().toLowerCase(Locale.ROOT)).anonymousId(anonymousUUID))
	}

	fun addShutdownHook() {
		Runtime.getRuntime().addShutdownHook(Thread {
			AnalyticsHandler.track("close", mapOf("githubVersion" to Main.githubVersion))
			AnalyticsHandler.analytics.flush()
			AnalyticsHandler.analytics.shutdown()
		})
	}

}